package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.client.util.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;

public abstract class EndPointerBase implements EndPointer, Runnable{
	private static Logger _logger = Logger.getLogger(EndPointerBase.class);

	protected InputStream astream;
	protected OutputStream ostream;
	protected AFormat format;
	protected SpeechEventListener listener;
	protected boolean streamEndReached = false;
	protected boolean speechStarted = false;
	protected boolean speechEnded = false;
	private Thread t;

	private boolean preTrigger = false;;

	public EndPointerBase() {
		super();
		preTrigger = false;
	}
	
	public abstract void doEndpointing();

	
	public void start(InputStream audioStream,  AFormat format, OutputStream outputStream, SpeechEventListener listener) throws IOException {
		_logger.info("start");
		setInputStream(audioStream);
        this.ostream = outputStream;
        this.listener = listener;
        this.format = format;
    	t=new Thread(this);
    	t.start();
    	if (preTrigger) {
        	speechStarted=true;
    	    listener.speechStarted();
    	    preTrigger = false;
    	}
    
    }

	public  long triggerStart() {
		_logger.info("Trigger start called");
		//TODO: Danger that this could be called before start method. which will reset the flags (not to mention the listener has been set yet!)
    	speechStarted=true;
    	if( listener!= null) {
    	    listener.speechStarted();
    	    return 1;
    	} else {
    		preTrigger = true;
    		return -1;
    	}
    }

	public void triggerEnd() {
		_logger.info("Trigger End Called");
    	speechEnded=true;
    	if( listener!= null)
    	    listener.speechEnded();
    }

	/**
     * Sets the InputStream from which this StreamDataSource reads.
     *
     * @param inputStream the InputStream from which audio data comes
     * @param streamName  the name of the InputStream
     */
    protected void setInputStream(InputStream inputStream) {
		_logger.info("setinput stream and reset start flag");
        astream = inputStream;
        streamEndReached = false;
    	speechStarted = false;
    	speechEnded = false;
    }

	protected void closeInputDataStream() {
    	_logger.info("Closing data stream");
        streamEndReached = true;
        if (astream != null) {
        	try {
                astream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

	public void stopRecording() {
        streamEndReached = true;
        //closeInputDataStream();
    }

	@Override
    public void run() {
	    doEndpointing();
	    
    }


}