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

public class HttpRecognizerTest extends TestCase {

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


	private static Logger _logger = Logger.getLogger(HttpRecognizerTest.class);
    public static final String CRLF = "\r\n";
    
   
    //private static String service = "http://ec2-75-101-188-39.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";  
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
    
	private String grammar = "file:///work/speechcloud/etc/grammar/example.gram";    
	URL grammarUrl = null;
	HttpRecognizer recog;
	
	File soundFile1 = new File("c:/work/speechcloud/etc/prompts/lookupsports.wav");	 	
	File soundFile2 = new File("c:/work/speechcloud/etc/prompts/get_me_a_stock_quote.wav");	 	
	File soundFile3 = new File("c:/work/speechcloud/etc/prompts/i_would_like_sports_news.wav");	 	

	
    String wav = "audio/x-wav";
    String s4feature = "audio/x-s4feature";
    String s4audio = "audio/x-s4audio";
    
    
    String audioConfigFile="c:/work/speechcloud/etc/sphinxfrontendonly-audio.xml";
    String featureConfigFile="c:/work/speechcloud/etc/sphinxfrontendonly-feature.xml";

    public String configForMime(String mimeType) {
    	String configFile = null;
    	if (mimeType.equals(wav)) {
            configFile=audioConfigFile;
    	} else if (mimeType.equals(s4feature)) {
            configFile=featureConfigFile;
    	} else if (mimeType.equals(s4audio)) {
            configFile=audioConfigFile;
    	} else {
    		_logger.warn("Unrecognized data mode: "+mimeType+"  Guessing audio/x-wav.");
    		mimeType = "audio/x-wav";
    	}
    	return configFile;
    }
    
    
    
	
	
	     protected void setUp() {
		    	recog = new HttpRecognizer();
		    	recog.setService(service);

		    	try {
		    		grammarUrl = new URL(grammar);
				} catch (MalformedURLException e) {  
			         e.printStackTrace();  
				}		    
	     }

	 

	    public void testRecognizeFile() {
	    	System.out.println("Starting File Test ...");
	        String fname = "c:/work/speechcloud/etc/prompts/lookupsports.wav"; 	
	    	boolean lmflg = true;
	    	boolean doEndpointing = true;
	    	RecognitionResult r = recog.recognize(fname, grammarUrl, lmflg, doEndpointing);
	    	System.out.println("lm result: "+r.getText());
	    	
	    	lmflg = false;
	    	r = recog.recognize(fname, grammarUrl, lmflg, doEndpointing);
	    	System.out.println("grammar result: "+r.getText());
	    }
	     
	    public void testRecognizeFileS4EP() {
	    	System.out.println("Starting File EP Test ...");	

	    	
	    	long timeout = 10000;       	
	    	FileS4EndPointingInputStream2 epStream = new FileS4EndPointingInputStream2();

	    	epStream.setMimeType(s4audio);
	    	epStream.setS4ConfigFile(configForMime(s4audio));
	    	epStream.setupStream(soundFile1);
	    	epStream.init();

	    	RecognitionResult r = null;
	    	
	    	boolean lmflg = true;
	        try {	            
	            r = recog.recognize(grammarUrl,  epStream,  lmflg,  timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
	    	
            System.out.println("lm result: "+r.getText());
	   
            
            timeout = 10000;       	
	    	epStream = new FileS4EndPointingInputStream2();

	    	epStream.setMimeType(s4audio);
	    	epStream.setS4ConfigFile(configForMime(s4audio));
	    	epStream.setupStream(soundFile1);
	    	epStream.init();

            
	    	lmflg = false;
	        try {	            
	            r = recog.recognize(grammarUrl,  epStream,  lmflg,  timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            
            }
            System.out.println("grammar result: "+r.getText());
	    	
	    }

	    
	    public void testRecognizeAudioStream() {
	    	System.out.println("Starting Stream Test ...");
	    	
	    	
	    	// Get a stream for the test
	    	AudioInputStream	audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile1);
	    		type = AudioSystem.getAudioFileFormat(soundFile1).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}

	    	//run the test
	    	boolean lmflg = true;
	    	boolean doEndpointing = true;
	    	RecognitionResult r = recog.recognize(audioInputStream, type, null, lmflg, doEndpointing);
	    	System.out.println("lm result: "+r.getText());
	    	
	    	// Get a stream for the test
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile1).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
	    	//run the test
	    	lmflg = false;
	    	r = recog.recognize(audioInputStream, type, grammarUrl, lmflg, doEndpointing);
	    	System.out.println("grammar result: "+r.getText());	    	
	    	
	    	
	    }
	     
	    public void testRecognizeStreamWithFormatParamater() {
	    	System.out.println("Starting Input Stream with format parameter Test ...");
	    	

			
	    	
	    	// Get a stream for the test
	    	AudioInputStream	audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile1);
	    		type = AudioSystem.getAudioFileFormat(soundFile1).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	        AudioFormat format = audioInputStream.getFormat();
	        
	        
	    	//run the test
	    	boolean lmflg = true;
	    	boolean doEndpointing = true;
	    	RecognitionResult r = recog.recognize(audioInputStream, format, type, null, lmflg,doEndpointing);
	    	System.out.println("lm result: "+r.getText());
	    	
