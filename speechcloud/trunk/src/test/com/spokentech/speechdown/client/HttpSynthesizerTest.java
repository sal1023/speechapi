/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.Utterance;

import junit.framework.TestCase;
import org.tritonus.share.sampled.Encodings;

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

	    @Override
        public void recognitionComplete(Utterance rr) {
	        // TODO Auto-generated method stub
	        
        }

    }


	private static Logger _logger = Logger.getLogger(HttpSynthesizerTest.class);
    public static final String CRLF = "\r\n";
    
   
    private static String service = "http://www.speechapi.com:8000/speechcloud/SpeechDownloadServlet";  
    //private static String service = "http://ec2-174-129-20-250.compute-1.amazonaws.com/speechcloud/SpeechDownloadServlet";  
    //private static String service = "http://localhost:8090/speechcloud/SpeechDownloadServlet";    

    private static int sampleRate = 16000;
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
    private  AudioFormat format2;
    private  AudioFormat format3;
	private String devId ="HttpSynthesizerTest";
	private String key = null;
	private String userId =null;
	
	     protected void setUp() {
		    	synth = new HttpSynthesizer(devId, key );
		    	synth.setService(service);	 
		    	
		        //format = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
		
		    	format = new AudioFormat(
		                AudioFormat.Encoding.PCM_SIGNED,
		                16000,
		                16,
		                1,
		                2,
		                16000,
		                true);
		    	
		    	 System.out.println("***** "+Encodings.getEncoding("MPEG1L3"));
		    	
		    	format2 = new AudioFormat(
		                //Encodings.getEncoding("MPEG1L3"),
		    			new Encoding("MPEG1L3"),
		                11025,
		                32,
		                1,
		                -1,
		                -1,
		                true);
		    	
		        format3 = new AudioFormat ( 44100, 32, channels, signed, bigEndian);             
		        
		    	
		    	System.out.println(format.toString());
		    	System.out.println(format2.toString());
	     }



	    public void testSynth() {
	    	
	    	String text;
	    	String voice;
	    	String outFileName;
	    	InputStream stream;
	    	
	    	 
	    	System.out.println("Starting Test ...");
	    	
	    	
	    	text = "To be or not to be, that is the question. Whether tis nobler in the mind to suffer the slings and arrows of outrageous fortune, or to take arms against a sea of troubles, And by opposing end them. To die—to sleep";
			voice = "hmm-jmk";
	    	outFileName = "to-be.mp3";
	    	stream = synth.synthesize(userId, text, format2, mpeg, voice);
	        if (stream != null) {
	        	writeStreamToFile(stream,outFileName);
	        }
	    	
	    	text = "this is a only a test";
	    	voice = "jmk-arctic";
	    	outFileName = "this-is.wav";
	    	stream = synth.synthesize(userId , text, format, wav, voice);
	        if (stream != null) {
	        	writeStreamToFile(stream,outFileName);
	        	/*try {
	        	    format = new AudioFormat ((float) 16000.0, sampleSizeInBits, channels, signed, false);
	  		         	
	        		AudioInputStream ais = new AudioInputStream(stream,format,-1);
	                streamAudioToSpeaker(ais);
                } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                } catch (UnsupportedAudioFileException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                } catch (LineUnavailableException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                }*/
	        }
	
	    	text = "A man, a plan, a canal, panama";
			voice = "slt-arctic";
	    	outFileName = "a-man.wav";
	    	 stream = synth.synthesize(userId, text, format, wav, voice);
	        if (stream != null) {
	        	writeStreamToFile(stream,outFileName);
	        	/*try {
	        	    format = new AudioFormat ((float) 16000.0, sampleSizeInBits, channels, signed, false);
	  		         	
	        		AudioInputStream ais = new AudioInputStream(stream,format,-1);
	                streamAudioToSpeaker(ais);
                } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                } catch (UnsupportedAudioFileException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                } catch (LineUnavailableException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                }*/
	        }
			
	    	text = "If a server wants to start sending a response before knowing its total length (like with long script output), it might use the simple chunked transfer-encoding, which breaks the complete response into smaller chunks and sends them in series. ";
			voice = "hmm-slt";
	    	outFileName = "If-a-server.mp3";
	    	 stream = synth.synthesize(userId, text, format2, mpeg, voice);
	        if (stream != null) {
	        	writeStreamToFile(stream,outFileName);
	
	        }
			//voice = "hmm-jmk";
	    	//voice = "jmk-arctic";
			//voice = "hmm-slt";
			//voice = "slt-arctic";
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
	    
	   /** Read sampled audio data from the specified URL and play it */
    public  void streamAudioToSpeaker(AudioInputStream ain)
        throws IOException, UnsupportedAudioFileException,
               LineUnavailableException
    {
        SourceDataLine line = null;   // And write it here.

        try {
     
            // Get information about the format of the stream
            AudioFormat format = ain.getFormat( );
            DataLine.Info info=new DataLine.Info(SourceDataLine.class,format);

            // If the format is not supported directly (i.e. if it is not PCM
            // encoded), then try to transcode it to PCM.
            if (!AudioSystem.isLineSupported(info)) {
                // This is the PCM format we want to transcode to.
                // The parameters here are audio format details that you
                // shouldn't need to understand for casual use.
                AudioFormat pcm =
                    new AudioFormat(format.getSampleRate( ), 16,
                                    format.getChannels( ), true, false);

                // Get a wrapper stream around the input stream that does the
                // transcoding for us.
                ain = AudioSystem.getAudioInputStream(pcm, ain);

                // Update the format and info variables for the transcoded data
                format = ain.getFormat( ); 
                info = new DataLine.Info(SourceDataLine.class, format);
            }

            // Open the line through which we'll play the streaming audio.
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);  

            // Allocate a buffer for reading from the input stream and writing
            // to the line.  Make it large enough to hold 4k audio frames.
            // Note that the SourceDataLine also has its own internal buffer.
            int framesize = format.getFrameSize( );
            byte[  ] buffer = new byte[4 * 1024 * framesize]; // the buffer
            int numbytes = 0;                               // how many bytes

            // We haven't started the line yet.
            boolean started = false;

            for(;;) {  // We'll exit the loop when we reach the end of stream
                // First, read some bytes from the input stream.
                int bytesread=ain.read(buffer,numbytes,buffer.length-numbytes);
                // If there were no more bytes to read, we're done.
                if (bytesread == -1) break;
                numbytes += bytesread;
                
                // Now that we've got some audio data to write to the line,
                // start the line, so it will play that data as we write it.
                if (!started) {
                    line.start( );
                    started = true;
                }
                
                // We must write bytes to the line in an integer multiple of
                // the framesize.  So figure out how many bytes we'll write.
                int bytestowrite = (numbytes/framesize)*framesize;
                
                // Now write the bytes. The line will buffer them and play
                // them. This call will block until all bytes are written.
                line.write(buffer, 0, bytestowrite);
                
                // If we didn't have an integer multiple of the frame size, 
                // then copy the remaining bytes to the start of the buffer.
                int remaining = numbytes - bytestowrite;
                if (remaining > 0)
                    System.arraycopy(buffer,bytestowrite,buffer,0,remaining);
                numbytes = remaining;
            }

            // Now block until all buffered sound finishes playing.
            line.drain( );
        }
        finally { // Always relinquish the resources we use
            if (line != null) line.close( );
            if (ain != null) ain.close( );
        }
    }
	    
}



