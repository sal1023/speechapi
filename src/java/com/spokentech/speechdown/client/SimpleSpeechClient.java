package com.spokentech.speechdown.client;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.log4j.Logger;

import com.spokentech.speechdown.client.endpoint.ExternalTriggerEndPointer;
import com.spokentech.speechdown.client.endpoint.MicS4EndPointingInputStream;
import com.spokentech.speechdown.client.endpoint.S4EndPointer;
import com.spokentech.speechdown.client.endpoint.StreamEndPointingInputStream;
import com.spokentech.speechdown.client.exceptions.AsynchNotEnabledException;
import com.spokentech.speechdown.client.exceptions.HttpRecognizerException;
import com.spokentech.speechdown.client.exceptions.StreamInUseException;
import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.AudioUtils;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.common.Utterance.OutputFormat;

public class SimpleSpeechClient {
	private static Logger _logger = Logger.getLogger(SimpleSpeechClient.class);

    private static int audioBufferSize = 16000;
    
    private static String upService = "http://www.speechapi.com:8000/speechcloud/SpeechUploadServlet";      
    private static String downService = "http://www.speechapi.com:8000/speechcloud/SpeechDownloadServlet";      
    //private static String upService = "http://spokentech.net/speechcloud/SpeechUploadServlet";      
    //private static String downService = "http://spokentech.net/speechcloud/SpeechDownloadServlet";      

    private static int sampleRate = 8000;
    private static boolean signed = true;
    private static boolean bigEndian = true;
    private static int channels = 1;
    private static int sampleSizeInBits = 16;

    private static boolean s4Mode = true;
    private static String streamMode = "audio";
    private static boolean lmflag = false;
    private static boolean batchFlag = true;
    private static OutputFormat outMode = OutputFormat.json;
    
	private static final String DEFAULT_VOICE = "jmk-arctic";
	private static final String  WAV = "audio/x-wav";
	private static final String  MP3 = "audio/mpeg";


	private static final int RECOGNIZING = 0;

	private static final int READY = 1;

    private SpeechEventListener listener;

	private String devId ="ff-plugin";
	private String userId = null;
	private String key = null;
    
    //public JSObject _window = null;

	HttpSynthesizer synth = null;
    HttpRecognizer recog =null;

	private int state = READY;

	private String currentId;
	ExternalTriggerEndPointer ep;
	StreamEndPointingInputStream epStream ;

	private Utterance results;
	private boolean resultsYet;

	TargetDataLine audioLine = null;
	
    public void setup() {
		 synth = new HttpSynthesizer(devId, key);    
		 synth.setService(downService);	 
	     recog = new HttpRecognizer(devId,key);
	     recog.setService(upService);
	     recog.enableAsynchMode(2);
		 System.out.println("created a recognizer ");
		 

    }
    
    public  String recognize (String grammar) {
	
    	if (state ==  RECOGNIZING) {
    		recog.cancel();
    	}
    	state = RECOGNIZING;
    	
    	_logger.debug("Starting recognition ...");
    	

        //_window = (JSObject) JSObject.getWindow(this);
		/*try {
           JSObject window = JSObject.getWindow(this);
           System.out.println("Calling JavaScript getString();");
           String res = (String) window.eval("smash.getString();");
           System.out.println("Got string from JavaScript: \"" + res + "\"");
		} catch (Exception e) {
			e.printStackTrace();
		}*/

    	String mimeType = "audio/x-s4audio";

    	long timeout = 10000;       	

        AudioFormat desiredFormat = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);

    	S4EndPointer ep = new S4EndPointer();
    	MicS4EndPointingInputStream epStream = new MicS4EndPointingInputStream(ep);
    	epStream.setupStream(desiredFormat, mimeType);
		System.out.println("Set up input stream ");

    	listener = new SpeechListener();
    	String r = null;
        try {
			currentId = recog.recognizeAsynch(devId, key,userId, grammar,  epStream,  lmflag, batchFlag, outMode, timeout, listener ) ;
			System.out.println("Called recognize ");
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();       
        } catch (StreamInUseException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (AsynchNotEnabledException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
    	
        if (r == null) {
        	System.out.println("null result");
        } else {
           System.out.println("result: "+r);	 
        }
        
	    //String result=r.getText();
	    //create the json object with tags for extracting semantic meaning
	    //String tags = toJSONString(r);
        
        //String[] params = new String[2];
        //params[0] = r.getText();
        //_window.call("smash.recognitionEvent", params); 
        

        return currentId;
 
    }


	public  void play (String text, String voice) {

		_logger.debug("Starting Synthesizer ...");

		String mime = MP3;
		//Encoding encoding = new Encoding("MPEG1L3");
		
		if (voice == null)
		   voice=DEFAULT_VOICE;




		//format = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
		//int frameSize = sampleSizeInBits/8;
		//int frameRate = sampleRate/frameSize;
		
		AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                16000,
                16,
                1,
                2,
                16000,
                true);

		
		//TODO: This is a bug, why do I need to flip flop the endian-ness?
		AudioFormat formata = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                16000,
                16,
                1,
                2,
                16000,
                false);
		

		

