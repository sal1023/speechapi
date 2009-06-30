package com.spokentech.speechdown.cli;

import com.spokentech.speechdown.client.SpeechAttachService;
import com.spokentech.speechdown.client.SpeechAttachPortType;
import com.spokentech.speechdown.client.RecRequestAttachType;
import com.spokentech.speechdown.client.RecResponseType;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.net.MalformedURLException;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

import javax.activation.DataHandler;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;

import javax.sound.sampled.TargetDataLine;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.namespace.QName;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.InvalidRecognitionResultException;
import com.spokentech.speechdown.common.RecognitionResult;
import com.sun.xml.ws.developer.JAXWSProperties;

/**
 * Test program that uses the web services api to send audio from the mic.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@users.spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
public class MicRecognizer {
    private static Logger _logger = Logger.getLogger(MicRecognizer.class);
    public static final String CRLF = "\r\n";
    
	private static  QName speechAttachQName = new QName("http://spokentech.com/speechdown", "SpeechAttachPort");
    

    public static final String HELP_OPTION = "help";
    public static final String SERVICE_OPTION = "service";
    public static final String LIMIT_OPTION = "limit";
    
    private static int limit = 10000;
    
    
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
    	String service = line.hasOption(SERVICE_OPTION) ? line.getOptionValue(SERVICE_OPTION) : null; 

    	URL serviceUrl = null;
    	SpeechAttachService aService = null;
    	//if url specified
    	if (service != null) {
    	    try {
    	        serviceUrl = new URL(service);
 			} catch (MalformedURLException e) {  
    	         e.printStackTrace();  
 			}
    	    aService  = new SpeechAttachService(serviceUrl,speechAttachQName);    			
    	}else {
    	   //else (use the default url)
    	    aService = new SpeechAttachService();
    	}
    	
    	
    	
    	//
    	//
    	//
    	
        desiredFormat = new AudioFormat ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, desiredFormat);
        if (!AudioSystem.isLineSupported(info)) {
            _logger.info(desiredFormat + " not supported");
            throw new RuntimeException("unsupported audio format");
        } 
        
        _logger.info("Desired format: " + desiredFormat);
        
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


                /* Set the frame size depending on the sample rate.
                 */
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
        
    	/*
    	 *  Web service approach 1 of 3
    	 *
    	 *  
    	*/
    	//setup http steaming on the client side
    	MTOMFeature feature = new MTOMFeature();
    	SpeechAttachPortType port2 = aService.getSpeechAttachPort(feature);
    	Map<String, Object> ctxt = ((BindingProvider)port2).getRequestContext();
    	// Enable HTTP chunking mode, otherwise HttpURLConnection buffers
    	ctxt.put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);
        //log((BindingProvider)port);
        
        //Add the audio file attachment
    	
    	
    	PipedOutputStream	outputStream = new PipedOutputStream();
    	PipedInputStream inputStream = null;
        try {
	        inputStream = new PipedInputStream(outputStream);
        } catch (IOException e3) {
	        // TODO Auto-generated catch block
	        e3.printStackTrace();
        }
    	
        /*
         * Web Service Approach 2 of 3
         *
         */
    	DataHandler rdh = new DataHandler(new MyDataSource(inputStream)); 
    	RecRequestAttachType recRequest = new RecRequestAttachType();
    	recRequest.setAudio(rdh);
    	
        
    	MicRecognizer mr = new MicRecognizer();
    	mr.start(audioLine,outputStream);
    	
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
    	   
    	/*
    	 *  *** Web Service Approach 3 of 3
    	 *
    	*/  
    	recRequest.setGrammar(sb.toString()); 
    	
    	//Make the Recognition request (returns the recognition results)
    	RecResponseType recResult = port2.recognize (recRequest);

    	try {
    	   RecognitionResult r = RecognitionResult.constructResultFromString(recResult.getSerialized());
    	   _logger.info("The recognition result is: "+r.getText());
    	} catch (InvalidRecognitionResultException e) {
    		e.printStackTrace();
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
