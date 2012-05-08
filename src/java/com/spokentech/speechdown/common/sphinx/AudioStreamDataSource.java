/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.common.sphinx;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.util.AudioFileProcessListener;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.util.props.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.server.recog.StreamDataSource;

import java.io.PipedInputStream;
/**
 * An AudioFileDataSource generates a stream of audio data from a given audion file. All required information concerning
 * the audio format are read directly from the file . One would need to call {@link #setAudioFile(java.io.File,String)}
 * to set the input file, and call {@link #getData} to obtain the Data frames.
 * <p/>
 * Using JavaSound as backend this class is able to handle all sound files supported by JavaSound. Beside the built-in
 * support for .wav, .au and .aiff. Using plugins (cf.  http://www.jsresources.org/ ) it can be extended to support
 * .ogg, .mp3, .speex and others.
 *
 * @author Holger Brandl
 */

public class AudioStreamDataSource extends BaseDataProcessor implements StreamDataSource {
	private static Logger _logger = Logger.getLogger(AudioStreamDataSource.class.getName());
    /** SphinxProperty for the number of bytes to read from the InputStream each time. */
    @S4Integer(defaultValue = 1024)
    public static final String PROP_BYTES_PER_READ = "bytesPerRead";
    /** Default value for PROP_BYTES_PER_READ. */
    public static final int PROP_BYTES_PER_READ_DEFAULT = 1024;

    @S4ComponentList(type = Configurable.class)
    public static final String AUDIO_FILE_LISTENERS = "audioFileListners";
    protected List<AudioFileProcessListener> fileListeners = new ArrayList<AudioFileProcessListener>();


    protected InputStream dataStream;
    protected int sampleRate;
    protected int bytesPerRead = PROP_BYTES_PER_READ_DEFAULT;
    protected int bytesPerValue;
    private long totalValuesRead;
    protected boolean bigEndian;
    protected boolean signedData;
    private boolean streamEndReached = false;
    private boolean utteranceEndSent = false;
    private boolean utteranceStarted = false;
    private long totalValues = 0;

    long firstSample = totalValuesRead;
    
    
    private File curAudioFile;
    
