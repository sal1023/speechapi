
package com.spokentech.speechdown.common.sphinx;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.util.AudioFileProcessListener;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.util.props.*;

import java.io.File;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.server.recog.StreamDataSource;

import com.xuggle.xuggler.IAudioResampler;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IError;

import com.xuggle.xuggler.IPacket;

import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;



public class XugglerAudioStreamDataSource extends BaseDataProcessor implements StreamDataSource {
	private static Logger _logger = Logger.getLogger(XugglerAudioStreamDataSource.class.getName());
    /** SphinxProperty for the number of bytes to read from the InputStream each time. */
    @S4Integer(defaultValue = 1024)
    public static final String PROP_BYTES_PER_READ = "bytesPerRead";
    /** Default value for PROP_BYTES_PER_READ. */
    public static final int PROP_BYTES_PER_READ_DEFAULT = 1024;

    @S4ComponentList(type = Configurable.class)
    public static final String AUDIO_FILE_LISTENERS = "audioFileListners";
    protected List<AudioFileProcessListener> fileListeners = new ArrayList<AudioFileProcessListener>();

	//Xuggler
    private IAudioResampler mASampler = null;
	private IPacket packet;
	
    int audioStreamId = -1;
    IStreamCoder audioCoder = null;


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

    private File curAudioFile;
    
    int TargetRate = 8000;
    
    //private BufferedWriter out;
    
    IContainer container ;
    

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        bytesPerRead = ps.getInt(PROP_BYTES_PER_READ);

        //Logger logger = ps.getLogger();

        // attach all pool-listeners
        List<? extends Configurable> list = ps.getComponentList(AUDIO_FILE_LISTENERS);
        for (Configurable configurable : list) {
            assert configurable instanceof AudioFileProcessListener;
            addNewFileListener((AudioFileProcessListener) configurable);
        }

