package com.spokentech.speechdown.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
import javax.sound.sampled.AudioFormat.Encoding;

import javax.xml.ws.BindingProvider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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

import com.spokentech.speechdown.common.RecognitionResult;


/**
 * Test program that uses the restful api to send audio from the mic.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@users.spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
public class MicRecognizer2 {
    private static Logger _logger = Logger.getLogger(MicRecognizer2.class);
    public static final String CRLF = "\r\n";
    


    public static final String HELP_OPTION = "help";
    public static final String SERVICE_OPTION = "service";
    public static final String LIMIT_OPTION = "limit";
    
    private static final String defaultService = "http://localhost:8090/speechcloud/SpeechUploadServlet";
    
    private static int limit = 100000;
    
    
    private static AudioFormat desiredFormat;
    private static int sampleRate = 8000;
    private static boolean signed = true;
    private static boolean bigEndian = true;
    private static int channels = 1;
    private static int sampleSizeInBits = 16;
    
    private static int audioBufferSize = 160000;
    private static int msecPerRead = 10;
    private static int frameSizeInBytes;
    
    private  void start(TargetDataLine audioLine,OutputStream outputStream) {
   	   Thread myThread = new RecordingThread("Mic",audioLine,outputStream);
	   myThread.start();
    }
    
    private static Options getOptions() {

        Options options = new Options();
        Option option = new Option(HELP_OPTION, "print this message");
        options.addOption(option);

        option = new Option(SERVICE_OPTION, true, "location of resource server (defaults to localhost)");
        option.setArgName("service");
        options.addOption(option);
        
        option = new Option(LIMIT_OPTION, true, "location of resource server (defaults to localhost)");
        option.setArgName("service");
        options.addOption(option);

        return options;
   }
    
    
    public static void main (String[] args) {

        AudioInputStream audioStream = null;
    	
    	_logger.info("Starting Recogniizer ...");
    	 	
       	// setup a shutdown hook to cleanup and send a SIP bye message even if there is a 
    	// unexpected crash (ie ctrl-c)
    	Runtime.getRuntime().addShutdownHook(new Thread() {
    		public void run() {
    			_logger.info("Caught the shutting down hook ...");
    		}
    	});


    	//get the command line args
    	CommandLine line = null;
		Options options = getOptions();
    	try {
    		CommandLineParser parser = new GnuParser();
    		line = parser.parse(options, args, true);
    		args = line.getArgs();
    	} catch (ParseException e) {
    		e.printStackTrace();
    	}

    	if (args.length != 1 || line.hasOption(HELP_OPTION)) {
    		HelpFormatter formatter = new HelpFormatter();
    		formatter.printHelp("MicRecognizer [options] <grammar-file>", options);
    		return;
    	}
 	
    	String grammar = args[0];
    	URL grammarUrl = null;
    	try {
    		grammarUrl = new URL(grammar);
		} catch (MalformedURLException e) {  
	         e.printStackTrace();  
		}

    	// lookup resource server
    	String service = line.hasOption(SERVICE_OPTION) ? line.getOptionValue(SERVICE_OPTION) : defaultService; 
    	
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
        		audioStream = new AudioInputStream(audioLine);

        		/* Set the frame size depending on the sample rate. */
        		float sec = ((float) msecPerRead) / 1000.f;
        		frameSizeInBytes =
        			(audioStream.getFormat().getSampleSizeInBits() / 8) *
        			(int) (sec * audioStream.getFormat().getSampleRate());

        		_logger.info("Frame size: " + frameSizeInBytes + " bytes");
        	} 
        } else {
        	_logger.info("Can't find microphone");
        	throw new RuntimeException("Can't find microphone");
        }
        
        //Setup a piped stream to get an input stream that can be used for feeding the chuck encoded post 
    	PipedOutputStream	outputStream = new PipedOutputStream();
    	PipedInputStream inputStream = null;
        try {
	        inputStream = new PipedInputStream(outputStream);
        } catch (IOException e3) {
	        // TODO Auto-generated catch block
	        e3.printStackTrace();
        }
        
        //start up the microphone
    	MicRecognizer2 mr = new MicRecognizer2();
    	mr.start(audioLine,outputStream);
    	
    	/*
    	// Add the grammar string (read from a URL in this case)
    	StringBuilder sb = null;
    	try {
    	   BufferedReader gin = new BufferedReader(new InputStreamReader(grammarUrl.openStream()));
    	   
	        sb = new StringBuilder();
	        String line2 = null;
	        while ((line2 = gin.readLine()) != null) {
	            sb.append(line2);
	            sb.append(CRLF);
	        }
       	} catch (MalformedURLException e1) {
    		_logger.info(e1.getStackTrace());
    	} catch (IOException e2) {
    		_logger.info(e2.getStackTrace());
    	}
    	_logger.info("Grammar: "+sb);
    	*/
    	
    	
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
        InputStreamBody audioBody = new InputStreamBody(inputStream, "audio/x-wav","audio.wav");      
        
        // get the audio format and send as form fields.  Cound not send aduio file with format included because audio files do not
        // support mark/reset.  That is needed for stremaing using http chunk encoding on the servlet side using file upload.
        AudioFormat format = audioStream.getFormat();
        _logger.info("Actual format: " + format);    	
    	StringBody sampleRate = null;
    	StringBody bigEndian = null;
    	StringBody bytesPerValue = null;
    	StringBody encoding = null;
        try {
        	sampleRate = new StringBody(String.valueOf((int)format.getSampleRate()));
        	bigEndian = new StringBody(String.valueOf(format.isBigEndian()));
        	bytesPerValue =new StringBody(String.valueOf(format.getSampleSizeInBits()/8));
        	encoding = new StringBody(format.getEncoding().toString());
        	
        } catch (UnsupportedEncodingException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
        
        //add the form field parts
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
    
	    //execute the post command
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
        
        //get the response from the post
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

    
    private static final void log(BindingProvider port) {
        if (Boolean.getBoolean("wsmonitor")) {
            String address = (String)port.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
            address = address.replaceFirst("8080", "4040");
            port.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);
        }
    }
    
    
    /** This Thread records audio, and caches them in an audio buffer. */
    class RecordingThread extends Thread {

        private boolean done = false;
        private long totalSamplesRead = 0;

        private TargetDataLine aline;
        private OutputStream ostream;

        /**
         * Creates the thread with the given name
         *
         * @param name the name of the thread
         */
        public RecordingThread(String name, TargetDataLine audioLine, OutputStream outputStream ) {
            super(name);
            aline = audioLine;
            ostream = outputStream;
        }


        /** Starts the thread, and waits for recorder to be ready */
        public void start() {
    		aline.start();
			super.start();
        }


        /**
         * Stops the thread. This method does not return until recording has actually stopped, and all the data has been
         * read from the audio line.
         */
        public void stopRecording() {
        	
			aline.stop();
			aline.drain();
			aline.close();
			//m_bRecording = false;


        }


        public void run() {
        	
            // Read the next chunk of data from the TargetDataLine.
            //byte[] data = new byte[frameSizeInBytes];

			// TODO: intelligent size
			//byte[]	abBuffer = new byte[65536];
			byte[] data = new byte[aline.getBufferSize() / 5];

			AudioFormat	format = aline.getFormat();

			while (!done) {
				_logger.info("trying to read: " + data.length);
	            int numBytesRead = aline.read(data, 0, data.length);
				_logger.info(" ...read: " + numBytesRead);
	            int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
                totalSamplesRead += (numBytesRead / sampleSizeInBytes);
               
                /*
                
                if (numBytesRead != frameSizeInBytes) {
    				_logger.info(" Number of bytes read not eaual to frame szie in bytes");
                    if (numBytesRead % sampleSizeInBytes != 0) {
                        throw new Error("Incomplete sample read.");
                    }

                    byte[] shrinked = new byte[numBytesRead];
                    System.arraycopy(data, 0, shrinked, 0, numBytesRead);
                    data = shrinked;
                }
                
                double[] samples;

                if (bigEndian) {
                    samples = DataUtil.bytesToValues
                            (data, 0, data.length, sampleSizeInBytes, signed);
                } else {
                    samples = DataUtil.littleEndianBytesToValues
                            (data, 0, data.length, sampleSizeInBytes, signed);
                }


                if (channels > 1) {
                    samples = convertStereoToMono(samples, channels);
                }
                */
             
				try {
					ostream.write(data, 0, numBytesRead);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (totalSamplesRead > limit)
					done = true;
			}

			_logger.info("Done! "+ totalSamplesRead);
			/* We close the output stream.
			 */
			try {
				ostream.close();
			} catch (IOException e) {
				e.printStackTrace();
			} 	
    
        }
    }

    /**
     * Converts a little-endian byte array into an array of doubles. Each consecutive bytes of a float are converted
     * into a double, and becomes the next element in the double array. The number of bytes in the double is specified
     * as an argument. The size of the returned array is (data.length/bytesPerValue).
     *
     * @param data          a byte array
     * @param offset        which byte to start from
     * @param length        how many bytes to convert
     * @param bytesPerValue the number of bytes per value
     * @param signedData    whether the data is signed
     * @return a double array, or <code>null</code> if byteArray is of zero length
     * @throws java.lang.ArrayIndexOutOfBoundsException
     *
     */
    public static final double[] littleEndianBytesToValues(byte[] data,
    		int offset,
    		int length,
    		int bytesPerValue,
    		boolean signedData)
    throws ArrayIndexOutOfBoundsException {

    	if (0 < length && (offset + length) <= data.length) {
    		assert (length % bytesPerValue == 0);
    		double[] doubleArray = new double[length / bytesPerValue];

    		int i = offset + bytesPerValue - 1;

    		for (int j = 0; j < doubleArray.length; j++) {
    			int val = (int) data[i--];
    			if (!signedData) {
    				val &= 0xff; // remove the sign extension
    			}
    			for (int c = 1; c < bytesPerValue; c++) {
    				int temp = (int) data[i--] & 0xff;
    				val = (val << 8) + temp;
    			}

    			// advance 'i' to the last byte of the next value
    			i += (bytesPerValue * 2);

    			doubleArray[j] = (double) val;
    		}

    		return doubleArray;

    	} else {
    		throw new ArrayIndexOutOfBoundsException
    		("offset: " + offset + ", length: " + length
    				+ ", array length: " + data.length);
    	}
    }

    /**
     * Converts a big-endian byte array into an array of doubles. Each consecutive bytes in the byte array are converted
     * into a double, and becomes the next element in the double array. The size of the returned array is
     * (length/bytesPerValue). Currently, only 1 byte (8-bit) or 2 bytes (16-bit) samples are supported.
     *
     * @param byteArray     a byte array
     * @param offset        which byte to start from
     * @param length        how many bytes to convert
     * @param bytesPerValue the number of bytes per value
     * @param signedData    whether the data is signed
     * @return a double array, or <code>null</code> if byteArray is of zero length
     * @throws java.lang.ArrayIndexOutOfBoundsException
     *
     */
    public static final double[] bytesToValues(byte[] byteArray,
    		int offset,
    		int length,
    		int bytesPerValue,
    		boolean signedData)
    throws ArrayIndexOutOfBoundsException {

    	if (0 < length && (offset + length) <= byteArray.length) {
    		assert (length % bytesPerValue == 0);
    		double[] doubleArray = new double[length / bytesPerValue];

    		int i = offset;

    		for (int j = 0; j < doubleArray.length; j++) {
    			int val = (int) byteArray[i++];
    			if (!signedData) {
    				val &= 0xff; // remove the sign extension
    			}
    			for (int c = 1; c < bytesPerValue; c++) {
    				int temp = (int) byteArray[i++] & 0xff;
    				val = (val << 8) + temp;
    			}

    			doubleArray[j] = (double) val;
    		}

    		return doubleArray;
    	} else {
    		throw new ArrayIndexOutOfBoundsException
    		("offset: " + offset + ", length: " + length
    				+ ", array length: " + byteArray.length);
    	}
    }


}
