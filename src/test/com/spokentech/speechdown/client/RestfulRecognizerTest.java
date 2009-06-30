package com.spokentech.speechdown.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import com.spokentech.speechdown.cli.MicRecognizer2;

import junit.framework.TestCase;

public class RestfulRecognizerTest extends TestCase {

    private static Logger _logger = Logger.getLogger(MicRecognizer2.class);
    public static final String CRLF = "\r\n";
    
    

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
	    	 	
	       	// setup a shutdown hook to cleanup and send a SIP bye message even if there is a 
	    	// unexpected crash (ie ctrl-c)
	    	Runtime.getRuntime().addShutdownHook(new Thread() {
	    		public void run() {
	    			_logger.info("Caught the shutting down hook ...");
	    		}
	    	});


	        
	    	File soundFile = new File("c:/work/speechcloud/etc/prompts/lookupsports.wav");	 	
	    	String grammar = "file:///work/speechcloud/etc/grammar/example.gram";
	    	URL grammarUrl = null;
	    	try {
	    		grammarUrl = new URL(grammar);
			} catch (MalformedURLException e) {  
		         e.printStackTrace();  
			}
    		    	
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
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile);
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
	    	
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
	    	
	        
	        // second part is the mic audio
	        InputStreamBody audioBody = new InputStreamBody(audioInputStream, "audio/x-wav","audio.wav");      

			mpEntity.addPart("grammar", grammarBody);
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
