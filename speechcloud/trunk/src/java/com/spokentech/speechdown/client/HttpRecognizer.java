package com.spokentech.speechdown.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;

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
import org.speechforge.cairo.rtp.server.RTPStreamReplicator;

import com.spokentech.speechdown.client.rtp.RtpReceiver;
import com.spokentech.speechdown.common.InvalidRecognitionResultException;
import com.spokentech.speechdown.common.RecognitionResult;


/**
 * Test program that uses the restful api to send audio from the mic.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@users.spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
public class HttpRecognizer {

	private static Logger _logger = Logger.getLogger(HttpRecognizer.class);

    //desired format
    private static AudioFormat desiredFormat;
    private static int sampleRate = 8000;
    private static boolean bigEndian = true;
    private static boolean signed = true;    
    private static int channels = 1;
    private static int sampleSizeInBits = 16;
    
    private static int audioBufferSize = 16000;
    
    MySpeechEventListener listener;
    
 	private static final int	EXTERNAL_BUFFER_SIZE = 3200;
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	DataOutputStream dos = new DataOutputStream(baos);
	byte[]	abData = new byte[EXTERNAL_BUFFER_SIZE];

	private boolean speechStarted = false;
	
	private  void saveAudioToFile(String service, RtpReceiver  rtpReceiver) throws InstantiationException, IOException {


		//Setup a piped stream to get an input stream that can be used for feeding the chunk encoded post 
    	PipedOutputStream	outputStream = new PipedOutputStream();
    	PipedInputStream inputStream = null;
        try {
	        inputStream = new PipedInputStream(outputStream,audioBufferSize);
        } catch (IOException e3) {
	        // TODO Auto-generated catch block
	        e3.printStackTrace();
        }

    	File outWavFile = new File("test.wav");

    	//TODO: Assuming it is a wav file.  Should get the actual type of input file
    	AudioFileFormat.Type outputType = null;
    	AudioFileFormat.Type[] typesSupported = AudioSystem.getAudioFileTypes();
    	for (AudioFileFormat.Type aTypesSupported : typesSupported) {
    		if (aTypesSupported.getExtension().equals("wav")) {
    			outputType =  aTypesSupported;
    		}
    	}
        //AudioFormat audioFormat = audioStream.getFormat();

        //create the listener (listens for end points)
        SpeechEventListener listener = 	new MySpeechEventListener();
        
        
	    long timeout = 10000;
	    rtpReceiver.startAudioTransfer(timeout, outputStream, listener);
    	
     	//now wait for a start speech event
        speechStarted = false;
		while (!speechStarted) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                	_logger.debug("Interrupt Excepton while waiting for tts to complete");
                }
            }
        }
    	_logger.info("Speech started!");
    	
    	int nBytesRead=0;
    	while (nBytesRead != -1){  		

    		//read the data from the file
    		try{
    			nBytesRead = inputStream.read(abData, 0, abData.length);
    			_logger.info("Read "+nBytesRead+ " bytes");
    			if (nBytesRead >0) {
	    			dos.write(abData, 0, nBytesRead);
	    			_logger.info("then wrote them ");
    			}
    		}catch (IOException e){
    			e.printStackTrace();
    		}
    	}

    	javax.media.format.AudioFormat aFormat = rtpReceiver.getFormat();
    	
    	_logger.info("Out of the the Loop "+baos.size());
    	//write it to the new file
    	byte[] outAudioData = baos.toByteArray();
    	ByteArrayInputStream bais = new ByteArrayInputStream(outAudioData);
    	AudioFormat outFormat = new AudioFormat((float) aFormat.getFrameRate(),aFormat.getSampleSizeInBits(), 1, true, true);
    	AudioInputStream ais = new AudioInputStream(bais, outFormat, outAudioData.length / aFormat.getFrameSizeInBits());
    	//_logger.debug(audioFormat.toString());
  
		if (AudioSystem.isFileTypeSupported(outputType, ais)) {
			try {
				AudioSystem.write(ais, outputType, outWavFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			_logger.info("output type not supported..."); 
		}
	}

	
	public  RecognitionResult recognizeNextUtteranceFromMic(String mimeType, URL grammarUrl, String service, MicReceiver mic) throws InstantiationException, IOException {


		//Setup a piped stream to get an input stream that can be used for feeding the chunk encoded post 
    	PipedOutputStream	outputStream = new PipedOutputStream();
    	PipedInputStream inputStream = null;
        try {
	        inputStream = new PipedInputStream(outputStream,audioBufferSize);
        } catch (IOException e3) {
	        // TODO Auto-generated catch block
	        e3.printStackTrace();
        }

        //create the listener (listens for end points)
        MySpeechEventListener listener = new MySpeechEventListener();

	    long timeout = 10000;
	    mic.startAudioTransfer(timeout, outputStream, listener);


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
    	

        
        
        //one part is the audio
        InputStreamBody audioBody = new InputStreamBody(inputStream, mimeType,"audio.wav");      
        
        //parameters are fields (StringBoodys)
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	//StringBody dataStreamMode = new StringBody(dataMode);  	
    	AudioFormat format = mic.getFormat();
    	
        try {
           	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	//dataStreamMode = new StringBody(dataMode);
        	
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
		//mpEntity.addPart("dataMode", dataStreamMode);
		mpEntity.addPart("sampleRate", sampleRate);
		mpEntity.addPart("bigEndian", bigEndian);
		mpEntity.addPart("bytesPerValue", bytesPerValue);
		mpEntity.addPart("encoding", encoding);
		
		//add the grammar part
		mpEntity.addPart("grammar", grammarBody);
		
		//add the audio part
		mpEntity.addPart("audio", audioBody);
				
		//set the multipart entity for the post command
	    httppost.setEntity(mpEntity);

     	//now wait for a start speech event
        speechStarted = false;
		while (!speechStarted) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                	_logger.debug("Interrupt Excepton while waiting for tts to complete");
                }
            }
        }
    	_logger.info("Speech started!");

	    
	    //execute the post command
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
        
        //get the response from the post
        HttpEntity resEntity = response.getEntity();

        _logger.info("----------------------------------------");
        _logger.info(response.getStatusLine());
        if (resEntity != null) {
        	_logger.info("Response content length: " + resEntity.getContentLength());
        	_logger.info("Chunked?: " + resEntity.isChunked());
        }
        RecognitionResult r = null;
        if (resEntity != null) {
            try {
                InputStream s = resEntity.getContent();
                String result = readInputStreamAsString(s);
                _logger.info(result);
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
	
	
	public  RecognitionResult recognizeNextUtterance(String mimeType, URL grammarUrl, String service, TargetDataLine audioLine) {

		//Setup a piped stream to get an input stream that can be used for feeding the chunk encoded post 
    	PipedOutputStream	outputStream = new PipedOutputStream();
    	PipedInputStream inputStream = null;
        try {
	        inputStream = new PipedInputStream(outputStream,audioBufferSize);
        } catch (IOException e3) {
	        // TODO Auto-generated catch block
	        e3.printStackTrace();
        }


        //create the listener (listens for end points)
        MySpeechEventListener listener = 	new MySpeechEventListener();
        
        //create the thread and start it
        Thread myThread = new EndPointerThread("Mic",audioLine,outputStream, listener );
     	myThread.start();

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

    	

    	
        //one part is the audio
        InputStreamBody audioBody = new InputStreamBody(inputStream, mimeType,"audio.wav");      
        
        // get the audio format and send as form fields.  Cound not send aduio file with format included because audio files do not
        // support mark/reset.  That is needed for stremaing using http chunk encoding on the servlet side using file upload.
		AudioInputStream audioStream = new AudioInputStream(audioLine);
        AudioFormat format = audioStream.getFormat();
        _logger.info("Actual format: " + format);    	
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	//StringBody dataStreamMode = null;
        try {
        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	//dataStreamMode = new StringBody(dataMode);
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
		//mpEntity.addPart("dataMode", dataStreamMode);
		mpEntity.addPart("sampleRate", sampleRate);
		mpEntity.addPart("bigEndian", bigEndian);
		mpEntity.addPart("bytesPerValue", bytesPerValue);
		mpEntity.addPart("encoding", encoding);
		
		//add the grammar part
		mpEntity.addPart("grammar", grammarBody);
		
		//add the audio part
		mpEntity.addPart("audio", audioBody);
		
		
		//set the multipart entity for the post command
	    httppost.setEntity(mpEntity);

	    
    	
     	//now wait for a start speech event
        speechStarted = false;
		while (!speechStarted) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                	_logger.debug("Interrupt Excepton while waiting for tts to complete");
                }
            }
        }
    	_logger.info("Speech started!");
	    

	    //execute the post command
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
        
        //get the response from the post
        HttpEntity resEntity = response.getEntity();

        _logger.info("----------------------------------------");
        _logger.info(response.getStatusLine());
        if (resEntity != null) {
        	_logger.info("Response content length: " + resEntity.getContentLength());
        	_logger.info("Chunked?: " + resEntity.isChunked());
        }
        RecognitionResult r = null;
        if (resEntity != null) {
            try {
                InputStream s = resEntity.getContent();
                String result = readInputStreamAsString(s);
                _logger.info(result);
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
	
	public RecognitionResult recognizeNextUtterance(String mimeType, URL grammarUrl, String service, RtpReceiver  rtpReceiver) throws InstantiationException, IOException {

		//Setup a piped stream to get an input stream that can be used for feeding the chunk encoded post 
    	PipedOutputStream	outputStream = new PipedOutputStream();
    	PipedInputStream inputStream = null;
        try {
	        inputStream = new PipedInputStream(outputStream,audioBufferSize);
        } catch (IOException e3) {
	        // TODO Auto-generated catch block
	        e3.printStackTrace();
        }
		
        //create the listener (listens for end points)
        MySpeechEventListener listener = new MySpeechEventListener();

	    long timeout = 10000;
	    rtpReceiver.startAudioTransfer(timeout, outputStream, listener);
        	    
	    //--------------------------------------------------------------------------------------------
	    //TODO: This is an alternative to the startAudio Transfer which uses the S4 frontend
	    //
	    //  Should this be an option inside of rtpreceiver (so its does
	    //
        //create the thread and start it
        //Thread myThread = new Thresholder("rtpChannel",rtpReceiver.getInputStream(),outputStream, listener);
     	//myThread.start();
        //----------------------------------------------------------------------------------------------

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
    	
        //one part is the audio
        InputStreamBody audioBody = new InputStreamBody(inputStream, mimeType,"audio.wav");      
        
        // get the audio format and send as form fields.  Cound not send aduio file with format included because audio files do not
        // support mark/reset.  That is needed for stremaing using http chunk encoding on the servlet side using file upload.

        javax.media.format.AudioFormat format = rtpReceiver.getFormat();
        _logger.info("Actual format: " + format);    	
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
    	//StringBody dataStreamMode = new StringBody(dataMode);
        try {
        	boolean bigEndianFlag = false;
        	if (format.getEndian() ==  javax.media.format.AudioFormat.BIG_ENDIAN) 
        		bigEndianFlag = true;
        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(bigEndianFlag));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
		//mpEntity.addPart("dataMode", dataStreamMode);
		mpEntity.addPart("sampleRate", sampleRate);
		mpEntity.addPart("bigEndian", bigEndian);
		mpEntity.addPart("bytesPerValue", bytesPerValue);
		mpEntity.addPart("encoding", encoding);
		
		//add the grammar part
		mpEntity.addPart("grammar", grammarBody);
		
		//add the audio part
		mpEntity.addPart("audio", audioBody);
		
		
		//set the multipart entity for the post command
	    httppost.setEntity(mpEntity);

     	//now wait for a start speech event
        speechStarted = false;
		while (speechStarted) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                	_logger.debug("Interrupt Excepton while waiting for tts to complete");
                }
            }
        }
    	_logger.info("Speech started!");
    	
	    //execute the post command
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
        
        //get the response from the post
        HttpEntity resEntity = response.getEntity();

        _logger.info("----------------------------------------");
        _logger.info(response.getStatusLine());
        if (resEntity != null) {
        	_logger.info("Response content length: " + resEntity.getContentLength());
        	_logger.info("Chunked?: " + resEntity.isChunked());
        }

        RecognitionResult r = null;
        if (resEntity != null) {
            try {
                InputStream s = resEntity.getContent();
                String result = readInputStreamAsString(s);
                _logger.info(result);
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
	
    public class MySpeechEventListener implements SpeechEventListener {
    
		public void speechEnded() {
		    _logger.info("Speech Ended");
	    }

	    public void speechStarted() {
		    _logger.info("Speech Started");
			//signal for the blocking call to check for unblocking
			synchronized (this) {
				speechStarted=true;
				this.notifyAll();
			}
	    }

		@Override
        public void noInputTimeout() {
		    _logger.info("No input timeout");   
        }
    }
       
    public static void testrtp(String dataMode, String grammar, String service, int port) throws IOException, InstantiationException {
        
        
        String configFile="c:/work/speechcloud/etc/sphinxrtpfrontendonly-audio.xml";
    	if (dataMode.equals("audio")) {
            configFile="c:/work/speechcloud/etc/sphinxrtpfrontendonly-audio.xml";
    	} else if (dataMode.equals("feature")) {
            configFile="c:/work/speechcloud/etc/sphinxrtpfrontendonly-feature.xml";
    	} else {
    		_logger.warn("Unrecognized data mode: "+dataMode+"  Guessing audio.");
    		dataMode = "audio";
    	}
    	
    	
    	URL grammarUrl = null;
    	try {
    		grammarUrl = new URL(grammar);
		} catch (MalformedURLException e) {  
	         e.printStackTrace();  
		}
		
        RtpReceiver rtpReceiver = new RtpReceiver();
        rtpReceiver.setS4ConfigFile(configFile);
        rtpReceiver.init();
        
        RTPStreamReplicator replicator = new RTPStreamReplicator(port);
        rtpReceiver.setupStream(replicator);
  
        //start up the microphone
    	HttpRecognizer mr = new HttpRecognizer();
        mr.recognizeNextUtterance(dataMode, grammarUrl, service, rtpReceiver);
    }
    
    
    public static void testmic2(String mimeType, String grammar, String service) throws IOException, InstantiationException {
        _logger.info("Using mime type : "+mimeType);
        String configFile="c:/work/speechcloud/etc/sphinxmicfrontendonly-audio.xml";
    	if (mimeType.equals("audio/x-wav")) {
            configFile="c:/work/speechcloud/etc/sphinxmicfrontendonly-audio.xml";
    	} else if (mimeType.equals("audio/x-s4feature")) {
            configFile="c:/work/speechcloud/etc/sphinxmicfrontendonly-feature.xml";
    	} else if (mimeType.equals("audio/x-s4audio")) {
            configFile="c:/work/speechcloud/etc/sphinxmicfrontendonly-audio.xml";
    	} else {
    		_logger.warn("Unrecognized data mode: "+mimeType+"  Guessing audio/x-wav.");
    		mimeType = "audio/x-wav";
    	}

    	URL grammarUrl = null;
    	try {
    		grammarUrl = new URL(grammar);
		} catch (MalformedURLException e) {  
	         e.printStackTrace();  
		}
		

        MicReceiver micReceiver = new MicReceiver();
        micReceiver.setS4ConfigFile(configFile);
        micReceiver.init();
        
 
        //start up the microphone
    	HttpRecognizer recog = new HttpRecognizer();
        recog.recognizeNextUtteranceFromMic(mimeType, grammarUrl, service, micReceiver);
    }
    
    public static void testmic(String mimeType, String grammar, String service) {
 	
        _logger.info("Using mime type : "+mimeType);
 
        
        if (mimeType.equals("audio/x-wav")) {
            //ok!

    	} else {
    		_logger.warn("Mime type not supported: "+mimeType+"  Trying to process like audio/x-wav.");
    		mimeType = "audio/x-wav";
    	}

    
    	
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
        		//AudioInputStream audioStream = new AudioInputStream(audioLine);

        		// Set the frame size depending on the sample rate. 
        		//float sec = ((float) msecPerRead) / 1000.f;
        		//frameSizeInBytes =
        		//	(audioStream.getFormat().getSampleSizeInBits() / 8) *
        		//	(int) (sec * audioStream.getFormat().getSampleRate());

        		//_logger.info("Frame size: " + frameSizeInBytes + " bytes");
        	} 
        } else {
        	_logger.info("Can't find microphone");
        	throw new RuntimeException("Can't find microphone");
        }
        
        
        
        //start up the microphone
    	HttpRecognizer hr = new HttpRecognizer();
        hr.recognizeNextUtterance(mimeType, grammarUrl, service, audioLine);
        
        //results are...
        
        
        //results are...
        
    }


	public  String readInputStreamAsString(InputStream in) throws IOException {

		BufferedInputStream bis = new BufferedInputStream(in);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while(result != -1) {
			byte b = (byte)result;
			buf.write(b);
			result = bis.read();
		}        
		_logger.debug(buf.toString());
		return buf.toString();
	}
    

    
}