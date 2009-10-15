/**
 * This class will read audio from the mic and stream out only the audio between start and end speech.  
 * It use Sphinx4 frontend to do the endpointing. 
 *
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import org.apache.log4j.Logger;

import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import com.spokentech.speechdown.client.Microphone;
import com.spokentech.speechdown.client.sphinx.SpeechDataStreamer;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.sphinx.SpeechDataMonitor;


// TODO: Auto-generated Javadoc
/**
 * The Class MicS4EndPointingInputStream.  This class will read audio from the mic and stream out only the audio between start and end speech.  
 * It use Sphinx4 frontend to do the endpointing. 
 */
public class MicS4EndPointingInputStream extends EndPointingInputStreamBase implements EndPointingInputStream {
	
    private static Logger _logger = Logger.getLogger(MicS4EndPointingInputStream.class);

	private int id;

	private ConfigurationManager cm;

	private Microphone mic;

	private String s4ConfigFile = "/config/sphinx-config.xml";
	

	private String mimeType;
	
	/**
	 * Gets the mime type.
	 * 
	 * @return the mimeType
	 */
    public String getMimeType() {
    	return mimeType;
    }


	/**
	 * Sets the mime type.
	 * 
	 * @param mimeType the mimeType to set
	 */
    public void setMimeType(String mimeType) {
    	this.mimeType = mimeType;
    }
	
    /**
     * Gets the s4 config file.
     * 
     * @return the s4ConfigFile
     */
    public String getS4ConfigFile() {
    	return s4ConfigFile;
    }


	/**
	 * Sets the s4 config file.
	 * 
	 * @param configFile the s4ConfigFile to set
	 */
    public void setS4ConfigFile(String configFile) {
    	s4ConfigFile = configFile;
    }


	/**
	 * Inits the.
	 */
	public void init() {
	    //URL sphinxConfigUrl = MicReceiver.class.getResource(s4ConfigFile);
        //if (sphinxConfigUrl == null) {
        //    throw new RuntimeException("Sphinx config file not found!");
        //}
        //cm = new ConfigurationManager(sphinxConfigUrl);
        _logger.debug("config: "+s4ConfigFile);
        cm = new ConfigurationManager(s4ConfigFile);
	}
	
	/**
	 * Setup stream.
	 */
	public void setupStream() {
		_logger.info("Setting up the stream");
        setupPipedStream();
	}

	/**
	 * Shutdown stream.
	 */
	public void shutdownStream() {
		//TODO:
		_logger.info("Shutdown stream not implemented!");
	}
	

	
	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#startAudioTransfer(long, com.spokentech.speechdown.client.SpeechEventListener)
	 */
	public void startAudioTransfer(long timeout, SpeechEventListener listener) throws InstantiationException, IOException {

		_listener = new Listener(listener);
		
		//TODO: do I need multiple front ends
		id = 10;

		//get elements from the s4 front end
		SpeechDataMonitor speechDataMonitor = (SpeechDataMonitor) cm.lookup("speechDataMonitor");
		if (speechDataMonitor != null) {
			speechDataMonitor.setSpeechEventListener(_listener);
		}

		FrontEnd frontEnd = (FrontEnd) cm.lookup("frontEnd");		
 		Microphone mic = new Microphone();		
 		frontEnd.setDataSource((DataProcessor) mic);
		frontEnd.initialize();
		
		SpeechDataStreamer sds = new SpeechDataStreamer();
		sds.startStreaming(frontEnd, outputStream);
		
		mic.initialize();
		mic.startRecording();
		if (timeout > 0)
			startInputTimers(timeout);
		
		_state = WAITING_FOR_SPEECH;
	}

	
	
    /* (non-Javadoc)
     * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#stopAudioTransfer()
     */
    public synchronized void stopAudioTransfer() {
    	_logger.info("Stopping mic");
    	mic.stopRecording();
    	//mic.shutDown();
    	_timer.cancel();
    }
	
	
    /**
     * Starts the input timers which trigger no-input-timeout if speech has not started after the specified time.
     * 
     * @param noInputTimeout the amount of time to wait, in milliseconds, before triggering a no-input-timeout.
     * 
     * @return {@code true} if input timers were started or {@code false} if speech has already started.
     * 
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


	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#getFormat1()
	 */
	@Override
    public AudioFormat getFormat1() {
		return mic.getAudioFormat();
    }


	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#getFormat2()
	 */
	@Override
    public javax.media.format.AudioFormat getFormat2() {
	    // TODO Auto-generated method stub
	    return null;
    }
	
	
	
}
