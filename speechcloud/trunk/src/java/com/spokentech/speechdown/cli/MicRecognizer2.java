package com.spokentech.speechdown.cli;

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

import com.spokentech.speechdown.client.HttpRecognizer;


/**
 * Test program that uses the restful api to send audio from the mic.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@users.spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
public class MicRecognizer2 {


	private static Logger _logger = Logger.getLogger(MicRecognizer2.class);

  

    public static final String HELP_OPTION = "help";
    public static final String SERVICE_OPTION = "service";
    public static final String S4_MODE_OPTION = "mode";
    public static final String STREAM_MODE_OPTION = "stream";
    
    //private static final String defaultService = "http://ec2-75-101-234-41.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";
    private static final String defaultService = "http://localhost:8090/speechcloud/SpeechUploadServlet";


    private static AudioFormat desiredFormat;
    private static int sampleRate = 8000;
    private static boolean signed = true;
    private static boolean bigEndian = true;
    private static int channels = 1;
    private static int sampleSizeInBits = 16;
    
    private static int audioBufferSize = 16000;
    private static int msecPerRead = 10;
    private static int frameSizeInBytes;
    
    private static boolean s4Mode = false;
    private static String streamMode = "audio";
    
      
    
 	private static final int	EXTERNAL_BUFFER_SIZE = 3200;
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	DataOutputStream dos = new DataOutputStream(baos);
	byte[]	abData = new byte[EXTERNAL_BUFFER_SIZE];


  
    public static void main (String[] args) {
	
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

    	if (line.hasOption(S4_MODE_OPTION))
    		s4Mode=true;
    	if (line.hasOption(STREAM_MODE_OPTION))
    		streamMode=line.getOptionValue(STREAM_MODE_OPTION);    	
    	
    	String mimeType = null;
    	if ((s4Mode) && (streamMode.equals("audio"))) {
    		mimeType = "audio/x-s4audio";
    	} else if ((!s4Mode) && (streamMode.equals("audio"))) {
    		mimeType = "audio/x-wav";
    	} else if ((s4Mode) && (streamMode.equals("feature"))) {
    		mimeType = "audio/x-s4feature";
    	} else if ((!s4Mode) && (streamMode.equals("feature"))) {
    		mimeType = "audio/x-wav";
    		_logger.warn("This command line combo not supported!, s4mode= "+s4Mode+" streamMode=" +streamMode);	
    	} else {
    		mimeType = "audio/x-wav";
    		_logger.warn("unrecognized comand line combo s4mode= "+s4Mode+" streamMode=" +streamMode);
    	}
    	
    	if (!s4Mode) {
    		HttpRecognizer.testmic(mimeType,grammar, service);
    	} else {
    		try {
	            HttpRecognizer.testmic2(mimeType,grammar, service);
            } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            } catch (InstantiationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
    	}
    	
    	/*
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
   
            audioLine.addLineListener(new LineListener() {
                public void update(LineEvent event) {
                    _logger.info("line listener " + event);
                }
            });
        } catch (LineUnavailableException e) {
            _logger.info("microphone unavailable " + e.getMessage());
        }
        
        //start up the microphone
        HttpRecognizer hr = new HttpRecognizer();
        hr.recognizeNextUtterance(grammarUrl, service, audioLine);
        
        //results are...
        */
    }

    private static Options getOptions() {

        Options options = new Options();
        Option option = new Option(HELP_OPTION, "print this message");
        options.addOption(option);

        option = new Option(SERVICE_OPTION, true, "location of resource server (defaults to localhost)");
        option.setArgName("service");
        options.addOption(option);
        
        option = new Option(S4_MODE_OPTION, true, "Endpointing Mode (s4 or normal) (defaults to Normal)");
        option.setArgName(S4_MODE_OPTION);
        options.addOption(option);
        
        option = new Option(STREAM_MODE_OPTION, true, "Stream Mode (feature or audio) (defaults to Audio)");
        option.setArgName(STREAM_MODE_OPTION);
        options.addOption(option);


        return options;
   }
    
}