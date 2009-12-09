package com.spokentech.speechdown.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;

import javax.sound.sampled.TargetDataLine;


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


import junit.framework.TestCase;

public class RestfulTransccibeTest extends TestCase {

    private static Logger _logger = Logger.getLogger(RestfulTransccibeTest.class);
    public static final String CRLF = "\r\n";
    
    

    //private static String service = "http://ec2-67-202-5-50.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";  
    private static String service = "http://localhost:8090/speechcloud/SpeechUploadServlet";    
    private static AudioFormat desiredFormat;
    private static int sampleRate = 8000;
    private static boolean signed = true;
    private static boolean bigEndian = true;
    private static int channels = 1;
    private static int sampleSizeInBits = 16;
    
    private static int audioBufferSize = 160000;
    private static int msecPerRead = 10;
    private static int frameSizeInBytes;
	
	     protected void setUp() {
	    	 

	     }

	 

	    public void testRecognize1() {
	        AudioInputStream audioStream = null;
	    	
	    	_logger.info("Starting Recogniizer ...");

	    	File soundFile0 = new File("c:/work/speechcloud/etc/prompts/cubanson.wav");
	    	//File soundFile0 = new File("c:/work/speechcloud/etc/prompts/fabfour.wav");
	    	File soundFile1 = new File("c:/work/speechcloud/etc/prompts/lookupsports.wav");	 	
	    	File soundFile2 = new File("c:/work/speechcloud/etc/prompts/get_me_a_stock_quote.wav");	 	
	    	File soundFile3 = new File("c:/work/speechcloud/etc/prompts/i_would_like_sports_news.wav");	 	
	    	//File soundFile4 = new File("c:/temp/38.wav");	 	
	    	
	    	

    		
	        doTranscribeTest(soundFile0);
	        //doTranscribeTest(soundFile1);
	        doTranscribeTest(soundFile2);
	        doTranscribeTest(soundFile3);

	    }


		private void doTranscribeTest(File soundFile1) {
	        desiredFormat = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	        DataLine.Info info = new DataLine.Info(TargetDataLine.class, desiredFormat);
	        if (!AudioSystem.isLineSupported(info)) {
	            _logger.info(desiredFormat + " not supported");
	            throw new RuntimeException("unsupported audio format");
	        } 
	        
	        _logger.info("Desired format: " + desiredFormat);
	 

	    	// read in the sound file.
	    	AudioInputStream	audioInputStream = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile1);
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
	    	
	    	// Plain old http approach    	
	    	HttpClient httpclient = new DefaultHttpClient();
	        HttpPost httppost = new HttpPost(service);
	    	
	    	//create the multipart entity
	    	MultipartEntity mpEntity = new MultipartEntity();
	    	
	    	
	        
	        // get the audio format and send as form fields.  Cound not send aduio file with format included because audio files do not
	        // support mark/reset.  That is needed for stremaing using http chunk encoding on the servlet side using file upload.
	        AudioFormat format = audioInputStream.getFormat();
	        _logger.info("Actual format: " + format);    	
	    	StringBody sampleRate = null;
	    	StringBody bigEndian = null;
	    	StringBody bytesPerValue = null;
	    	StringBody encoding = null;
	    	StringBody lmFlag = null;
	    	StringBody  recMode = null;
	    	
	        try {
	        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
	        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
	        	//bigEndian = new StringBody(String.valueOf(Boolean.FALSE));
	        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
	        	encoding = new StringBody(format.getEncoding().toString());
	        	lmFlag = new StringBody(String.valueOf(Boolean.TRUE));
	        	recMode = new StringBody("transcript");
	        } catch (UnsupportedEncodingException e1) {
		        // TODO Auto-generated catch block
		        e1.printStackTrace();
	        }
	        
	        //add the form field parts
			mpEntity.addPart("sampleRate", sampleRate);
			mpEntity.addPart("bigEndian", bigEndian);
			mpEntity.addPart("bytesPerValue", bytesPerValue);
			mpEntity.addPart("encoding", encoding);
			mpEntity.addPart("lmFlag", lmFlag);
			mpEntity.addPart("recMode", recMode);
			
			
	        // second part is the mic audio
	        InputStreamBody audioBody = new InputStreamBody(audioInputStream, "audio/x-wav","audio.wav");      

			mpEntity.addPart("audio", audioBody);
	        
	        httppost.setEntity(mpEntity);
	    
	        
	        System.out.println("executing request " + httppost.getRequestLine());
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

	        System.out.println("----------------------------------------");
	        System.out.println(response.getStatusLine());
	        if (resEntity != null) {
	            System.out.println("Response content length: " + resEntity.getContentLength());
	            System.out.println("Chunked?: " + resEntity.isChunked());

	        }
	        if (resEntity != null) {
	            try {
	                InputStream s = resEntity.getContent();
	                int c;
	                while ((c = s.read()) != -1) {
	                    System.out.write(c);
	                }

		            resEntity.consumeContent();
	            } catch (IOException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
	            }
	        }
        }
	 



}