		InputStream stream = synth.synthesize(userId, text, format, WAV, voice);
     	System.out.println("after calling synthesize ");

		if (stream != null) {

				try {
					//format = new AudioFormat ((float) 16000.0, 16, 1, signed, false);
					AudioInputStream ais = new AudioInputStream(stream,formata,-1);
			     	System.out.println("after making ais ");

					AudioUtils.streamAudioToSpeaker(ais);
			     	System.out.println("after calling streama to speaker ");

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnsupportedAudioFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


		} else {
			_logger.warn("stream was null");
		}

	}
	
	
	
	
    public String recognizeWithTriggers(String grammar) {
	    
    	if (state ==  RECOGNIZING) {
    		recog.cancel(currentId);
    	}
    	state = RECOGNIZING;
    	
    	
	    AudioFormat desiredFormat = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, desiredFormat);
        if (!AudioSystem.isLineSupported(info)) {
            _logger.warn(desiredFormat + " not supported");
            throw new RuntimeException("unsupported audio format");
        } 
        System.out.println("Desired format: " + desiredFormat);

        
        // Get the audio line from the microphone (java sound api) 

        try {
             audioLine = (TargetDataLine) AudioSystem.getLine(info);
            /* Add a line listener that just traces
             * the line states.
             */
            audioLine.addLineListener(new LineListener() {
                public void update(LineEvent event) {
                    _logger.debug("line listener " + event);
                }
            });
        } catch (LineUnavailableException e) {
            _logger.warn("microphone unavailable " + e.getMessage());
        }
 
        // open up the line from the mic (javasound)
        if (audioLine != null) {
        	if (!audioLine.isOpen()) {
        		_logger.debug("open");
        		try {
        			audioLine.open(desiredFormat, audioBufferSize);
        		} catch (LineUnavailableException e) {
        			_logger.warn("Can't open microphone " + e.getMessage());
        			e.printStackTrace();
        		}  
        	} 
        } else {
        	_logger.warn("Can't find microphone");
        	throw new RuntimeException("Can't find microphone");
        }
    	

		 //setup the external trigger endpoint stream and reuse (trigger starts and stops)

	     String mimeType = "audio/x-wav";
	     ep = new ExternalTriggerEndPointer();
		 epStream = new StreamEndPointingInputStream(ep);
	     //epStream = new MicS4EndPointingInputStream(ep);

		 epStream.setMimeType(mimeType);
		 AFormat f = FormatUtils.covertToNeutral(desiredFormat);	
		 AudioInputStream as = new AudioInputStream(audioLine);
		 
		 epStream.setupStream(as, f);
    	
    	
    	long timeout = 10000; 	
    	listener = new SpeechListener();
    	boolean lmflg = false;
    	boolean batchFlag = true;

        try {	            
            currentId = recog.recognizeAsynch(devId,key,userId, grammar,  epStream,  lmflg,  batchFlag,OutputFormat.json, timeout,listener) ;
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (HttpRecognizerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return currentId;
    }
	
    public void triggerStart() {
       	if (state ==  RECOGNIZING) {
       		System.out.println("Calling start trigger");
       		audioLine.start();
           ep.triggerStart();
       	} else {
       		System.out.println("Triggered start but not in recognizing state");
       	}
    }
	
    public void triggerEnd() {
	    if (state ==  RECOGNIZING) {
       		System.out.println("Calling end trigger");
	        ep.triggerEnd();
	        resultsYet=false;
	        audioLine.stop();
	   	} else {
	   		System.out.println("Triggered start but not in recognizing state");
	   	}
    }
    
    public String getResults() {
    	if ((state == RECOGNIZING) && (!resultsYet)) {
    		return null;
    	} else {
	        state = READY;
    		return results.getText();
    	}
	
    }


	
    public class SpeechListener implements SpeechEventListener {
        
        /* (non-Javadoc)
         * @see com.spokentech.speechdown.common.SpeechEventListener#noInputTimeout()
         */
        @Override
        public void noInputTimeout() {
    	    _logger.debug("No input timeout");

        }

        /* (non-Javadoc)
         * @see com.spokentech.speechdown.common.SpeechEventListener#recognitionComplete(com.spokentech.speechdown.common.RecognitionResult)
         */
        @Override
        public void recognitionComplete(Utterance arg0) {
    	    _logger.debug("Recognition Completed");
            results = arg0;
    	    resultsYet=true;
        }

        /* (non-Javadoc)
         * @see com.spokentech.speechdown.common.SpeechEventListener#speechEnded()
         */
        @Override
        public void speechEnded() {
    	    _logger.debug("Speech ended");

        }

        /* (non-Javadoc)
         * @see com.spokentech.speechdown.common.SpeechEventListener#speechStarted()
         */
        @Override
        public void speechStarted() {
    	    _logger.debug("Speech started");

        }


    }
}
