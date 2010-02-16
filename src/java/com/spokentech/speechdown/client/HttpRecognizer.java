/**
 * The HttpReconizer class allows you to issue a recognize command with am audio input stream.
 * The requests is sent to a http recognizer server using http protocol.  The audio is sent as an attachment in
 * a multi-part post.  The multi-part post us chunk encoded so that there recognition starts as the audio is streamed
 * 
 *
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
package com.spokentech.speechdown.client;


import java.io.BufferedInputStream;

import com.spokentech.speechdown.common.HttpCommandFields;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import com.spokentech.speechdown.client.endpoint.EndPointingInputStream;
import com.spokentech.speechdown.client.exceptions.AsynchNotEnabledException;
import com.spokentech.speechdown.client.exceptions.HttpRecognizerException;
import com.spokentech.speechdown.client.exceptions.StreamInUseException;
import com.spokentech.speechdown.client.util.AFormat;
import com.spokentech.speechdown.common.InvalidRecognitionResultException;
import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.common.SpeechEventListener;

// TODO: Auto-generated Javadoc
/**
 * The Class HttpRecognizer.
 * <pre>
 * String service = "http://myspeechservice.com/"
 * HttpRecognizer	recog = new HttpRecognizer();
 * recog.setService(service);
 * AudioInputStream inputStream = getStreamFromSomewhere(...)'
 * Type type = AudioFileFormat.Type.WAVE
 * URL grammar = new URL("file:///mygrammars/example.gram");
 * boolean lmflag = false;
 * RecognitionResults results = synthAduio= recog.recognize(inputStream,type,grammar, lmflag);
 * String rawResults = results.getText();
 * </pre>
 */
public class HttpRecognizer {

	private static Logger _logger =  Logger.getLogger(HttpRecognizer.class.getName());

	
	//TODO: add in local no input timer capability
	//protected /*static*/ Timer _timer = new Timer();
	//protected TimerTask _noInputTimeoutTask;	
    
    protected  String service = "http://spokentech.net/speechcloud/SpeechUploadServlet";    

	//Default values (if not specified as a parameter)
    private static int sampleRate = 8000;
    private static boolean signed = true;
    private static boolean bigEndian = true;
    private static int channels = 1;
    private static int sampleSizeInBits = 16;
    
	private boolean speechStarted = false;
	private boolean requestCanceled = false;
    private int numThreadsForAsyncCalls;
	
    private WorkQueue workQ = null;
    
     
    /**
     * Enable asynch mode.
     * 
     * @param numThreadsForAsyncCalls the num threads for async calls
     */
    public void enableAsynchMode(int numThreadsForAsyncCalls) {
        this.numThreadsForAsyncCalls = numThreadsForAsyncCalls;
	    workQ = new WorkQueue(numThreadsForAsyncCalls);
    }

	/**
	 * Gets the service.  The service is the a string containing the URL of the speechCloud server
	 * 
	 * @return the service
	 */
    public  String getService() {
    	return service;
    }

	/**
	 * Sets the service.  This must be set before using this class.  It is the url of the remote speechcloud service.
	 * 
	 * @param service the service to set
	 */
    public  void setService(String service) {
    	this.service = service;
        //_logger.setLevel(Level.FINE);
        // The root logger's handlers default to INFO. We have to
        // crank them up. We could crank up only some of them
        // if we wanted, but we will turn them all up.
        //Handler[] handlers =  Logger.getLogger( "" ).getHandlers();
        //for ( int index = 0; index < handlers.length; index++ ) {
        //   handlers[index].setLevel( Level.FINE );
        //}
    }


	

