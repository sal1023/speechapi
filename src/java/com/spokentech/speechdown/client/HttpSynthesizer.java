package com.spokentech.speechdown.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.sound.sampled.AudioFormat;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

public class HttpSynthesizer {
    private static Logger _logger = Logger.getLogger(HttpSynthesizer.class);

    //format = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	public InputStream  synthesize(String text, String service, AudioFormat format,String mimeType) {
		
    	// Plain old http approach    	
    	HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(service);
    	
    	//create the multipart entity
    	MultipartEntity mpEntity = new MultipartEntity();
           
        // get the audio format and send as form fields.  Cound not send aduio file with format included because audio files do not
        // support mark/reset.  That is needed for stremaing using http chunk encoding on the servlet side using file upload.


        _logger.info("Format: " + format);    	
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	StringBody mime = null;
        try {
        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	mime = new StringBody("audio/x-au");
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
		mpEntity.addPart("sampleRate", sampleRate);
		mpEntity.addPart("bigEndian", bigEndian);
		mpEntity.addPart("bytesPerValue", bytesPerValue);
		mpEntity.addPart("encoding", encoding);
		mpEntity.addPart("mimeType", mime);
        
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

        _logger.info("----------------------------------------");
        _logger.info(response.getStatusLine());
        if (resEntity != null) {
            _logger.info("Response content length: " + resEntity.getContentLength());
            _logger.info("Chunked?: " + resEntity.isChunked());

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
