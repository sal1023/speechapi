package com.spokentech.speechdown.cli;

import com.spokentech.speechdown.client.SpeechAttachService;
import com.spokentech.speechdown.client.SpeechAttachPortType;
import com.spokentech.speechdown.client.SynthResponseAttachType;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import javax.activation.DataHandler;
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

import com.sun.xml.ws.developer.JAXWSProperties;

public class Synthesizer {
    private static Logger _logger = Logger.getLogger(Synthesizer.class);
    public static final String CRLF = "\r\n";
    
	private static  QName speechAttachQName = new QName("http://spokentech.com/speechcloud", "SpeechAttachPort");
    

    public static final String HELP_OPTION = "help";
    public static final String SERVICE_OPTION = "service";

    private static Options getOptions() {

        Options options = new Options();
        Option option = new Option(HELP_OPTION, "print this message");
        options.addOption(option);

        option = new Option(SERVICE_OPTION, true, "location of resource server (defaults to localhost)");
        option.setArgName("service");
        options.addOption(option);

        return options;
   }
    
    
    public static void main (String[] args) {

    	
    	_logger.info("Starting Synthesizer ...");
    	
    	
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

    	if (args.length != 2 || line.hasOption(HELP_OPTION)) {
    		HelpFormatter formatter = new HelpFormatter();
    		formatter.printHelp("Synthesizer [options] <text string> <output audio-file>", options);
    		return;
    	}

    	
    	String prompt = args[0];
    	String outFileName = args[1];
    	  
    	// lookup resource server
    	String service = line.hasOption(SERVICE_OPTION) ? line.getOptionValue(SERVICE_OPTION) : null; 

    	
    	/*  
    	 * Link api (maybe add this later as a command line arg
    	 * 
    	 * 
        SpeechLinkPortType port = new SpeechLinkService().getSpeechLinkPort();
        //log((BindingProvider)port);
	    String prompt = "Hello world!";
        String ttsAudio = port.synthesize (prompt);
        _logger.info("The synthesis result for Link Service: "+ttsAudio);
        */  
    	
    	URL serviceUrl = null;
    	SpeechAttachService aService = null;
    	//if url specified
    	if (service != null) {
    	    try {
    	       serviceUrl = new URL(service);
 			} catch (MalformedURLException e) {  
    	         e.printStackTrace();  
 			}
    	    aService = new SpeechAttachService(serviceUrl,speechAttachQName);    			
    	}else {
    	   //else (use the default url)
    	    aService = new SpeechAttachService();
    	}
    	
    	//setup http steaming on the client side
    	MTOMFeature feature = new MTOMFeature();
    	SpeechAttachPortType port2 = aService.getSpeechAttachPort(feature);
    	Map<String, Object> ctxt = ((BindingProvider)port2).getRequestContext();
    	// Enable HTTP chunking mode, otherwise HttpURLConnection buffers
    	ctxt.put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);
        //log((BindingProvider)port);  
    
    	//Synthesize request (returns a wav file attachment)
    	_logger.info("Starting the synthesis test (attach service)");  	
        SynthResponseAttachType response = port2.synthesize (prompt);
        DataHandler dh = response.getAudio();
        FileOutputStream out = null;
        InputStream in = null;
        
        //write the attachment to file system
        try {
            in = dh.getInputStream();
            out = new FileOutputStream(outFileName);
            
            // TODO  Add buffered streams
            //inputStream = new BufferedInputStream(in);
            //outputStream = new BufferedOutputStream(out);

            int c;

            while ((c = in.read()) != -1) {
                out.write(c);
            }
        } catch (IOException e) {
        	e.printStackTrace();
        

        } finally {
        	try {
	            if (in != null) {
	                in.close();
	            }
	            if (out != null) {
	                out.close();
	            }
            } catch (IOException e) {
            	e.printStackTrace();
            }
        }

	    _logger.info("The synthesis result is: "+response.getAudio().getName());
	
    }

    private static final void log(BindingProvider port) {
        if (Boolean.getBoolean("wsmonitor")) {
            String address = (String)port.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
            address = address.replaceFirst("8080", "4040");
            port.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);
        }
    }
}
