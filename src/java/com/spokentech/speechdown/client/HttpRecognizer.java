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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFileFormat.Type;

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
import org.apache.log4j.Logger;

import com.spokentech.speechdown.client.endpoint.EndPointingInputStream;
import com.spokentech.speechdown.common.InvalidRecognitionResultException;
import com.spokentech.speechdown.common.RecognitionResult;

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

    private static Logger _logger = Logger.getLogger(HttpRecognizer.class);
 
    private  String service = "http://localhost:8090/speechcloud/SpeechUploadServlet";    

	//Default values (if not specified as a parameter)
    private static int sampleRate = 8000;
    private static boolean signed = true;
    private static boolean bigEndian = true;
    private static int channels = 1;
    private static int sampleSizeInBits = 16;
    
	private boolean speechStarted = false;
    
    
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
    }


	/**
	 * Recognize. The audio in the file are return th result.  This is a blocking call
	 * 
	 * @param fileName the file name
	 * @param grammarUrl the grammar url.  If lmFlag is false, you must set the grammar file url.  (JSGF is supported)
	 * @param lmflg the lmflg.  If lmflga is true, recognition will use the default language mode on speechcloud server.
	 * 
	 * @return the recognition result
	 */
	public RecognitionResult recognize(String  fileName, URL grammarUrl, boolean lmflg, boolean doEndpointing) {
		
		
    	File soundFile = new File(fileName);	 
    	
    	// read in the sound file.
    	AudioInputStream audioInputStream = null;
    	Type type = null;
    	try {
    		audioInputStream = AudioSystem.getAudioInputStream(soundFile);
    		type = AudioSystem.getAudioFileFormat(soundFile).getType();

    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return recognize(audioInputStream, type, grammarUrl, lmflg, doEndpointing);
    	
	}
	


	/**
	 * Recognize. This method will recognize th audiostream
	 * 
	 * @param audioInputStream the audio input stream
	 * @param type the audiostream  (AudioFileFormat.Type.WAVE,  AudioFileFormat.Type.AU) 
	 * @param grammarUrl the grammar url.  If lmFlag is false, you must set the grammar file url.  (JSGF is supported)
	 * @param lmflg the lmflg.  If lmflga is true, recognition will use the default language mode on speechcloud server.
	 * 
	 * @return the recognition result
	 */
	public RecognitionResult recognize(AudioInputStream audioInputStream, Type type, URL grammarUrl, boolean lmflg, boolean doEndpointing) {
        // get the audio format and send as form fields.  Cound not send aduio file with format included because audio files do not
        // support mark/reset.  That is needed for stremaing using http chunk encoding on the servlet side using file upload.
        AudioFormat format = audioInputStream.getFormat();
		
    	return recognize(audioInputStream, format, type, grammarUrl, lmflg, doEndpointing);
    }

	/**
	 * Recognize.
	 * 
	 * @param inputStream the input stream of the audiofile
	 * @param format the format of the inputstreamd (note the is needed in plain input streams unlike audioInputstreams where this information is included in the stream.
	 * @param type the audiostream  (AudioFileFormat.Type.WAVE,  AudioFileFormat.Type.AU) 
	 * @param grammarUrl the grammar url.  If lmFlag is false, you must set the grammar file url.  (JSGF is supported)
	 * @param lmflg the lmflg.  If lmflga is true, recognition will use the default language mode on speechcloud server.
	 * 
	 * @return the recognition result
	 */
	public RecognitionResult recognize(InputStream inputStream, AudioFormat format, Type type, URL grammarUrl, boolean lmflg, boolean doEndpointing) {
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


        _logger.info("Actual format: " + format);    	
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody lmFlag = null;
    	StringBody endpointFlag = null;
        try {
        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	lmFlag = new StringBody(String.valueOf(lmflg));
        	endpointFlag = new StringBody(String.valueOf(doEndpointing));
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
		
		String mimeType = null;
		String fname = null;
		if (type == AudioFileFormat.Type.WAVE) {
			mimeType = "audio/x-wav";
			fname = "audio.wav";
		} else if (type == AudioFileFormat.Type.AU) {
			mimeType = "audio/x-au";
			fname = "audio.au";
		} else {
			_logger.warn("unhanlded format type "+type.getExtension());
		}
        InputStreamBody audioBody = new InputStreamBody(inputStream, mimeType,fname);      

        if (!lmflg)
        	mpEntity.addPart("grammar", grammarBody);
		
        mpEntity.addPart("audio", audioBody);      
        httppost.setEntity(mpEntity);
         
        _logger.info("executing request " + httppost.getRequestLine());
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

        _logger.info(response.getStatusLine());
        if (resEntity != null) {
        	_logger.info("Response content length: " + resEntity.getContentLength());
        	_logger.info("Chunked?: " + resEntity.isChunked());

            Header[] headers = response.getAllHeaders();
            for (int i=0; i<headers.length; i++) {
            	_logger.info(headers[i]);
            }
        }
        
        
        RecognitionResult r = null;
        if (resEntity != null) {
            try {
                InputStream s = resEntity.getContent();
                String result = readInputStreamAsString(s);
                _logger.info(result);
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
	 * Recognize.  recognize audio from the local microphone/
	 * 
	 * @param audioLine the audio line of (must likely used for the local microphone)
	 * @param grammarUrl the grammar url.  If lmFlag is false, you must set the grammar file url.  (JSGF is supported)
	 * @param lmflg the lmflg.  If lmflga is true, recognition will use the default language mode on speechcloud server.
	 * 
	 * @return the recognition result
	 */
	public  RecognitionResult recognize(TargetDataLine audioLine, URL grammarUrl, boolean lmflg, boolean doEndpointing) {

        
        //create the thread and start it
        AudioLine2InputStream  line2Stream = new AudioLine2InputStream("Mic",audioLine);
        line2Stream.start();

    	// Plain old http approach
    	HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(service);
    	
    	//create the multipart entity
    	MultipartEntity mpEntity = new MultipartEntity();


    	// one part is the grammar
        InputStreamBody grammarBody = null;
        try {
        	grammarBody = new InputStreamBody(grammarUrl.openStream(), "plain/text","grammar.gram");
        } catch (IOException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }

    	String mimeType = "audio/x-wav";
    	String fname = "audio.wav";
        //one part is the audio
        InputStreamBody audioBody = new InputStreamBody(line2Stream.getIstream(), mimeType,fname);      
        
        // get the audio format and send as form fields.  Cound not send aduio file with format included because audio files do not
        // support mark/reset.  That is needed for stremaing using http chunk encoding on the servlet side using file upload.
		AudioInputStream audioStream = new AudioInputStream(audioLine);
        AudioFormat format = audioStream.getFormat();
        _logger.info("Actual format: " + format);    	
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody lmFlag = null;
    	StringBody endpointFlag = null;
    	//StringBody dataStreamMode = null;
        try {
        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	lmFlag = new StringBody(String.valueOf(lmflg));
        	endpointFlag = new StringBody(String.valueOf(doEndpointing));
        	//dataStreamMode = new StringBody(dataMode);
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
		
		//add the grammar part
		mpEntity.addPart("grammar", grammarBody);
		
		//add the audio part
		mpEntity.addPart("audio", audioBody);
		
		
		//set the multipart entity for the post command
	    httppost.setEntity(mpEntity);



	    //execute the post command
        _logger.info("executing request " + httppost.getRequestLine());
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

        _logger.info("----------------------------------------");
        _logger.info(response.getStatusLine());
        if (resEntity != null) {
        	_logger.info("Response content length: " + resEntity.getContentLength());
        	_logger.info("Chunked?: " + resEntity.isChunked());
        }
        RecognitionResult r = null;
        if (resEntity != null) {
            try {
                InputStream s = resEntity.getContent();
                String result = readInputStreamAsString(s);
                _logger.info(result);
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
	 * Recognize.  This method will do require and endpointing stream as input (containing the audio).  The endpointstream will do endpoing (here on the client)
	 * This can save bandwidth aty the cost of processing on the client.
	 * 

	 * @param epStream the end-pointing stream.  Create a epStream (wraps a inputStream). and use in this method to do endpoint on the client.
	 * @param grammarUrl the grammar url.  If lmFlag is false, you must set the grammar file url.  (JSGF is supported)
	 * @param lmflg the lmflg.  If lmflga is true, recognition will use the default language mode on speechcloud server.
	 * @param timeout the timeout
	 * 
	 * @return the recognition result
	 * 
	 * @throws InstantiationException the instantiation exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public  RecognitionResult recognize(URL grammarUrl, EndPointingInputStream epStream, boolean lmflg, long timeout) throws InstantiationException, IOException {


        //create the listener (listens for end points)
        MySpeechEventListener listener = new MySpeechEventListener();

	    epStream.startAudioTransfer(timeout, listener);


    	// Plain old http approach    	
    	HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(service);
    	
    	//create the multipart entity
    	MultipartEntity mpEntity = new MultipartEntity();
    	
    	// one part is the grammar
        InputStreamBody grammarBody = null;
        try {
        	grammarBody = new InputStreamBody(grammarUrl.openStream(), "plain/text","grammar.gram");
        } catch (IOException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
  
        //one part is the audio
        InputStreamBody audioBody = new InputStreamBody(epStream.getInputStream(), epStream.getMimeType(),"audio.wav");      
        
        //parameters are fields (StringBoodys)
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody lmFlag = null;
    	//StringBody dataStreamMode = new StringBody(dataMode);  	
    	AudioFormat format = epStream.getFormat1();
    	
        try {
           	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	lmFlag = new StringBody(String.valueOf(lmflg));
        	//dataStreamMode = new StringBody(dataMode);
        	
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
		//mpEntity.addPart("dataMode", dataStreamMode);
		mpEntity.addPart("sampleRate", sampleRate);
		mpEntity.addPart("bigEndian", bigEndian);
		mpEntity.addPart("bytesPerValue", bytesPerValue);
		mpEntity.addPart("encoding", encoding);
		mpEntity.addPart("lmFlag", lmFlag);
		
		//add the grammar part
		mpEntity.addPart("grammar", grammarBody);
		
		//add the audio part
		mpEntity.addPart("audio", audioBody);
				
		//set the multipart entity for the post command
	    httppost.setEntity(mpEntity);

     	//now wait for a start speech event
        speechStarted = false;
		while (!speechStarted) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                	_logger.debug("Interrupt Excepton while waiting for tts to complete");
                }
            }
        }
    	_logger.info("Speech started!");

	    
	    //execute the post command
        _logger.info("executing request " + httppost.getRequestLine());
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

        _logger.info("----------------------------------------");
        _logger.info(response.getStatusLine());
        if (resEntity != null) {
        	_logger.info("Response content length: " + resEntity.getContentLength());
        	_logger.info("Chunked?: " + resEntity.isChunked());
        }
        RecognitionResult r = null;
        if (resEntity != null) {
            try {
                InputStream s = resEntity.getContent();
                String result = readInputStreamAsString(s);
                _logger.info(result);
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
	 * Read input stream  and return it as a stream
	 * 
	 * @param in the inputSTream
	 * 
	 * @return the string representation of the inputStream
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private  String readInputStreamAsString(InputStream in) throws IOException {

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
    private class MySpeechEventListener implements SpeechEventListener {
    
		/* (non-Javadoc)
		 * @see com.spokentech.speechdown.client.SpeechEventListener#speechEnded()
		 */
		public void speechEnded() {
		    _logger.info("Speech Ended");
	    }

	    /* (non-Javadoc)
    	 * @see com.spokentech.speechdown.client.SpeechEventListener#speechStarted()
    	 */
    	public void speechStarted() {
		    _logger.info("Speech Started");
			//signal for the blocking call to check for unblocking
			synchronized (this) {
				speechStarted=true;
				this.notifyAll();
			}
	    }

		/* (non-Javadoc)
		 * @see com.spokentech.speechdown.client.SpeechEventListener#noInputTimeout()
		 */
		@Override
        public void noInputTimeout() {
		    _logger.info("No input timeout");   
        }
    }
	
}