	/**
	 * Recognize.
	 * 
	 * @param format the format of the inputstreamd (note the is needed in plain input streams unlike audioInputstreams where this information is included in the stream.
	 * @param grammarUrl the grammar url.  If lmFlag is false, you must set the grammar file url.  (JSGF is supported)
	 * @param lmflg the lmflg.  If lmflga is true, recognition will use the default language mode on speechcloud server.
	 * @param inputStream the input stream
	 * @param doEndpointing the do endpointing
	 * @param batchMode the batch mode
	 * @param mimeType the mime type
	 * 
	 * @return the recognition result
	 */
	public RecognitionResult recognize(InputStream inputStream, AFormat format, String mimeType, URL grammarUrl, boolean lmflg, boolean doEndpointing, boolean batchMode) {
	    // Plain old http approach    	
    	HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(service);
    	
    	//create the multipart entity
    	MultipartEntity mpEntity = new MultipartEntity();
    	
    	
    	// one part is the grammar
        InputStreamBody grammarBody = null;
        if (!lmflg) {
	        try {
	        	grammarBody = new InputStreamBody(grammarUrl.openStream(), "plain/text","grammar.gram");
	        } catch (IOException e1) {
		        // TODO Auto-generated catch block
		        e1.printStackTrace();
	        }
        }
        
        _logger.fine("Actual format: " + format);    	
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody lmFlag = null;
    	StringBody endpointFlag = null;
    	StringBody batchModeFlag = null;
    	StringBody continuousFlag = null;
        try {
        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	lmFlag = new StringBody(String.valueOf(lmflg));
        	endpointFlag = new StringBody(String.valueOf(doEndpointing));
        	batchModeFlag = new StringBody(String.valueOf(batchMode));
           	continuousFlag = new StringBody(String.valueOf(Boolean.FALSE));
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
		mpEntity.addPart(HttpCommandFields.SAMPLE_RATE_FIELD_NAME, sampleRate);
		mpEntity.addPart(HttpCommandFields.BIG_ENDIAN_FIELD_NAME, bigEndian);
		mpEntity.addPart(HttpCommandFields.BYTES_PER_VALUE_FIELD_NAME, bytesPerValue);
		mpEntity.addPart(HttpCommandFields.ENCODING_FIELD_NAME, encoding);
		mpEntity.addPart(HttpCommandFields.LANGUAGE_MODEL_FLAG, lmFlag);
		mpEntity.addPart(HttpCommandFields.ENDPOINTING_FLAG, endpointFlag);
		mpEntity.addPart(HttpCommandFields.CMN_BATCH, batchModeFlag);
		mpEntity.addPart(HttpCommandFields.CONTINUOUS_FLAG,continuousFlag);

       	
		String fname = null;
		if (mimeType.equals( "audio/x-wav")) {
			fname = "audio.wav";
		} else if(mimeType.equals( "audio/x-wav")) {
			fname = "audio.au";
		} else {
			fname = "audio.wav";
			_logger.info("unhanlded mime type "+ mimeType);
		}


        InputStreamBody audioBody = new InputStreamBody(inputStream, mimeType,fname);      

        if (!lmflg)
        	mpEntity.addPart("grammar", grammarBody);
		
        mpEntity.addPart("audio", audioBody);      
        httppost.setEntity(mpEntity);
         
        _logger.fine("executing request " + httppost.getRequestLine());
        HttpResponse response = null;
        try {
	        response = httpclient.execute(httppost);
        } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        HttpEntity resEntity = response.getEntity();

        _logger.fine(response.getStatusLine().toString());
        if (resEntity != null) {
        	_logger.fine("Response content length: " + resEntity.getContentLength());
        	_logger.fine("Chunked?: " + resEntity.isChunked());

            Header[] headers = response.getAllHeaders();
            for (int i=0; i<headers.length; i++) {
            	_logger.fine(headers[i].toString());
            }
        }
        
        RecognitionResult r = null;
        if (resEntity != null) {
            try {
                InputStream s = resEntity.getContent();
                String result = readInputStreamAsString(s);
                _logger.fine(result);
                r = RecognitionResult.constructResultFromString(result);
	            resEntity.consumeContent();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (InvalidRecognitionResultException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
        }
        
        return r;
    }


 	
	/**
	 * Recognize asynch.
	 * 
	 * @param grammarUrl the grammar url
	 * @param epStream the ep stream
	 * @param lmflg the lmflg
	 * @param batchMode the batch mode
	 * @param timeout the timeout
	 * @param eventListener the event listener
	 * 
	 * @return the string
	 * 
	 * @throws InstantiationException the instantiation exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws StreamInUseException 
	 * @throws AsynchNotEnabledException 
	 * @throws HttpRecognizerException 
	 */
	public  String recognizeAsynch(URL grammarUrl, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, long timeout, SpeechEventListener eventListener ) throws InstantiationException, IOException, StreamInUseException, AsynchNotEnabledException {

		if (epStream.checkAndSetIfInUse()) 
			throw new StreamInUseException();

		InputStream grammarIs = grammarUrl.openStream();
		AsynchCommand command = null;
		if (workQ != null) {
		   command = new AsynchCommand(AsynchCommand.CommandType.recognize, service, grammarIs, epStream, lmflg, batchMode, timeout, eventListener);
		   workQ.execute(command);
		} else {
			_logger.info("AsycnMode is not enabled.  Use the enableAsynch(int numthreads) to enable ascynh mode");
			throw new AsynchNotEnabledException();
	    }
		return command.getId();
	}
	
	/**
	 * Recognize asynch.
	 * 
	 * @param grammar the grammar
	 * @param epStream the ep stream
	 * @param lmflg the lmflg
	 * @param batchMode the batch mode
	 * @param timeout the timeout
	 * @param eventListener the event listener
	 * 
	 * @return the string
	 * 
	 * @throws InstantiationException the instantiation exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws HttpRecognizerException 
	 */
	public  String recognizeAsynch(String grammar, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, long timeout, SpeechEventListener eventListener ) throws InstantiationException, IOException, StreamInUseException, AsynchNotEnabledException {

		if (epStream.checkAndSetIfInUse()) 
			throw new StreamInUseException();
		
		InputStream grammarIs = new ByteArrayInputStream(grammar.getBytes());
		AsynchCommand command = null;
		if (workQ != null) {
		   command = new AsynchCommand(AsynchCommand.CommandType.recognize, service, grammarIs, epStream, lmflg, batchMode, timeout, eventListener);
		   workQ.execute(command);
		} else {
			_logger.info("AsycnMode is not enabled.  Use the enableAsynch(int numthreads) to enable ascynh mode");
			throw new AsynchNotEnabledException();
	    }
		return command.getId();
	}	

	/**
	 * Recognize.  With no listener specified
	 * 
	 * @param grammarUrl the grammar url
	 * @param epStream the ep stream
	 * @param lmflg the lmflg
	 * @param batchMode the batch mode
	 * @param timeout the timeout
	 * 
	 * @return the recognition result
	 * 
	 * @throws InstantiationException the instantiation exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public  RecognitionResult recognize(URL grammarUrl, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, long timeout ) throws InstantiationException, IOException {
		return recognize( grammarUrl,  epStream,  lmflg,  batchMode,  timeout, null);
	}

	
	/**
	 * Recognize.
	 * 
	 * @param grammar the grammar
	 * @param epStream the ep stream
	 * @param lmflg the lmflg
	 * @param batchMode the batch mode
	 * @param timeout the timeout
	 * @param eventListener the event listener
	 * 
	 * @return the recognition result
	 * 
	 * @throws InstantiationException the instantiation exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public  RecognitionResult recognize(String grammar, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, long timeout, SpeechEventListener eventListener) throws InstantiationException, IOException {
		//String charset = Charset.defaultCharset;
		InputStream is = new ByteArrayInputStream(grammar.getBytes());
		RecognitionResult r = recognize(is, epStream,lmflg,batchMode,timeout,eventListener);
		return r;
	}
	
	/**
	 * Recognize.  This method will do require and endpointing stream as input (containing the audio).  The endpointstream will do endpoing (here on the client)
	 * This can save bandwidth aty the cost of processing on the client.
	 * 
	 * @param epStream the end-pointing stream.  Create a epStream (wraps a inputStream). and use in this method to do endpoint on the client.
	 * @param grammarUrl the grammar url.  If lmFlag is false, you must set the grammar file url.  (JSGF is supported)
	 * @param lmflg the lmflg.  If lmflga is true, recognition will use the default language mode on speechcloud server.
	 * @param timeout the timeout
	 * @param batchMode the batch mode
	 * @param eventListener the event listener
	 * 
	 * @return the recognition result
	 * 
	 * @throws InstantiationException the instantiation exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public  RecognitionResult recognize(URL grammarUrl, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, long timeout, SpeechEventListener eventListener) throws InstantiationException, IOException {

		InputStream is = grammarUrl.openStream();

		RecognitionResult r = recognize(is, epStream,lmflg,batchMode,timeout,eventListener);
		return r;
	}
		

	/**
	 * Recognize.
	 * 
	 * @param grammar the grammar
	 * @param epStream the ep stream
	 * @param lmflg the lmflg
	 * @param batchMode the batch mode
	 * @param timeout the timeout
	 * @param eventListener the event listener
	 * 
	 * @return the recognition result
	 * 
	 * @throws InstantiationException the instantiation exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public  RecognitionResult recognize(InputStream grammar, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, long timeout, SpeechEventListener eventListener) throws InstantiationException, IOException {

        //create the listener (listens for end points)  A decorator, so also can  pass events back to client
        speechStarted = false;
        requestCanceled = false;
        MySpeechEventListener listener = new MySpeechEventListener(eventListener);

    	// Plain old http approach    	
    	HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(service);
    	
    	//create the multipart entity
    	MultipartEntity mpEntity = new MultipartEntity();
    	
    	// one part is the grammar
        InputStreamBody grammarBody = new InputStreamBody(grammar, "plain/text","grammar.gram");

        //parameters are fields (StringBoodys)
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody lmFlag = null;
    	StringBody endpointFlag = null;
    	StringBody continuousFlag = null;
    	StringBody batchModeFlag = null;
    	AFormat format = epStream.getFormat();

        try {
           	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	lmFlag = new StringBody(String.valueOf(lmflg));
           	endpointFlag = new StringBody(String.valueOf(epStream.getEndPointer().requiresServerSideEndPointing()));
           	continuousFlag = new StringBody(String.valueOf(Boolean.FALSE));
        	batchModeFlag = new StringBody(String.valueOf(batchMode));
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
		//mpEntity.addPart("dataMode", dataStreamMode);

		mpEntity.addPart(HttpCommandFields.SAMPLE_RATE_FIELD_NAME, sampleRate);
		mpEntity.addPart(HttpCommandFields.BIG_ENDIAN_FIELD_NAME, bigEndian);
		mpEntity.addPart(HttpCommandFields.BYTES_PER_VALUE_FIELD_NAME, bytesPerValue);
		mpEntity.addPart(HttpCommandFields.ENCODING_FIELD_NAME, encoding);
		mpEntity.addPart(HttpCommandFields.LANGUAGE_MODEL_FLAG, lmFlag);
		mpEntity.addPart(HttpCommandFields.ENDPOINTING_FLAG, endpointFlag);
		mpEntity.addPart(HttpCommandFields.CONTINUOUS_FLAG,continuousFlag);
		mpEntity.addPart(HttpCommandFields.CMN_BATCH, batchModeFlag);
		

		//add the grammar part
		mpEntity.addPart("grammar", grammarBody);
		
        speechStarted = false;
        requestCanceled = false;
	    epStream.startAudioTransfer(timeout, listener);
		
        //one part is the audio
        InputStreamBody audioBody = new InputStreamBody(epStream.getInputStream(), epStream.getMimeType(),"audio.wav");      
		//add the audio part
		mpEntity.addPart("audio", audioBody);
				
		//set the multipart entity for the post command
	    httppost.setEntity(mpEntity);
    	_logger.fine("Waiting for Speech start ...");
     	//now wait for a start speech event
		while ((!speechStarted)&&(!requestCanceled)) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                	_logger.fine("Interrupt Excepton "+speechStarted);
                }
            }
        }
		if (requestCanceled) {
	    	_logger.fine("Request  canceled!!!");
			return(null);
		}
		
    	_logger.fine("Speech started!!!");

	    //execute the post command
        _logger.fine("executing request " + httppost.getRequestLine());
        HttpResponse response = null;
        try {
	        response = httpclient.execute(httppost);
        } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        
        //get the response from the post
        HttpEntity resEntity = response.getEntity();

        _logger.fine("----------------------------------------");
        _logger.fine(response.getStatusLine().toString());
        if (resEntity != null) {
        	_logger.fine("Response content length: " + resEntity.getContentLength());
        	_logger.fine("Chunked?: " + resEntity.isChunked());
        }
        RecognitionResult r = null;
        if (resEntity != null) {
            try {
                InputStream s = resEntity.getContent();
                String result = readInputStreamAsString(s);
                _logger.fine(result);
                r = RecognitionResult.constructResultFromString(result);
	            resEntity.consumeContent();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (InvalidRecognitionResultException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
        }

        // send back event through the listener (decorator).  If the client did not pass in a eventListener (and it is null), decorator
        // will do nothing, else propagate the event to the client. 
        listener.recognitionComplete(r);
		return r;
	}
	
	
	
	/**
	 * Recognize.
	 * 
	 * @param format the format of the inputstreamd (note the is needed in plain input streams unlike audioInputstreams where this information is included in the stream.
	 * @param grammarUrl the grammar url.  If lmFlag is false, you must set the grammar file url.  (JSGF is supported)
	 * @param lmflg the lmflg.  If lmflga is true, recognition will use the default language mode on speechcloud server.
	 * @param inputStream the input stream
	 * @param mimeType the mime type
	 * 
	 * @return the recognition result
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws IllegalStateException the illegal state exception
	 */
	public InputStream transcribe(InputStream inputStream, AFormat format, String mimeType, URL grammarUrl, boolean lmflg) throws IllegalStateException, IOException {
	    // Plain old http approach    	
    	HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(service);
    	
    	//create the multipart entity
    	MultipartEntity mpEntity = new MultipartEntity();
    	
    	
    	// one part is the grammar
        InputStreamBody grammarBody = null;
        if (!lmflg) {
	        try {
	        	grammarBody = new InputStreamBody(grammarUrl.openStream(), "plain/text","grammar.gram");
	        } catch (IOException e1) {
		        // TODO Auto-generated catch block
		        e1.printStackTrace();
	        }
        }
        
        _logger.fine("Actual format: " + format);    	
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody lmFlag = null;
    	StringBody endpointFlag = null;
    	StringBody batchModeFlag = null;
    	StringBody continuousFlag = null;
        try {
        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	lmFlag = new StringBody(String.valueOf(lmflg));
        	endpointFlag = new StringBody(String.valueOf(Boolean.TRUE));
        	batchModeFlag = new StringBody(String.valueOf(Boolean.TRUE));
           	continuousFlag = new StringBody(String.valueOf(Boolean.TRUE));
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
		mpEntity.addPart(HttpCommandFields.SAMPLE_RATE_FIELD_NAME, sampleRate);
		mpEntity.addPart(HttpCommandFields.BIG_ENDIAN_FIELD_NAME, bigEndian);
		mpEntity.addPart(HttpCommandFields.BYTES_PER_VALUE_FIELD_NAME, bytesPerValue);
		mpEntity.addPart(HttpCommandFields.ENCODING_FIELD_NAME, encoding);
		mpEntity.addPart(HttpCommandFields.LANGUAGE_MODEL_FLAG, lmFlag);
		mpEntity.addPart(HttpCommandFields.ENDPOINTING_FLAG, endpointFlag);
		mpEntity.addPart(HttpCommandFields.CMN_BATCH, batchModeFlag);
		mpEntity.addPart(HttpCommandFields.CONTINUOUS_FLAG,continuousFlag);

       	
		String fname = null;
		if (mimeType.equals( "audio/x-wav")) {
			fname = "audio.wav";
		} else if(mimeType.equals( "audio/x-wav")) {
			fname = "audio.au";
		} else {
			fname = "audio.wav";
			_logger.info("unhanlded mime type "+mimeType);
		}


        InputStreamBody audioBody = new InputStreamBody(inputStream, mimeType,fname);      

        if (!lmflg)
        	mpEntity.addPart("grammar", grammarBody);
		
        mpEntity.addPart("audio", audioBody);      
        httppost.setEntity(mpEntity);
         
        _logger.fine("executing request " + httppost.getRequestLine());
        HttpResponse response = null;
        try {
	        response = httpclient.execute(httppost);
        } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        HttpEntity resEntity = response.getEntity();

        _logger.fine(response.getStatusLine().toString());
        if (resEntity != null) {
        	_logger.fine("Response content length: " + resEntity.getContentLength());
        	_logger.fine("Chunked?: " + resEntity.isChunked());

            Header[] headers = response.getAllHeaders();
            for (int i=0; i<headers.length; i++) {
            	_logger.fine(headers[i].toString());
            }
        }
        
        
        RecognitionResult r = null;
        if (resEntity != null) {
            return resEntity.getContent();
        } else {
        	return null;
        }


    }

	
	
	/**
	 * Read input stream  and return it as a stream.
	 * 
	 * @param in the inputSTream
	 * 
	 * @return the string representation of the inputStream
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected  String readInputStreamAsString(InputStream in) throws IOException {

		BufferedInputStream bis = new BufferedInputStream(in);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while(result != -1) {
			byte b = (byte)result;
			buf.write(b);
			result = bis.read();
		}        
		_logger.fine(buf.toString());
		return buf.toString();
	}
 
	
    /**
     * The listener interface for receiving mySpeechEvent events.
     * The class that is interested in processing a mySpeechEvent
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's <code>addMySpeechEventListener<code> method. When
     * the mySpeechEvent event occurs, that object's appropriate
     * method is invoked.
     * 
     * @see MySpeechEventEvent
     */
    private class MySpeechEventListener extends SpeechEventListenerDecorator {
   
        /**
         * TODOC.
         * 
         * @param speechEventListener the speech event listener
         */
        public MySpeechEventListener(SpeechEventListener speechEventListener) {
            super(speechEventListener);
        }

    	
		/* (non-Javadoc)
		 * @see com.spokentech.speechdown.client.SpeechEventListener#speechEnded()
		 */
		public void speechEnded() {
		    _logger.fine("HttpRec: Speech Ended");
            super.speechEnded();
	    }

	    /* (non-Javadoc)
    	 * @see com.spokentech.speechdown.client.SpeechEventListener#speechStarted()
    	 */
    	public void speechStarted() {
		    _logger.fine("HttpRec: Speech Started");
			//signal for the blocking call to check for unblocking
			synchronized (HttpRecognizer.this) {
				speechStarted=true;
				HttpRecognizer.this.notifyAll();
			}
            super.speechStarted();
	    }

		/* (non-Javadoc)
		 * @see com.spokentech.speechdown.client.SpeechEventListener#noInputTimeout()
		 */
		@Override
        public void noInputTimeout() {
		    _logger.fine("No input timeout"); 
            super.noInputTimeout();
        }
    }
    
    /**
     * The Class NoInputTimeoutTask.
     */
    public class NoInputTimeoutTask extends TimerTask {

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            synchronized (HttpRecognizer.this) {
                //_noInputTimeoutTask = null;
                // TODO: forward on the evnt to the listener
                // TODO: check the state of sych calls and notifyall, if waiting
                // TODO: could be a timeout for a asynch call, 

                }
        }
        
    }

    
  
	/**
	 * Cancel synchronous request.  Use this if doing client side endpointing
	 * Limitation is that it will not cancel a request after startspeech has been detected. 
	 * Or in general if the http requests has been executed. 
	 */
	public void cancel() {
		//TODO: cancel httpclient requests, following just unblocks the local endpointer.
		synchronized (this) {
			requestCanceled=true;
			this.notifyAll();
		}  
    } 
    
	/**
	 * Cancel asych requests with the given identifier.  Usse this if doing client side endpointing
	 * Limitation is that it will not cancel a request after startspeech has been detected. 
	 * Or in general if the http requests has been executed. 
	 * 
	 * @param id the id
	 */
	public void cancel(String id) {
	    workQ.cancel(id);
    }
	
	
}
