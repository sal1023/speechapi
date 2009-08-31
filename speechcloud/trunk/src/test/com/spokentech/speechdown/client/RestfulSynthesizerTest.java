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

import com.spokentech.speechdown.cli.MicRecognizer2;

import junit.framework.TestCase;

public class RestfulSynthesizerTest extends TestCase {

    private static Logger _logger = Logger.getLogger(MicRecognizer2.class);
    public static final String CRLF = "\r\n";
    
    

    //private static String service = "http://ec2-75-101-211-235.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";  
    private static String service = "http://localhost:8090/speechcloud/SpeechDownloadServlet";    
    private static AudioFormat format;
    private static int sampleRate = 8000;
    private static boolean signed = true;
    private static boolean bigEndian = true;
    private static int channels = 1;
    private static int sampleSizeInBits = 16;
    

	     protected void setUp() {
	    	 

	     }



	    public void testRecognize1() {

	    	
	    	_logger.info("Starting Recogniizer ...");
	    	 	
	       	// setup a shutdown hook to cleanup and send a SIP bye message even if there is a 
	    	// unexpected crash (ie ctrl-c)
	    	Runtime.getRuntime().addShutdownHook(new Thread() {
	    		public void run() {
	    			_logger.info("Caught the shutting down hook ...");
	    		}
	    	});
	        
    
    		    	
	        doSynthesisTest("this is a only a test");
	        doSynthesisTest("To be or not to be, that is the question.");
	        doSynthesisTest("A man, a plan, a canal, panama");
	        doSynthesisTest("If a server wants to start sending a response before knowing its total length (like with long script output), it might use the simple chunked transfer-encoding, which breaks the complete response into smaller chunks and sends them in series. ");
	

	    }

		private void doSynthesisTest(String text) {
			
	    	// Plain old http approach    	
	    	HttpClient httpclient = new DefaultHttpClient();
	        HttpPost httppost = new HttpPost(service);
	    	
	    	//create the multipart entity
	    	MultipartEntity mpEntity = new MultipartEntity();
	           
	        // get the audio format and send as form fields.  Cound not send aduio file with format included because audio files do not
	        // support mark/reset.  That is needed for stremaing using http chunk encoding on the servlet side using file upload.

	        
	        format = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);

	        _logger.info("Actual format: " + format);    	
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
