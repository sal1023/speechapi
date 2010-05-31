package com.spokentech.speechdown.client;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;

import javax.activation.FileDataSource;
import javax.activation.DataHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.MTOMFeature;
import com.sun.xml.ws.developer.JAXWSProperties;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jvnet.staxex.StreamingDataHandler;


import com.spokentech.speechdown.client.SpeechAttachPortType;
import com.spokentech.speechdown.client.SpeechAttachService;
import com.spokentech.speechdown.client.SpeechLinkPortType;
import com.spokentech.speechdown.client.SpeechLinkService;
import com.spokentech.speechdown.common.InvalidRecognitionResultException;
import com.spokentech.speechdown.client.RecRequestAttachType;
import com.spokentech.speechdown.client.RecRequestLinkType;
import com.spokentech.speechdown.client.RecResponseType;
import com.spokentech.speechdown.client.SynthResponseAttachType;

public class SpeechServiceClient {
    private static Logger _logger = Logger.getLogger(SpeechServiceClient.class);
    public static final String CRLF = "\r\n";
    public static void main (String[] args) {

    	
    	_logger.debug("Starting Link service tests ...");
    	
    	
        SpeechLinkPortType port = new SpeechLinkService().getSpeechLinkPort();
            //log((BindingProvider)port);


	    String audio = "http://www.spokentech.com/utterance.wav";
	    String grammar = "http://www.spokentech.com/grammar.jsgf";
	    RecRequestLinkType linkRequest = new RecRequestLinkType();
	    linkRequest.grammar = grammar;
	    linkRequest.audioURL =audio;
        RecResponseType result = port.recognize (linkRequest);
        _logger.debug("The recognition result for Link Service: "+result);


	    String prompt = "Hello world!";
        String ttsAudio = port.synthesize (prompt);
        _logger.debug("The synthesis result for Link Service: "+ttsAudio);
        /*} catch (SynthesisException_Exception ex) {
            System.out.printf ("Caught SynthesisException_Exception: %s\n", ex.getFaultInfo ().getDetail ());
        } catch (RecognitionException_Exception ex) {
            System.out.printf ("Caught RecognitionException_Exception: %s\n", ex.getFaultInfo ().getDetail ());
        }*/
        
        _logger.debug("... done Link Service tests.");
    	_logger.debug("Starting Attach service tests ...");
    	_logger.debug("First the recognition request.");

    	
    	SpeechAttachService aService = new SpeechAttachService();
   
    	//setup http steaming on the client side
    	MTOMFeature feature = new MTOMFeature();
    	SpeechAttachPortType port2 = aService.getSpeechAttachPort(feature);
    	Map<String, Object> ctxt = ((BindingProvider)port2).getRequestContext();
    	// Enable HTTP chunking mode, otherwise HttpURLConnection buffers
    	ctxt.put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);
    	

        //log((BindingProvider)port);
        
        //Add the audio file attachmnent

        String filename = "./etc/prompts/lookupsports.wav"; 
    	File fstart = new File(filename);
    	_logger.debug("File name for the audio file: "+fstart.getAbsolutePath());
    	DataHandler rdh = new DataHandler(new FileDataSource(filename)); 
    	RecRequestAttachType recRequest = new RecRequestAttachType();
    	recRequest.setAudio(rdh);
    	
    	
    	// Add the grammar string (read from a URL in this case)
    	StringBuilder sb = null;
    	try {
    	   URL content = new URL("file:///C:/work/speechcloud/etc/grammar/example.gram");
    	   BufferedReader gin = new BufferedReader(new InputStreamReader(content.openStream()));
    	   
	        sb = new StringBuilder();
	        String line = null;
	        while ((line = gin.readLine()) != null) {
	            sb.append(line);
	            sb.append(CRLF);
	        }
       	} catch (MalformedURLException e1) {
    		_logger.info(e1.getStackTrace());
    	} catch (IOException e2) {
    		_logger.info(e2.getStackTrace());
    	}
    	_logger.debug("Grammar: "+sb);
    	recRequest.setGrammar(sb.toString());

    	//Make the Recognition request (returns the recognition results)
    	RecResponseType recResult = port2.recognize (recRequest);

    	//try {
    	//   RecognitionResult r = RecognitionResult.constructResultFromString(recResult.getSerialized());
    	//   _logger.debug("The recognition result is: "+r.getText());
    	//} catch (InvalidRecognitionResultException e) {
    	//	e.printStackTrace();
    	//}
    	
    	
    	//Synthesize request (returns a wav file attachment)
    	_logger.debug("Starting the synthesis test (attach service)");  	
        SynthResponseAttachType response = port2.synthesize (prompt);
        DataHandler dh = response.getAudio();

        InputStream in = null;
        

        
        FileOutputStream out = null;
   
        //write the attachment to file system
        try {
            if (dh instanceof StreamingDataHandler) {
            	_logger.debug("Data handler is instance of streamdatdaHandler");
                in = ((StreamingDataHandler) dh).readOnce();
            } else {
            	_logger.debug("Data hadnler is not streamdatahandler");
                in = dh.getInputStream();
            }
            out = new FileOutputStream("data.wav");
            
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

        
	    _logger.debug("The synthesis result is: "+response.getAudio().getName());

    }

    private static final void log(BindingProvider port) {
        if (Boolean.getBoolean("wsmonitor")) {
            String address = (String)port.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
            address = address.replaceFirst("8080", "4040");
            port.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, address);
        }
    }
}
