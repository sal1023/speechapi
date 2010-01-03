package com.spokentech.speechdown.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.logging.Logger;


import com.spokentech.speechdown.client.endpoint.EndPointingInputStream;
import com.spokentech.speechdown.common.SpeechEventListener;

public class AsynchCommand implements Runnable {


    private static Logger _logger = Logger.getLogger(AsynchCommand.class.getName());
	
   public enum CommandType {recognize, synthesize}
	

	public AsynchCommand(CommandType type, String service, InputStream grammarIs,
            EndPointingInputStream epStream, boolean lmflg, boolean batchMode, long timeout,
            SpeechEventListener eventListener) {
	    super();
	    this.type = type;
	    this.service = service;
	    this.grammarIs = grammarIs;
	    this.epStream = epStream;
	    this.lmflg = lmflg;
	    this.batchMode = batchMode;
	    this.timeout = timeout;
	    this.eventListener = eventListener;
	    this.id = UUID.randomUUID().toString();
    }


    private CommandType type;
	private String service;
	private InputStream grammarIs;
	private EndPointingInputStream epStream;
	private boolean lmflg;
	private boolean batchMode;
	private long timeout;
	private SpeechEventListener eventListener;
	private HttpRecognizer httpRecognizer;
	
	private String id;
	
	/**
     * @return the id
     */
    public String getId() {
    	return id;
    }



	public void cancel(String id) {
		if (this.id.equals(id))
			if (httpRecognizer != null)
		        httpRecognizer.cancel();
			else
				_logger.info("cant cancel, no recognizer yet");
	}


	
	@Override
    public void run() {
		if (type == CommandType.recognize) {
			try {
				_logger.info("running command");
				//not thread safe so creating a new recognizer.  It is pretty lightweight so not much overhead but still ...
				//TODO: make it thread safe so the same HttpRecognizer can be reused (pass the recognizer object in the command)
			    httpRecognizer = new HttpRecognizer();
				httpRecognizer.setService(service);
	            httpRecognizer.recognize(grammarIs, epStream, lmflg, batchMode, timeout,eventListener);
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
		} else {
			_logger.info("Commad type not implemented "+ type);
		}

	    
    }
		

}
