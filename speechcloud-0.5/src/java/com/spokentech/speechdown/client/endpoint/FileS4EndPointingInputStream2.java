package com.spokentech.speechdown.client.endpoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat.Type;

import org.apache.log4j.Logger;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import com.spokentech.speechdown.client.sphinx.SpeechDataStreamer;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.sphinx.AudioStreamDataSource;
import com.spokentech.speechdown.common.sphinx.SpeechDataMonitor;

import com.spokentech.speechdown.server.recog.StreamDataSource;

public class FileS4EndPointingInputStream2 extends EndPointingInputStreamBase implements EndPointingInputStream {
	
    private static Logger _logger = Logger.getLogger(FileS4EndPointingInputStream2.class);

	private int id;

	private ConfigurationManager cm;

	private File  file;
	private AudioInputStream stream;
	private Type type;

	StreamDataSource dataSource = null;
	
	private String mimeType;
	
	/**
     * @return the mimeType
     */
    public String getMimeType() {
    	return mimeType;
    }


	/**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
    	this.mimeType = mimeType;
    }


	

	public void setupStream(File file) {
		_logger.debug("Setting up the file");
		this.file = file;

    	try {
    		stream = AudioSystem.getAudioInputStream(file);
    		type = AudioSystem.getAudioFileFormat(file).getType();

    	} catch (Exception e) {
    		e.printStackTrace();
    	}
        setupPipedStream();
	}

	

	public void shutdownStream() {
		//TODO:
		_logger.info("Shutdown stream not implemented!");
	}
	
	
	public synchronized void startAudioTransfer(long timeout, SpeechEventListener listener) throws InstantiationException, IOException {

		_listener = new Listener(listener);
		
 		StreamDataSource dataSource = new AudioStreamDataSource();

 		FrontEnd frontEnd = createFrontend(false, false, (DataProcessor) dataSource, _listener);
 		//frontEnd.initialize();
		
		dataSource.setInputStream((InputStream)stream, "ws-audiostream", (int)stream.getFormat().getSampleRate(), stream.getFormat().isBigEndian(), stream.getFormat().getSampleSizeInBits()/8,stream.getFormat().getEncoding());
 		
		
		SpeechDataStreamer sds = new SpeechDataStreamer();
		sds.startStreaming(frontEnd, outputStream);
		
		if (timeout > 0)
			startInputTimers(timeout);
		
		_state = WAITING_FOR_SPEECH;
	}

	
	
    public synchronized void stopAudioTransfer() {
    	_logger.debug("Stopping stream");
    	if (dataSource != null) {
	    	try {
		        dataSource.closeDataStream();
	        } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
	        }
    	}
    	if (_timer !=null) 
    	   _timer.cancel();
    }
	
	
    /**
     * Starts the input timers which trigger no-input-timeout if speech has not started after the specified time.
     * @param noInputTimeout the amount of time to wait, in milliseconds, before triggering a no-input-timeout. 
     * @return {@code true} if input timers were started or {@code false} if speech has already started.
     * @throws IllegalStateException if recognition is not in progress or if the input timers have already been started.
     */
    public synchronized boolean startInputTimers(long noInputTimeout) throws IllegalStateException {
        if (noInputTimeout <= 0) {
            throw new IllegalArgumentException("Illegal value for no-input-timeout: " + noInputTimeout);
        }
        //TODO: Determine if recognition is in progress and throw exception as needed
        //if (_processor == null) {
        //    throw new IllegalStateException("Recognition not in progress!");
        //}
        if (_noInputTimeoutTask != null) {
            throw new IllegalStateException("InputTimer already started!");
        }

        boolean startInputTimers = (_state == WAITING_FOR_SPEECH); 
        if (startInputTimers) {
            _noInputTimeoutTask = new NoInputTimeoutTask();
            _timer.schedule(_noInputTimeoutTask, noInputTimeout);
        }

        return startInputTimers;
    }


	@Override
    public AudioFormat getFormat1() {
	    // TODO Auto-generated method stub
		return stream.getFormat();
    }


	@Override
    public javax.media.format.AudioFormat getFormat2() {
	    // TODO Auto-generated method stub
	    return null;
    }
	

	

	
}