	    	// Get a stream for the test
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile2);
	    		type = AudioSystem.getAudioFileFormat(soundFile1).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	format = audioInputStream.getFormat();
	    	
	    	//run the test
	    	lmflg = false;
	    	r = recog.recognize(audioInputStream, format, type, grammarUrl, lmflg,doEndpointing);
	    	System.out.println("grammar result: "+r.getText());	    	
	    	
	    	
	    }
	     
	    public void xxxtestRecognizeStreamEP() {	    	
	    	System.out.println("Starting EP Stream Test ...");

	    	// read in the sound file.
	    	AudioInputStream	audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile1);
	    		type = AudioSystem.getAudioFileFormat(soundFile1).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}

	    	StreamInputEP(audioInputStream);

	    }
	    
	    
	    public void testRecognizeStreamS4EP() {
	
	    	System.out.println("Starting S4 EP Stream Test ...");

	    	// get an audio stream for the test from a file
	    	AudioInputStream	audioInputStream = null;
	    	Type type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile1);
	    		type = AudioSystem.getAudioFileFormat(soundFile1).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
	    	//run the test
	    	long timeout = 10000;
	    	
	    	StreamS4EndPointingInputStream epStream = new StreamS4EndPointingInputStream();
	    	epStream.setMimeType(s4audio);
	    	epStream.setS4ConfigFile(configForMime(s4audio));
	    	epStream.setupStream(audioInputStream);
	    	epStream.init();

	    	RecognitionResult r = null;
	    	boolean lmflg = true;
	        try {
	        	r = recog.recognize(grammarUrl,  epStream,  lmflg,  timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
	    	
           System.out.println("lm result: "+r.getText());
	
           
       	    // get an audio stream for the test from a file
            audioInputStream = null;
	    	type = null;
	    	try {
	    		audioInputStream = AudioSystem.getAudioInputStream(soundFile1);
	    		type = AudioSystem.getAudioFileFormat(soundFile1).getType();
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
	    	//run the test
	    	epStream = new StreamS4EndPointingInputStream();
	    	epStream.setMimeType(s4audio);
	    	epStream.setS4ConfigFile(configForMime(s4audio));
	    	epStream.setupStream(audioInputStream);
	    	epStream.init();
           
	    	lmflg = false;
	        try {
	        	r = recog.recognize(grammarUrl,  epStream,  lmflg,  timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
	    	
            System.out.println("grammar result: "+r.getText());
        	
	    	

	    }
	    
	    
	    //-------------------------------------------------------------------
	    


	    
	    private void MicInput() {
	    	
	    	
	        desiredFormat = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
	        DataLine.Info info = new DataLine.Info(TargetDataLine.class, desiredFormat);
	        if (!AudioSystem.isLineSupported(info)) {
	            _logger.info(desiredFormat + " not supported");
	            throw new RuntimeException("unsupported audio format");
	        } 
	        System.out.println("Desired format: " + desiredFormat);

	        
	        // Get the audio line from the microphone (java sound api) 
	    	TargetDataLine audioLine = null;
	        try {
	             audioLine = (TargetDataLine) AudioSystem.getLine(info);
	            /* Add a line listener that just traces
	             * the line states.
	             */
	            audioLine.addLineListener(new LineListener() {
	                public void update(LineEvent event) {
	                    _logger.info("line listener " + event);
	                }
	            });
	        } catch (LineUnavailableException e) {
	            _logger.info("microphone unavailable " + e.getMessage());
	        }
	 
	        // open up the line from the mic (javasound)
	        if (audioLine != null) {
	        	if (!audioLine.isOpen()) {
	        		_logger.info("open");
	        		try {
	        			audioLine.open(desiredFormat, audioBufferSize);
	        		} catch (LineUnavailableException e) {
	        			_logger.info("Can't open microphone " + e.getMessage());
	        			e.printStackTrace();
	        		}  
	        	} 
	        } else {
	        	_logger.info("Can't find microphone");
	        	throw new RuntimeException("Can't find microphone");
	        }
	    	boolean doEndpointing = true;
	    	boolean lmflg = true;
	    	RecognitionResult r = recog.recognize(audioLine, grammarUrl, lmflg, doEndpointing);
	    	System.out.println("lm result: "+r.getText());	        
	    	
	        lmflg = false;
	        r = recog.recognize(audioLine, grammarUrl, lmflg,doEndpointing);
	        System.out.println("grammar result: "+r.getText());
	    	
	    }
	    
	    
	    //private void testRtpInput(InputStream stream) {
	    //	
	    //}


	    private void StreamInputEP(AudioInputStream stream) {
	    
	    	
	    	long timeout = 10000;
	    	
	    	StreamEndPointingInputStream epStream = new StreamEndPointingInputStream();
	    	epStream.setMimeType(wav);
	    	epStream.setupStream(stream);
	    	

	    	RecognitionResult r = null;
	    	boolean lmflg = true;
	        try {          
	            r = recog.recognize(grammarUrl,  epStream,  lmflg,  timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }    	
            System.out.println("lm result: "+r.getText());

	    	lmflg = false;
	        try {          
	            r = recog.recognize(grammarUrl,  epStream,  lmflg,  timeout) ;
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }    	
            System.out.println("grammar result: "+r.getText());
	    	

	    	
	    }


	    //private void testRtpInputS4EP(InputStream stream) {
	    //	
	    //}
	    
	    
}



