/**
 * 
 * The HttpSynthesizer class allows you to issue a synthesize command to a remote speech server.
 * The requests is sent to a http synthesizer server using http protocol.  The audio is returned in the http response.
 * Audio chunk encoded so that audio can be returned for use by the client as quickly as possible.
 * 
 *
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
package com.spokentech.speechdown.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;

import com.spokentech.speechdown.common.HttpCommandFields;


/**
 * The Class HttpSynthesizer is a lightweight java client to do speech recognition.  You need to have the url of server the nsimply make 
 * synthesis calls
 * <Pre>  
 *	String service = "http://myspeechservice.com/"
 *	HttpSynthesizer	synth = new HttpSynthesizer();
 *	synth.setService(service);
 *	String text = "There are eels in my hovercraft";
 *	Format format = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
 *	InputStream synthAduio= synth.synthesize(text, format,"audio/x-au");
 *	AudioPlayer(synthAudio);
 * </pre>
 *          
 * 
 */
public class HttpSynthesizer {
    private static Logger _logger = Logger.getLogger(HttpSynthesizer.class.getName());

    
    private String service;
    
    
    /**
     * Gets the service.
     * 
     * @return the service
     */
    public String getService() {
    	return service;
    }


	/**
	 * Sets the service uses in synthesis requests
	 * 
	 * @param service  -- the url of the speechcloud service
	 */
    public void setService(String service) {
    	this.service = service;
    }


	//format = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	/**
     * Synthesize the text string and the synthesized audio in the specifeid format.
     * 
     * @param text the text to be synthesized
     * @param service the service url for the speech service
     * @param format the format of the audio returned  from the speech service
     * @param mimeType the mime type of the audio returned from the service.
     * 
     * @return the input stream
     */
    public InputStream  synthesize(String text,  AudioFormat format, String mimeType, String voiceName) {
		
    	// Plain old http approach    	
    	HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(service);
    	
    	//create the multipart entity
    	MultipartEntity mpEntity = new MultipartEntity();
           
        // get the audio format and send as form fields.  Cound not send aduio file with format included because audio files do not
        // support mark/reset.  That is needed for stremaing using http chunk encoding on the servlet side using file upload.


        _logger.fine("Format: " + format);    	
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody mime = null;
    	StringBody voice = null;
    	StringBody textBody = null;
        try {
        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	mime = new StringBody(mimeType);
         	voice = new StringBody(voiceName);
          	textBody = new StringBody(text);

        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
		mpEntity.addPart(HttpCommandFields.SAMPLE_RATE_FIELD_NAME, sampleRate);
		mpEntity.addPart(HttpCommandFields.BIG_ENDIAN_FIELD_NAME, bigEndian);
		mpEntity.addPart(HttpCommandFields.BYTES_PER_VALUE_FIELD_NAME, bytesPerValue);
		mpEntity.addPart(HttpCommandFields.ENCODING_FIELD_NAME, encoding);
		mpEntity.addPart(HttpCommandFields.MIME_TYPE, mime);
		mpEntity.addPart(HttpCommandFields.VOICE_NAME, voice);
		mpEntity.addPart(HttpCommandFields.TEXT, textBody);
        
        
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

        _logger.fine("----------------------------------------");
        _logger.fine(response.getStatusLine().toString());
        if (resEntity != null) {
            _logger.fine("Response content length: " + resEntity.getContentLength());
            _logger.fine("Chunked?: " + resEntity.isChunked());

        }
        InputStream s = null;
        if (resEntity != null) {
            try {
                s = resEntity.getContent();
	            //resEntity.consumeContent();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
        }
        
        return(s);
    }

	
}
