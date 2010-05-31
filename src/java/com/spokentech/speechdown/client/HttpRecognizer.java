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

import com.google.gson.Gson;
import com.spokentech.speechdown.common.HttpCommandFields;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.TimerTask;

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
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.common.Utterance.OutputFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory; 


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
	
	private static Log _logger =  LogFactory.getLog(HttpRecognizer.class.getName());

	public HttpRecognizer(String devId, String key) {
		this.devId = devId;
		this.key = key;
	    gson = new Gson();
    }
	
	private volatile String devId;
	private volatile String key;
	

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
    
    private Gson gson = null;
    
  

     
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
        //   handlers[index].setLevel( Level.info );
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
	public String recognize(String userId, InputStream inputStream, AFormat format, String mimeType, URL grammarUrl, boolean lmflg, boolean doEndpointing, boolean batchMode, OutputFormat outMode) {
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
        
        _logger.debug("Actual format: " + format);  
        StringBody outputFormat = null;
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody lmFlag = null;
    	StringBody endpointFlag = null;
    	StringBody batchModeFlag = null;
    	StringBody continuousFlag = null;
    	StringBody dId = null;
    	StringBody uId = null;
    	StringBody keyy = null;
    	
        try {
        	if (format != null) {
	        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
	        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
	        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
	        	encoding = new StringBody(format.getEncoding().toString());
				mpEntity.addPart(HttpCommandFields.SAMPLE_RATE_FIELD_NAME, sampleRate);
				mpEntity.addPart(HttpCommandFields.BIG_ENDIAN_FIELD_NAME, bigEndian);
				mpEntity.addPart(HttpCommandFields.BYTES_PER_VALUE_FIELD_NAME, bytesPerValue);
				mpEntity.addPart(HttpCommandFields.ENCODING_FIELD_NAME, encoding);
        	}
        	if (outMode!= null) {
        		outputFormat = new StringBody(outMode.name());
    			mpEntity.addPart(HttpCommandFields.OUTPUT_MODE, outputFormat);
        	}
        	lmFlag = new StringBody(String.valueOf(lmflg));
        	endpointFlag = new StringBody(String.valueOf(doEndpointing));
        	batchModeFlag = new StringBody(String.valueOf(batchMode));
           	continuousFlag = new StringBody(String.valueOf(Boolean.FALSE));
           	
    		if (devId != null) {
            	dId = new StringBody(devId);
    		    mpEntity.addPart(HttpCommandFields.DEVELOPER_ID,dId);
    		}
    		if (userId != null) {
            	uId = new StringBody(userId);
    		    mpEntity.addPart(HttpCommandFields.USER_ID,uId);
    		}
    		if (key != null) {
            	keyy = new StringBody(key);
    		    mpEntity.addPart(HttpCommandFields.DEVELOPER_SECRET,keyy);
    		}
           	
           	
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        

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
			_logger.debug("unhanlded mime type "+ mimeType);
		}


        InputStreamBody audioBody = new InputStreamBody(inputStream, mimeType,fname);      

        if (!lmflg)
        	mpEntity.addPart("grammar", grammarBody);
		
        mpEntity.addPart("audio", audioBody);      
        httppost.setEntity(mpEntity);
         
        _logger.debug("executing request " + httppost.getRequestLine());
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

        _logger.debug(response.getStatusLine().toString());
        if (resEntity != null) {
        	_logger.debug("Response content length: " + resEntity.getContentLength());
        	_logger.debug("Chunked?: " + resEntity.isChunked());

            Header[] headers = response.getAllHeaders();
            for (int i=0; i<headers.length; i++) {
            	_logger.debug(headers[i].toString());
            }
        }
        
        String result = null;
        if (resEntity != null) {
            try {
                InputStream s = resEntity.getContent();
                result = readInputStreamAsString(s);
                _logger.debug(result);    
	            resEntity.consumeContent();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
        }
        
        return result;
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
	public  String recognizeAsynch(String dId, String dKey, String userId, URL grammarUrl, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, OutputFormat outMode, long timeout, SpeechEventListener eventListener ) throws InstantiationException, IOException, StreamInUseException, AsynchNotEnabledException {

		if (epStream.checkAndSetIfInUse()) 
			throw new StreamInUseException();

		InputStream grammarIs = null;
		if (grammarUrl != null) 
		    grammarIs = grammarUrl.openStream();
		AsynchCommand command = null;
		if (workQ != null) {
		   command = new AsynchCommand(dId, userId, dKey, AsynchCommand.CommandType.recognize, service, grammarIs, epStream, lmflg, batchMode, outMode, timeout, eventListener);
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
	public  String recognizeAsynch(String dId, String dKey,String userId, String grammar, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, OutputFormat outMode, long timeout, SpeechEventListener eventListener ) throws InstantiationException, IOException, StreamInUseException, AsynchNotEnabledException {

		if (epStream.checkAndSetIfInUse()) 
			throw new StreamInUseException();
		
		InputStream grammarIs = new ByteArrayInputStream(grammar.getBytes());
		AsynchCommand command = null;
		if (workQ != null) {
		   command = new AsynchCommand(dId, userId, dKey, AsynchCommand.CommandType.recognize, service, grammarIs, epStream, lmflg, batchMode, outMode, timeout, eventListener);
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
	public  String recognize(String userId, URL grammarUrl, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, OutputFormat outMode, long timeout ) throws InstantiationException, IOException {
		return recognize(userId,  grammarUrl,  epStream,  lmflg,  batchMode,  outMode,timeout, null);
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
	public  String recognize(String userId, String grammar, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, OutputFormat outMode, long timeout, SpeechEventListener eventListener) throws InstantiationException, IOException {
		//String charset = Charset.defaultCharset;
		InputStream is = new ByteArrayInputStream(grammar.getBytes());
		String r = recognize(userId, is, epStream,lmflg,batchMode, outMode,timeout,eventListener);
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
	public  String recognize(String userId, URL grammarUrl, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, OutputFormat outMode, long timeout, SpeechEventListener eventListener) throws InstantiationException, IOException {

		InputStream is = null;
		if (grammarUrl!=null)
			is = grammarUrl.openStream();

		String r = recognize(userId, is, epStream,lmflg,batchMode,outMode,timeout,eventListener);
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
	public  String recognize(String userId, InputStream grammar, EndPointingInputStream epStream, boolean lmflg, boolean batchMode, OutputFormat outMode, long timeout, SpeechEventListener eventListener) throws InstantiationException, IOException {

        //create the listener (listens for end points)  A decorator, so also can  pass events back to client
        speechStarted = false;
        requestCanceled = false;
        MySpeechEventListener listener = new MySpeechEventListener(eventListener);

    	// Plain old http approach    	
    	HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(service);
    	
    	//create the multipart entity
    	MultipartEntity mpEntity = new MultipartEntity();
    	
    	InputStreamBody grammarBody =null;
    	if (!lmflg) {
    	   // one part is the grammar
           grammarBody = new InputStreamBody(grammar, "plain/text","grammar.gram");
    	}

        //parameters are fields (StringBoodys)
        StringBody outputFormat = null;
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody lmFlag = null;
    	StringBody endpointFlag = null;
    	StringBody continuousFlag = null;
    	StringBody batchModeFlag = null;
    	
    	StringBody dId = null;
    	StringBody uId = null;
    	StringBody keyy = null;
    	
    	AFormat format = epStream.getFormat();

        try {
        	if (format != null) {
	        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
	        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
	        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
	        	encoding = new StringBody(format.getEncoding().toString());
				mpEntity.addPart(HttpCommandFields.SAMPLE_RATE_FIELD_NAME, sampleRate);
				mpEntity.addPart(HttpCommandFields.BIG_ENDIAN_FIELD_NAME, bigEndian);
				mpEntity.addPart(HttpCommandFields.BYTES_PER_VALUE_FIELD_NAME, bytesPerValue);
				mpEntity.addPart(HttpCommandFields.ENCODING_FIELD_NAME, encoding);
        	}
        	if (outMode!= null) {
        		outputFormat = new StringBody(outMode.name());
    			mpEntity.addPart(HttpCommandFields.OUTPUT_MODE, outputFormat);
        	}
        	lmFlag = new StringBody(String.valueOf(lmflg));
           	endpointFlag = new StringBody(String.valueOf(epStream.getEndPointer().requiresServerSideEndPointing()));
           	continuousFlag = new StringBody(String.valueOf(Boolean.FALSE));
        	batchModeFlag = new StringBody(String.valueOf(batchMode));
        	
    		if (devId != null) {
            	dId = new StringBody(devId);
    		    mpEntity.addPart(HttpCommandFields.DEVELOPER_ID,dId);
    		}
    		if (userId != null) {
            	uId = new StringBody(userId);
    		    mpEntity.addPart(HttpCommandFields.USER_ID,uId);
    		}
    		if (key != null) {
            	keyy = new StringBody(key);
    		    mpEntity.addPart(HttpCommandFields.DEVELOPER_SECRET,keyy);
    		}
        	
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
		//mpEntity.addPart("dataMode", dataStreamMode);

		mpEntity.addPart(HttpCommandFields.LANGUAGE_MODEL_FLAG, lmFlag);
		mpEntity.addPart(HttpCommandFields.ENDPOINTING_FLAG, endpointFlag);
		mpEntity.addPart(HttpCommandFields.CONTINUOUS_FLAG,continuousFlag);
		mpEntity.addPart(HttpCommandFields.CMN_BATCH, batchModeFlag);
		
	

		

		//add the grammar part
    	if (!lmflg) {
		   mpEntity.addPart("grammar", grammarBody);
    	}
    	
        speechStarted = false;
        requestCanceled = false;
	    epStream.startAudioTransfer(timeout, listener);
		
        //one part is the audio
        InputStreamBody audioBody = new InputStreamBody(epStream.getInputStream(), epStream.getMimeType(),"audio.wav");      
		//add the audio part
		mpEntity.addPart("audio", audioBody);
				
		//set the multipart entity for the post command
	    httppost.setEntity(mpEntity);
    	_logger.debug("Waiting for Speech start ...");
     	//now wait for a start speech event
		while ((!speechStarted)&&(!requestCanceled)) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                	_logger.info("Interrupt Excepton "+speechStarted);
                }
            }
        }
		if (requestCanceled) {
	    	_logger.debug("Request  canceled!!!");
			return(null);
		}
		
    	_logger.debug("Speech started!!!");

	    //execute the post command
        _logger.debug("executing request " + httppost.getRequestLine());
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

        _logger.debug("----------------------------------------");
        _logger.debug(response.getStatusLine().toString());
        if (resEntity != null) {
        	_logger.debug("Response content length: " + resEntity.getContentLength());
        	_logger.debug("Chunked?: " + resEntity.isChunked());
        }

        String result = null;
        if (resEntity != null) {
            try {
                InputStream s = resEntity.getContent();
                result = readInputStreamAsString(s);
                _logger.debug(result);
	            resEntity.consumeContent();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
        }

        // send back event through the listener (decorator).  If the client did not pass in a eventListener (and it is null), decorator
        // will do nothing, else propagate the event to the client. 
        
        Utterance u = null;
        if (outMode == OutputFormat.json) {
        	u = gson.fromJson(result, Utterance.class);   
        } else  if (outMode == OutputFormat.text) {
        	u = new Utterance();
        	u.setText(result);
        } else {
        	_logger.debug("Response unrecognized output Format, using text "+outMode);

        }
        
        listener.recognitionComplete(u);
		return result;
	}
	
	
	
	/**
	 * Transcribe.
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
	public InputStream transcribe(String userId,  InputStream inputStream, AFormat format, String mimeType, URL grammarUrl, boolean lmflg, OutputFormat outMode) throws IllegalStateException, IOException {
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
        
        _logger.debug("Actual format: " + format);  
        StringBody outputFormat = null;
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody lmFlag = null;
    	StringBody endpointFlag = null;
    	StringBody batchModeFlag = null;
    	StringBody continuousFlag = null;
    	
    	StringBody dId = null;
    	StringBody uId = null;
    	StringBody keyy = null;
    	
        try {
        	if (format != null) {
	        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
	        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
	        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
	        	encoding = new StringBody(format.getEncoding().toString());
				mpEntity.addPart(HttpCommandFields.SAMPLE_RATE_FIELD_NAME, sampleRate);
				mpEntity.addPart(HttpCommandFields.BIG_ENDIAN_FIELD_NAME, bigEndian);
				mpEntity.addPart(HttpCommandFields.BYTES_PER_VALUE_FIELD_NAME, bytesPerValue);
				mpEntity.addPart(HttpCommandFields.ENCODING_FIELD_NAME, encoding);
        	}
        	if (outMode!= null) {
        		outputFormat = new StringBody(outMode.name());
    			mpEntity.addPart(HttpCommandFields.OUTPUT_MODE, outputFormat);
        	}
        	lmFlag = new StringBody(String.valueOf(lmflg));
        	endpointFlag = new StringBody(String.valueOf(Boolean.TRUE));
        	batchModeFlag = new StringBody(String.valueOf(Boolean.TRUE));
           	continuousFlag = new StringBody(String.valueOf(Boolean.TRUE));
           	
    		if (devId != null) {
            	dId = new StringBody(devId);
    		    mpEntity.addPart(HttpCommandFields.DEVELOPER_ID,dId);
    		}
    		if (userId != null) {
            	uId = new StringBody(userId);
    		    mpEntity.addPart(HttpCommandFields.USER_ID,uId);
    		}
    		if (key != null) {
            	keyy = new StringBody(key);
    		    mpEntity.addPart(HttpCommandFields.DEVELOPER_SECRET,keyy);
    		}
           	
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
    	if (format != null) {
			mpEntity.addPart(HttpCommandFields.SAMPLE_RATE_FIELD_NAME, sampleRate);
			mpEntity.addPart(HttpCommandFields.BIG_ENDIAN_FIELD_NAME, bigEndian);
			mpEntity.addPart(HttpCommandFields.BYTES_PER_VALUE_FIELD_NAME, bytesPerValue);
			mpEntity.addPart(HttpCommandFields.ENCODING_FIELD_NAME, encoding);
    	}
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
			_logger.debug("unhanlded mime type "+mimeType);
		}


        InputStreamBody audioBody = new InputStreamBody(inputStream, mimeType,fname);      

        if (!lmflg)
        	mpEntity.addPart("grammar", grammarBody);
		
        mpEntity.addPart("audio", audioBody);      
        httppost.setEntity(mpEntity);
         
        _logger.debug("executing request " + httppost.getRequestLine());
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

        _logger.debug(response.getStatusLine().toString());
        if (resEntity != null) {
        	_logger.debug("Response content length: " + resEntity.getContentLength());
        	_logger.debug("Chunked?: " + resEntity.isChunked());

            Header[] headers = response.getAllHeaders();
            for (int i=0; i<headers.length; i++) {
            	_logger.debug(headers[i].toString());
            }
        }
        
        
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
		_logger.debug(buf.toString());
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
		    _logger.debug("HttpRec: Speech Ended");
            super.speechEnded();
	    }

	    /* (non-Javadoc)
    	 * @see com.spokentech.speechdown.client.SpeechEventListener#speechStarted()
    	 */
    	public void speechStarted() {
		    _logger.debug("HttpRec: Speech Started");
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
		    _logger.debug("No input timeout"); 
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
