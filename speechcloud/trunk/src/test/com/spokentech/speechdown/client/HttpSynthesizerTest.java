package com.spokentech.speechdown.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;

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

import com.spokentech.speechdown.client.endpoint.EndPointingInputStream;
import com.spokentech.speechdown.client.endpoint.FileS4EndPointingInputStream;
import com.spokentech.speechdown.client.endpoint.FileS4EndPointingInputStream2;
import com.spokentech.speechdown.client.endpoint.StreamEndPointingInputStream;
import com.spokentech.speechdown.client.endpoint.StreamS4EndPointingInputStream;
import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.common.SpeechEventListener;


import junit.framework.TestCase;

public class HttpSynthesizerTest extends TestCase {

    public class Listener implements SpeechEventListener {

	    @Override
	    public void noInputTimeout() {
		    // TODO Auto-generated method stub

	    }

	    @Override
	    public void speechEnded() {
		    // TODO Auto-generated method stub

	    }

	    @Override
	    public void speechStarted() {
		    // TODO Auto-generated method stub

	    }

    }


	private static Logger _logger = Logger.getLogger(HttpSynthesizerTest.class);
    public static final String CRLF = "\r\n";
    
   
    //private static String service = "http://ec2-75-101-188-39.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";  
    private static String service = "http://localhost:8090/speechcloud/SpeechDownloadServlet";    

    private static int sampleRate = 8000;
    private static boolean signed = true;
    private static boolean bigEndian = true;
    private static int channels = 1;
    private static int sampleSizeInBits = 16;
    
    private static int audioBufferSize = 160000;
    private static int msecPerRead = 10;
    private static int frameSizeInBytes;
    
	HttpSynthesizer synth;
	
	
    String wav = "audio/x-wav";
    String mpeg = "audio/mpeg";
    
    private  AudioFormat format;

	
	     protected void setUp() {
		    	synth = new HttpSynthesizer();
		    	synth.setService(service);	 
		    	
		        format = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);

	     }



	    public void testSynth() {
	    	
	    	String text;
	    	String voice;
	    	String outFileName;
	    	InputStream stream;
	    	
	    	 
	    	System.out.println("Starting Test ...");
	    	
	    	
	    	text = "this is a only a test";
	    	voice = "jmk-arctic";
	    	outFileName = "this-is.wav";
	    	stream = synth.synthesize(text, format, wav, voice);
	        if (stream != null) {
	        	writeStreamToFile(stream,outFileName);
	        }
	    	
	    	text = "To be or not to be, that is the question. Whether tis nobler in the mind to suffer the slings and arrows of outrageous fortune, or to take arms against a sea of troubles, And by opposing end them. To die—to sleep";
			voice = "hmm-jmk";
	    	outFileName = "to-be.mp3";
	    	stream = synth.synthesize(text, format, mpeg, voice);
	        if (stream != null) {
	        	writeStreamToFile(stream,outFileName);
	        }
	    	text = "A man, a plan, a canal, panama";
			voice = "slt-arctic";
	    	outFileName = "a-man.wav";
	    	 stream = synth.synthesize(text, format, wav, voice);
	        if (stream != null) {
	        	writeStreamToFile(stream,outFileName);
	        }
			
	    	text = "If a server wants to start sending a response before knowing its total length (like with long script output), it might use the simple chunked transfer-encoding, which breaks the complete response into smaller chunks and sends them in series. ";
			voice = "hmm-slt";
	    	outFileName = "If-a-server.mpeg";
	    	 stream = synth.synthesize(text, format, mpeg, voice);
	        if (stream != null) {
	        	writeStreamToFile(stream,outFileName);
	        }




			 //voice = "hmm-bdl";
			 //voice = "bdl-arctic";
			 //voice = "misspelled";



	        
	    }
        

	public void writeStreamToFile(InputStream inStream, String fileName) {
		try {

			File f = new File(fileName);
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
	
			BufferedInputStream in = new BufferedInputStream(inStream);
	
			byte[] buffer = new byte[256]; 
			while (true) { 
				int bytesRead = in.read(buffer);
				//_logger.trace("Read "+ bytesRead + "bytes.");
				if (bytesRead == -1) break; 
				out.write(buffer, 0, bytesRead); 
			} 
			_logger.info("Closing streams");
			in.close(); 
			out.close(); 
		} 
		catch (Exception e) { 
			_logger.warn("upload Exception"); e.printStackTrace(); 
			e.printStackTrace();
		} 
	}
	    

	    
}