        initialize();
    }


    @Override
    public void initialize() {
        super.initialize();
        
        
        // Create a Xuggler container object
        container = IContainer.make();
        packet = IPacket.make();     

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
        
        
        if(container.open(inputStream, null) < 0)
           throw new IllegalArgumentException("could not open input stream");
           
        // query how many streams the call to open found
        int numStreams = container.getNumStreams();
        
        // and iterate through the streams to find the first audio stream
        audioStreamId = -1;
        audioCoder = null; 
        for(int i = 0; i < numStreams; i++) {
          // Find the stream object
          IStream stream = container.getStream(i);
          // Get the pre-configured decoder that can decode this stream;
          IStreamCoder coder = stream.getStreamCoder();
          System.out.println("CODER: "+coder.toString());
        
          if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
            audioStreamId = i;
            audioCoder = coder;
            break;
          }
        }
        
        if (audioStreamId == -1)
          throw new RuntimeException("could not find audio stream in container");
        
        /*
         * Now we have found the audio stream in this file.  Let's open up our decoder so it can
         * do work.
         */
        if (audioCoder.open() < 0)
          throw new RuntimeException("could not open audio decoder for container ");
        
        if (audioCoder.getSampleRate() != TargetRate) {
          mASampler = IAudioResampler.make(
              1, audioCoder.getChannels(),
              TargetRate, audioCoder.getSampleRate());
          sampleRate = mASampler.getOutputRate();
          if (mASampler == null) {
            throw new RuntimeException("could not open audio resampler for stream");
          }
        } else {
          sampleRate = TargetRate;
          mASampler = null;
        }


        bigEndian = false;
        bytesPerValue = 2;
        signedData = true;
 
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
        
        
        int cRetCode = container.open(inputStream, null);
        System.out.println("container ope ret code :"+cRetCode);
        	
            if (cRetCode != 0) {
                IError err = IError.make(cRetCode);
                if (err == null) {
                	  System.out.println("Null error code");
                } else {
                   System.out.println("IContainer.open() returned an error: "
                        + err.getType() + ", " + err.getDescription());
                   System.out.println(err.toString());
                }
                //return;
            }
          // throw new IllegalArgumentException("could not open input stream");
           
        // query how many streams the call to open found
        int numStreams = container.getNumStreams();
        
        System.out.println("Number of Streams: "+numStreams);
        // and iterate through the streams to find the first audio stream
        audioStreamId = -1;
        audioCoder = null; 
        for(int i = 0; i < numStreams; i++) {
          // Find the stream object
          IStream stream = container.getStream(i);
          // Get the pre-configured decoder that can decode this stream;
          IStreamCoder coder = stream.getStreamCoder();
          System.out.println("CODER["+i+"]: "+coder.toString() + "Type is:" +coder.getCodecType());
        
          if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
            audioStreamId = i;
            audioCoder = coder;
            break;
          }
        }
        
        if (audioStreamId == -1)
          throw new RuntimeException("could not find audio stream in container");
        
        /*
         * Now we have found the audio stream in this file.  Let's open up our decoder so it can
         * do work.
         */
        if (audioCoder.open() < 0)
          throw new RuntimeException("could not open audio decoder for container ");
        
        if (audioCoder.getSampleRate() != TargetRate) {
          mASampler = IAudioResampler.make(
              1, audioCoder.getChannels(),
              TargetRate, audioCoder.getSampleRate());
          sampleRate = mASampler.getOutputRate();
          if (mASampler == null) {
            throw new RuntimeException("could not open audio resampler for stream");
          }
        } else {
          mASampler = null;
          sampleRate = TargetRate;
        }


              
        bigEndian = false;
        bytesPerValue = 2;
        signedData = true;
 
 
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
                       output = readNextFrame2();
                	} catch (Exception e) {
                  		//e.printStackTrace();
                		_logger.fine("Exception reading audio! "+ e.getMessage());
  
                		if (e instanceof DataProcessingException)
                			throw new DataProcessingException();
                		else
                			//output being null, should just trigger sending the data end signal.  thats what we want.
                			output = null;
                	}
                    _logger.fine("Got the next frame ");
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
    private Data readNextFrame2() throws DataProcessingException {      	
    /*
     * Now, we start walking through the container looking at each packet.
     */
    	


    while (container.readNextPacket(packet) >= 0) {
        long firstSample = totalValuesRead;
      /*
       * Now we have a packet, let's see if it belongs to our audio stream
       */
      if (packet.getStreamIndex() == audioStreamId) {
        /*
         * We allocate a set of samples with the same number of channels as the
         * coder tells us is in this buffer.
         * 
         * We also pass in a buffer size (1024 in our example), although Xuggler
         * will probably allocate more space than just the 1024 (it's not important why).
         */
        IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());
        IAudioSamples reSamples = IAudioSamples.make(1024, audioCoder.getChannels());
        
        /*
         * A packet can actually contain multiple sets of samples (or frames of samples
         * in audio-decoding speak).  So, we may need to call decode audio multiple
         * times at different offsets in the packet's data.  We capture that here.
         */
        int offset = 0;
        
        /*
         * Keep going until we've processed all data
         */
        _logger.fine(">> PacketSize "+packet.getSize());
        while(offset < packet.getSize()) {
          int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);

          if (bytesDecoded < 0)
            throw new RuntimeException("got error decoding audio in:");
          offset += bytesDecoded;
          _logger.fine(">> "+bytesDecoded +"  "+offset+ " "+samples.getNumSamples());
          /*
           * Some decoder will consume data in a packet, but will not be able to construct
           * a full set of samples yet.  Therefore you should always check if you
           * got a complete set of samples from the decoder
           */
          
          int bytesResampled=0;
          if (mASampler != null && samples.getNumSamples() >0) {
        	  bytesResampled = mASampler.resample(reSamples, samples, samples.getNumSamples());
          }  else {
            reSamples = samples;
          }
          
          _logger.fine(">> Resamples"+bytesResampled +"  "+offset +" "+reSamples.getNumSamples());

        }

        // get a double array object

        
        byte[] rawBytes = reSamples.getData().getByteArray(0, reSamples.getSize());
        // turn it into an Data object
        double[] doubleData;
        if (bigEndian) {
            doubleData = DataUtil.bytesToValues(rawBytes, 0, rawBytes.length, bytesPerValue, signedData);
        } else {
            doubleData = DataUtil.littleEndianBytesToValues(rawBytes, 0, rawBytes.length, bytesPerValue, signedData);
        }
        
        //double[] doubleData = new double[reSamples.getSize()];
        //reSamples.getData().get(0, doubleData, 0, (int)reSamples.getNumSamples());
        //_logger.fine(">> CALLED GETDDATA");


        if (doubleData != null) {
           _logger.fine(">>writing double data,  "+ doubleData.length+ " values"+doubleData[0]+ " "+doubleData[doubleData.length-1]);
        } else { 
        	_logger.fine(">>doubleData was null");
        }
        
        totalValuesRead = totalValuesRead + reSamples.getNumSamples();
        long collectTime = System.currentTimeMillis();
        return new DoubleData(doubleData, sampleRate, collectTime, firstSample);
        
      } else {
        /*
         * This packet isn't part of our audio stream, so we just silently drop it.
         */
        do {} while(false);
      }
            
    }

    return null;

  }
    
    private void cleanUp() {
	    /*
	     * Technically since we're exiting anyway, these will be cleaned up by 
	     * the garbage collector... but because we're nice people and want
	     * to be invited places for Christmas, we're going to show how to clean up.
	     */
	    
	    if (audioCoder != null) {
	      audioCoder.close();
	      audioCoder = null;
	    }
	    if (container !=null) {
	      container.close();
	      container = null;
	    }
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
            	_logger.fine("total read "+totalRead+"  returning null");
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
                //_logger.fine("total read "+totalRead+"  tried to read "+bytesToRead);
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
    	 _logger.fine("Closing data stream");
        streamEndReached = true;
        cleanUp();
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
		return (long) (((double) totalValuesRead / (double) sampleRate) * 1000.0);
    }


}

