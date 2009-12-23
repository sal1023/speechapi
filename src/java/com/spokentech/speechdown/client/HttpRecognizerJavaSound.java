package com.spokentech.speechdown.client;

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

import com.spokentech.speechdown.client.util.AFormat;
import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.common.HttpCommandFields;
import com.spokentech.speechdown.common.InvalidRecognitionResultException;
import com.spokentech.speechdown.common.RecognitionResult;

public class HttpRecognizerJavaSound extends HttpRecognizer {
	   private static Logger _logger = Logger.getLogger(HttpRecognizerJavaSound.class);
	   	
	
	/**
	 * Recognize. The audio in the file are return th result.  This is a blocking call
	 * 
	 * @param fileName the file name
	 * @param grammarUrl the grammar url.  If lmFlag is false, you must set the grammar file url.  (JSGF is supported)
	 * @param lmflg the lmflg.  If lmflga is true, recognition will use the default language mode on speechcloud server.
	 * 
	 * @return the recognition result
	 */
	public RecognitionResult recognize(String  fileName, URL grammarUrl, boolean lmflg, boolean doEndpointing, boolean batchMode) {
		
		
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
    	return recognize(audioInputStream, type, grammarUrl, lmflg, doEndpointing,batchMode);
    	
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
	public RecognitionResult recognize(AudioInputStream audioInputStream, Type type, URL grammarUrl, boolean lmflg, boolean doEndpointing, boolean batchMode) {
        // get the audio format and send as form fields.  Cound not send aduio file with format included because audio files do not
        // support mark/reset.  That is needed for stremaing using http chunk encoding on the servlet side using file upload.
        AudioFormat format = audioInputStream.getFormat();
        AFormat f = FormatUtils.covertToNeutral(format);

		String mimeType = null;
		if (type == AudioFileFormat.Type.WAVE) {
			//Always a audio/x-wav
			mimeType = "audio/x-wav";
		} else if (type == AudioFileFormat.Type.AU) {
			mimeType = "audio/x-au";
		} else {
			_logger.warn("unhanlded format type "+type.getExtension());
		}
        
    	return recognize(audioInputStream, f, mimeType, grammarUrl, lmflg, doEndpointing, batchMode);
    }

	

	/**
	 * Recognize.  recognize audio from the local microphone/
	 * 
	 * @param audioLine the audio line of (must likely used for the local microphone)
	 * @param grammarUrl the grammar url.  If lmFlag is false, you must set the grammar file url.  (JSGF is supported)
	 * @param lmflg the lmflg.  If lmflga is true, recognition will use the default language mode on speechcloud server.
	 * @param doEndpointing the do endpointing
	 * @param batchMode the batch mode
	 * 
	 * @return the recognition result
	 */
	public  RecognitionResult recognize(TargetDataLine audioLine, URL grammarUrl, boolean lmflg, boolean doEndpointing, boolean batchMode) {

        
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
        _logger.debug("Actual format: " + format);    	
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody lmFlag = null;
    	StringBody endpointFlag = null;
    	StringBody continuousFlag = null;
    	StringBody batchModeFlag = null;
        try {
        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	lmFlag = new StringBody(String.valueOf(lmflg));
        	endpointFlag = new StringBody(String.valueOf(doEndpointing));
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
		mpEntity.addPart(HttpCommandFields.CMN_BATCH, batchModeFlag);
		mpEntity.addPart(HttpCommandFields.CONTINUOUS_FLAG,continuousFlag);
		
		//add the grammar part
		mpEntity.addPart("grammar", grammarBody);
		
		//add the audio part
		mpEntity.addPart("audio", audioBody);
		
		
		//set the multipart entity for the post command
	    httppost.setEntity(mpEntity);



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
        _logger.debug(response.getStatusLine());
        if (resEntity != null) {
        	_logger.debug("Response content length: " + resEntity.getContentLength());
        	_logger.debug("Chunked?: " + resEntity.isChunked());
        }
        RecognitionResult r = null;
        if (resEntity != null) {
            try {
                InputStream s = resEntity.getContent();
                String result = readInputStreamAsString(s);
                _logger.debug(result);
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
	

	
}
