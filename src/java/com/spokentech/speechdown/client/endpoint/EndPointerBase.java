package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread.State;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;

public abstract class EndPointerBase implements EndPointer, Runnable{
	private static Logger _logger = Logger.getLogger(EndPointerBase.class);
	private static final int DEFAULT_BUFFER_SIZE = 8000;
	
	protected InputStream astream;
	protected OutputStream ostream;
	protected AFormat format;
	protected SpeechEventListener listener;
	protected boolean streamEndReached = false;
	protected boolean speechStarted = false;
	protected boolean speechEnded = false;
	private Thread t;

	private boolean preTrigger = false;;
	protected int bytesToRead = DEFAULT_BUFFER_SIZE; 

	public EndPointerBase() {
		super();
		preTrigger = false;
		bytesToRead = DEFAULT_BUFFER_SIZE;
	}
	
	public EndPointerBase(int bufferSize) {
		super();
		preTrigger = false;
		bytesToRead = bufferSize;
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
	

	public boolean inUse() {

		if ( t == null)
			return false;
		
		boolean inUse = true;
		State s = t.getState();
		if (s == State.TERMINATED)
			inUse =false;
		
		return inUse;
	}


	public  long triggerStart() {
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
    	speechEnded=true;
    	closeOutputDataStream();
    	if( listener!= null)
    	    listener.speechEnded();
    	listener=null;
    }

	/**
     * Sets the InputStream from which this StreamDataSource reads.
     *
     * @param inputStream the InputStream from which audio data comes
     * @param streamName  the name of the InputStream
     */
    protected void setInputStream(InputStream inputStream) {
        astream = inputStream;
        streamEndReached = false;
    	speechStarted = false;
    	speechEnded = false;
    }

    
	protected void closeOutputDataStream() {
        streamEndReached = true;
        _logger.info("Closing output stream");
        if (ostream != null) {
            _logger.info("Not null output stream");
        	try {
                ostream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
	protected void closeInputDataStream() {
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
        //streamEndReached = true;
        //closeInputDataStream();
    }

	@Override
    public void run() {
	    doEndpointing();
	    
    }


}