    //private BufferedWriter out;

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        create(ps.getInt(PROP_BYTES_PER_READ), ps.getComponentList(AUDIO_FILE_LISTENERS, AudioFileProcessListener.class));
    }

    private void create( int bytesPerRead, List<AudioFileProcessListener> listeners ) {
        this.bytesPerRead = bytesPerRead;

        if( listeners != null ) {
            // attach all pool-listeners
            for (AudioFileProcessListener configurable : listeners) {
                addNewFileListener(configurable);
            }
        }

        initialize();
    }


    @Override
    public void initialize() {
        super.initialize();

        // reset all stream tags
        streamEndReached = false;
        utteranceEndSent = false;
        utteranceStarted = false;

        if (bytesPerRead % 2 == 1) {
            bytesPerRead++;
        }
        
        // TODO Go back to a real logger!
        _logger.setLevel(Level.INFO);
        // The root logger's handlers default to INFO. We have to
        // crank them up. We could crank up only some of them
        // if we wanted, but we will turn them all up.
         Handler[] handlers =  Logger.getLogger( "" ).getHandlers();
         for ( int index = 0; index < handlers.length; index++ ) {
           handlers[index].setLevel( Level.INFO );
         }

            //try {
            //    out = new BufferedWriter(new FileWriter("c:/temp/"+System.currentTimeMillis()));
            //} catch (IOException e) {
            //    // TODO Auto-generated catch block
            //    e.printStackTrace();
            //}
            //        out.close();

    }


    /**
     * Sets the audio file from which the data-stream will be generated of.
     *
     * @param audioFile  The location of the audio file to use
     * @param streamName The name of the InputStream. if <code>null</code> the complete path of the audio file will be
     *                   uses as stream name.
     */
    public void setAudioFile(File audioFile, String streamName) {
        try {
            setAudioFile(audioFile.toURI().toURL(), streamName);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Sets the audio file from which the data-stream will be generated of.
     *
     * @param audioFileURL The location of the audio file to use
     * @param streamName   The name of the InputStream. if <code>null</code> the complete path of the audio file will be
     *                     uses as stream name.
     */
    public void setAudioFile(URL audioFileURL, String streamName) {
        // first close the last stream if there's such a one
        if (dataStream != null) {
            try {
                dataStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            dataStream = null;
        }

        assert audioFileURL != null;
        if (streamName != null)
            streamName = audioFileURL.getPath();

        AudioInputStream audioStream = null;
        try {
            audioStream = AudioSystem.getAudioInputStream(audioFileURL);
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Audio file format not supported: " + e);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        curAudioFile = new File(audioFileURL.getFile());
        for (AudioFileProcessListener fileListener : fileListeners)
            fileListener.audioFileProcStarted(curAudioFile);

        setInputStream(audioStream, streamName);
    }
    
    /**
     * Sets the InputStream from which this StreamDataSource reads.
     *
     * @param inputStream the InputStream from which audio data comes
     * @param streamName  the name of the InputStream
     *  @deprecated
     */
   public void setInputStream(PipedInputStream inputStream, String streamName) {
        dataStream = inputStream;
        streamEndReached = false;
        utteranceEndSent = false;
        utteranceStarted = false;
        totalValuesRead = 0;

        _logger.fine("inputstream "+inputStream);

        //AudioFormat format = inputStream.getFormat();
        this.sampleRate = 8000; 
        this.bigEndian = false;
        this.bytesPerValue = 2;

        // test whether all files in the stream have the same format
        //if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED))
            signedData = true;
        //else if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED))
         //   signedData = false;
        //else
       //     throw new RuntimeException("used file encoding is not supported");

        totalValuesRead = 0;
    }


    /**
     * Sets the InputStream from which this StreamDataSource reads.
     *
     * @param inputStream the InputStream from which audio data comes
     * @param streamName  the name of the InputStream
     */
    public void setInputStream(InputStream inputStream, String streamName) {
        dataStream = inputStream;
        streamEndReached = false;
        utteranceEndSent = false;
        utteranceStarted = false;
        totalValues = 0;

        if (!(inputStream instanceof AudioInputStream)) 
            throw new RuntimeException("Not an  audio input stream");

        AudioFormat format = ((AudioInputStream) inputStream).getFormat();
        sampleRate = (int) format.getSampleRate();
        bigEndian = format.isBigEndian();

        String s = format.toString();
        logger.fine("input format is " + s);

        if (format.getSampleSizeInBits() % 8 != 0)
            throw new Error("StreamDataSource: bits per sample must be a multiple of 8.");
        bytesPerValue = format.getSampleSizeInBits() / 8;

        // test wether all files in the stream have the same format

        AudioFormat.Encoding encoding = format.getEncoding();
        if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED))
            signedData = true;
        else if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED))
            signedData = false;
        else
            throw new RuntimeException("used file encoding is not supported");

        totalValuesRead = 0;
    }

    


    /**
     * Sets the InputStream from which this StreamDataSource reads.
     *
     * @param inputStream the InputStream from which audio data comes
     * @param streamName  the name of the InputStream
     */
    public void setInputStream(InputStream inputStream, String streamName, AFormat format) {
        dataStream = inputStream;
        streamEndReached = false;
        utteranceEndSent = false;
        utteranceStarted = false;
        totalValues = 0;

        //int sampleRate, boolean bigEndian, int bytesPerValue, String encoding
        _logger.fine("inputstream "+inputStream);

        //AudioFormat format = inputStream.getFormat();
        this.sampleRate = (int) format.getSampleRate(); 
        this.bigEndian = format.isBigEndian();
        this.bytesPerValue = format.getSampleSizeInBits()/8;

        signedData =format.isSigned();
        
 

        totalValuesRead = 0;
    }
    

    /**
     * Reads and returns the next Data from the InputStream of StreamDataSource, return null if no data is read and end
     * of file is reached.
     *
     * @return the next Data or <code>null</code> if none is available
     * @throws edu.cmu.sphinx.frontend.DataProcessingException
     *          if there is a data processing error
     */
    public Data getData() throws DataProcessingException {
        getTimer().start();
        Data output = null;
        if (streamEndReached) {
            if (!utteranceEndSent) {
                // since 'firstSampleNumber' starts at 0, the last
                // sample number should be 'totalValuesRead - 1'
                output = createDataEndSignal();
                utteranceEndSent = true;
                _logger.fine("Sending end signal");
            } else {
            	 _logger.fine("Not Sending end signal");          	
            }
        } else {
            if (!utteranceStarted) {
                utteranceStarted = true;
                output = new DataStartSignal(sampleRate);
                _logger.fine("Sending start signal ");
            } else {
                if (dataStream != null) {
                	try {
                       output = readNextFrame();
                       //Thread.sleep(50);
                       
                	} catch (Exception e) {
                  		//e.printStackTrace();
                		_logger.fine("Exception reading audio! "+ e.getMessage());
  
                		if (e instanceof DataProcessingException)
                			throw new DataProcessingException();
                		else
                			//output being null, should just trigger sending the data end signal.  thats what we want.
                			output = null;
                	}
                    _logger.fine("Getting the next frame");
                    if (output == null) {
                        if (!utteranceEndSent) {
                            output = createDataEndSignal();
                            utteranceEndSent = true;
                            _logger.fine(".. but was null, sending end signal2");
                        }
                    }
                }
            }
        }
        getTimer().stop();
        return output;
    }


    private DataEndSignal createDataEndSignal() {
        //if (!(this instanceof ConcatAudioFileDataSource))
        //    for (AudioFileProcessListener fileListener : fileListeners)
        //        fileListener.audioFileProcFinished(curAudioFile);

    	long d = getDuration();
    	_logger.fine("********** End signal duration: "+d);
        return new DataEndSignal(d);
    }


    /**
     * Returns the next Data from the input stream, or null if there is none available
     *
     * @return a Data or null
     * @throws java.io.IOException
     */
    private Data readNextFrame() throws DataProcessingException {
        // read one frame's worth of bytes
        int read;
        int totalRead = 0;
        final int bytesToRead = bytesPerRead;
        byte[] samplesBuffer = new byte[bytesPerRead];
        long collectTime = System.currentTimeMillis();
        long firstSample = totalValuesRead;
        try {
            do {
                read = dataStream.read(samplesBuffer, totalRead, bytesToRead
                        - totalRead);
                if (read > 0) {
                    totalRead += read;
                }
            } while (read != -1 && totalRead < bytesToRead);
            if (totalRead <= 0) {
            	_logger.info("total read "+totalRead+"  returning null");
                closeDataStream();
                return null;
            }
            // shrink incomplete frames
            totalValuesRead += (totalRead / bytesPerValue);
            if (totalRead < bytesToRead) {
                totalRead = (totalRead % 2 == 0)
                        ? totalRead + 2
                        : totalRead + 3;
                byte[] shrinkedBuffer = new byte[totalRead];
                System
                        .arraycopy(samplesBuffer, 0, shrinkedBuffer, 0,
                                totalRead);
                samplesBuffer = shrinkedBuffer;
                _logger.info("total read "+totalRead+"  tried to read "+bytesToRead);
                closeDataStream();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new DataProcessingException("Error reading next frame of data in audioInputStream");
        }
        // turn it into an Data object
        double[] doubleData;
        if (bigEndian) {
            doubleData = DataUtil.bytesToValues(samplesBuffer, 0, totalRead, bytesPerValue, signedData);
        } else {
            doubleData = DataUtil.littleEndianBytesToValues(samplesBuffer, 0, totalRead, bytesPerValue, signedData);
        }

        _logger.fine("Total read in this frame: "+totalRead);
        totalValues = totalValues + doubleData.length;
        //try {
        //	for (int i=0;i<doubleData.length;i++) {
        //		out.write(i+" "+doubleData[i]);
        //		out.newLine();
        //	}
        //} catch (IOException e) {
        //	// TODO Auto-generated catch block
        //	e.printStackTrace();
        //}

        _logger.fine("writing double data,  "+ doubleData.length+ " values"+doubleData[0]+ " "+doubleData[doubleData.length-1]);
        return new DoubleData(doubleData, sampleRate, collectTime, firstSample);
    }


    public void closeDataStream() throws IOException {
    	 _logger.info("Closing data stream");
        streamEndReached = true;
        //if (dataStream != null) {
        //    dataStream.close();
        //}
    }


    /**
     * Returns the duration of the current data stream in milliseconds.
     *
     * @return the duration of the current data stream in milliseconds
     */
    private long getDuration() {
        return (long) (((double) totalValuesRead / (double) sampleRate) * 1000.0);
    }


    public int getSampleRate() {
        return sampleRate;
    }


    public boolean isBigEndian() {
        return bigEndian;
    }


    /** Adds a new listener for new file events. */
    public void addNewFileListener(AudioFileProcessListener l) {
        if (l == null)
            return;

        fileListeners.add(l);
    }


    /** Removes a listener for new file events. */
    public void removeNewFileListener(AudioFileProcessListener l) {
        if (l == null)
            return;

        fileListeners.remove(l);
    }


	@Override
    public long getLengthInMs() {

	    return (1000*totalValues)/sampleRate;
    }




}

