/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.FileUploadBase.IOFileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.log4j.Logger;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.gson.Gson;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.Utterance;

import com.spokentech.speechdown.common.HttpCommandFields;
import com.spokentech.speechdown.common.Utterance.OutputFormat;
import com.spokentech.speechdown.server.domain.SpeechRequestDTO;
import com.spokentech.speechdown.server.util.ServiceLogger;

/**
 * Servlet for uploading audio for speech recognition processing.
 *
 * @author slord
 * @author nielsgodfredsen
 */
@SuppressWarnings("serial")
public class SpeechUploadServlet extends HttpServlet {

	private static Logger _logger = Logger.getLogger(SpeechUploadServlet.class);

	//Format default values (used when sent separately as form fields)
    static final int DEFAULT_SAMPLE_RATE = 8000;
    static final boolean DEAFUALT_BIG_ENDIAN = true;
    static final int DEFAULT_BYTESPERVALUE = 2;
    static final String DEFAULT_ENCODING = AudioFormat.Encoding.PCM_SIGNED.toString();
    static final int DEFAULT_FRAMESIZEINBYTES = 2;
    static final int DEFAULT_FRAMERATE = 8000;
    static final boolean DEFAULT_SIGNED = true;
    static final int DEFAULT_CHANNELS = 1;

	private DiskFileItemFactory factory;
	private File destinationDir;
	
	SynthesizerService synthesizerService;
	RecognizerService recognizerService;

	
 	private static final int	EXTERNAL_BUFFER_SIZE = 3200;

	private static final String DEFAULT_LANGMODEL = "en";
	private static final String DEFAULT_AMODEL = "en";
	private static final String DEFAULT_DICT = "en";

	byte[]	abData = new byte[EXTERNAL_BUFFER_SIZE];


	private boolean serviceLogEnabled;

	private  Gson gson;
	
	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config);

		String tmp = config.getInitParameter("serviceLogging");
		serviceLogEnabled = false;
		if (tmp.compareToIgnoreCase("true") == 0)
			serviceLogEnabled = true;
		
		String tempDirParam = config.getInitParameter("tempDir");
		File tempDir = (tempDirParam != null) ? new File(tempDirParam) : null;

		String destinationDirParam = config.getInitParameter("destinationDir");
		if (destinationDirParam == null)
			throw new ServletException("'destinationDir' is a required parameter!");
		this.destinationDir = new File(destinationDirParam);

		this.factory = new DiskFileItemFactory(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD, tempDir);
		this.factory.setFileCleaningTracker(FileCleanerCleanup.getFileCleaningTracker(config.getServletContext()));

		ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		synthesizerService = (SynthesizerService)context.getBean("synthesizerService");
		recognizerService = (RecognizerService)context.getBean("recognizerService");
	    gson = new Gson();
		  
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		long start = System.currentTimeMillis();
		_logger.debug("New request came in" + start);
		
		if (_logger.isDebugEnabled()) {
			Enumeration hnames = request.getHeaderNames();
		    while (hnames.hasMoreElements()) {
		        String key = (String) hnames.nextElement();
		        _logger.debug(key + " -- " + request.getHeader(key));
		    }
		    
			Enumeration anames = request.getAttributeNames();
		    while (anames.hasMoreElements()) {
		        String key = (String) anames.nextElement();
		        _logger.debug(key + " aa " + request.getAttribute(key));
		    }
		    
			Enumeration pnames = request.getParameterNames();
		    while (pnames.hasMoreElements()) {
		        String key = (String) pnames.nextElement();
		        _logger.debug(key + " pp " + request.getParameter(key));
		    }
	
		    _logger.debug("protocol "+request.getProtocol());
		    _logger.debug("scheme: "+request.getScheme());	
		    _logger.debug("method: "+request.getMethod());
		    _logger.debug("PATH "+request.getContextPath());	
	
		    _logger.debug("remote addr :"+request.getRemoteAddr());
		    _logger.debug("remote host: "+request.getRemoteHost());
		    _logger.debug("remote port: "+request.getRemotePort());
		    _logger.debug("Local adr "+request.getLocalAddr());
		    _logger.debug("Local name "+request.getLocalName());
		    _logger.debug("Local port "+request.getLocalPort());
	
		    _logger.debug("loacale: "+request.getLocale()); 
		}
		
		// Check that we have a file upload request
		if (!ServletFileUpload.isMultipartContent(request)) {
			_logger.debug("redirecting to / because not multipart content");
			response.sendRedirect(response.encodeRedirectURL("/")); // TODO: allow redirect location to be configured
			return;
		}
		
		//System.out.println(request.toString());
		//Enumeration enumer = request.getParameterNames();
	    //while (enumer.hasMoreElements()) {
	    //    String key = (String) enumer.nextElement();
	    //    _logger.debug(key + " -- " + request.getParameter(key));
	    //}
		
	    boolean lmFlag = false;
		String gMode = "lm";
		String lmId = DEFAULT_LANGMODEL;
		String amId = DEFAULT_AMODEL;
		String dictId = DEFAULT_DICT;
	    boolean continuous = false;
	    boolean doEndpointing = false;
	    boolean cmnBatch = false;
	    OutputFormat outMode = OutputFormat.text;
	    
	    
    	String developerId =null;	
    	String developerSecret = null;
     	String userId = null;
     	String developerDefined = null;
    	
    	boolean doOog = false;
    	double oogBranchProb = 1e-25; 
    	double phoneInsertionProb = 1e-20;

	    
	    
		//set the audio format parameters to default values
		boolean formatSpecified = false;
	    AFormat af = new AFormat(DEFAULT_ENCODING, DEFAULT_SAMPLE_RATE, DEFAULT_BYTESPERVALUE*8,  DEFAULT_CHANNELS,  DEAFUALT_BIG_ENDIAN,
	    		DEFAULT_SIGNED,  DEFAULT_FRAMESIZEINBYTES,  DEFAULT_FRAMERATE);
	    

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload();

		InputStream audio = null;
		String grammarString = null;
		Utterance result = null;
		String textResult = null;
    	//TODO: grammar must be the first item processed in the iterator
		// Parse the request
		try {
			FileItemIterator iter = upload.getItemIterator(request);
			while (iter.hasNext()) {
			    FileItemStream item = iter.next();
			    String name = item.getFieldName();
			    InputStream stream = item.openStream();
			    String contentType = item.getContentType();
			    if (item.isFormField()) {
			    	String value = Streams.asString(stream,"UTF-8");
			        _logger.debug("Form field " + name + " with value " + value + " detected.");
			        try { 
			        	//form fields used to specify format, if not specified using format data in attachment header
				        if (name.equalsIgnoreCase(HttpCommandFields.SAMPLE_RATE_FIELD_NAME)) {
				        	af.setSampleRate((double)Integer.parseInt(value));
		        		    formatSpecified = true;
				        }else if (name.equalsIgnoreCase(HttpCommandFields.LANGUAGE_FIELD_NAME)) {
				        	//TODO: just pass in language rather than these 3 ID's all set to the same
				        	amId = value;
				        	dictId = value;
				        	lmId = value;
				        } else if (name.equalsIgnoreCase(HttpCommandFields.BIG_ENDIAN_FIELD_NAME)) {
				        	af.setBigEndian( Boolean.parseBoolean(value));
		        		    formatSpecified = true;
			        	} else if (name.equalsIgnoreCase(HttpCommandFields.BYTES_PER_VALUE_FIELD_NAME)) {
				        	af.setSampleSizeInBits( Integer.parseInt(value)*8);
		        		    formatSpecified = true;
		        		} else if (name.equalsIgnoreCase(HttpCommandFields.ENCODING_FIELD_NAME)) {
		        		    formatSpecified = true;
		        			/*if (value.equals(AudioFormat.Encoding.ALAW.toString())) {
		        				encoding = AudioFormat.Encoding.ALAW;
		        			} else if (value.equals(AudioFormat.Encoding.ULAW.toString())) {
		        				encoding = AudioFormat.Encoding.ULAW;
		        			} else if (value.equals(AudioFormat.Encoding.PCM_SIGNED.toString())) {
		        				encoding = AudioFormat.Encoding.PCM_SIGNED;
		        			} else if (value.equals(AudioFormat.Encoding.PCM_UNSIGNED.toString())) {
		        				encoding = AudioFormat.Encoding.PCM_UNSIGNED;
		        			} else {
		        				_logger.warn("Unsupported encoding: "+value);
		        			}*/
		        			af.setEncoding(value);
		        	    //Other form fields
			        	} else if (name.equalsIgnoreCase(HttpCommandFields.CONTINUOUS_FLAG)) {
				        	continuous  = Boolean.parseBoolean(value);
			        	} else if (name.equalsIgnoreCase(HttpCommandFields.CMN_BATCH)) {
				        	cmnBatch  = Boolean.parseBoolean(value);
			        	} else if (name.equalsIgnoreCase(HttpCommandFields.ENDPOINTING_FLAG)) {
				        	doEndpointing  = Boolean.parseBoolean(value);
			        	} else if (name.equalsIgnoreCase(HttpCommandFields.OOG_FLAG)) {
				        	 doOog = Boolean.parseBoolean(value);
				        	
			        	}else if (name.equalsIgnoreCase(HttpCommandFields.OUT_OF_GRAMMAR_BRANCH_PROB)) {
					         oogBranchProb = (double)Double.parseDouble(value);
			        	}else if (name.equalsIgnoreCase(HttpCommandFields.PHONE_INSERTION_PROB)) {
				        	 phoneInsertionProb = (double)Double.parseDouble(value);
				        	

	
				        } else if (name.equalsIgnoreCase(HttpCommandFields.LANGUAGE_MODEL_FLAG)) {
				        	// for backwards compatability with lmflag
			        		lmFlag = Boolean.parseBoolean(value);
			        		gMode="jsgf";
			        		if (lmFlag)
			        			gMode="lm";
				        } else if (name.equalsIgnoreCase(HttpCommandFields.GRAMMAR_MODE)) {
				        	gMode = value;
				        } else if (name.equalsIgnoreCase(HttpCommandFields.ACOUSTIC_MODEL_ID)) {
				        	amId = value;
				        } else if (name.equalsIgnoreCase(HttpCommandFields.LANGUAGE_MODEL_ID)) {
				        	lmId = value;
				        } else if (name.equalsIgnoreCase(HttpCommandFields.DICTIONARY_ID)) {
				        	dictId = value;
				        } else if (name.equalsIgnoreCase(HttpCommandFields.OUTPUT_MODE)) {
				        	outMode = OutputFormat.valueOf(value);
				        	
				        } else if (name.equalsIgnoreCase(HttpCommandFields.DEVELOPER_ID)) {
				        	developerId = value;
				        } else if (name.equalsIgnoreCase(HttpCommandFields.DEVELOPER_SECRET)) {
				        	developerSecret = value;
				        } else if (name.equalsIgnoreCase(HttpCommandFields.USER_ID)) {
				        	userId = value;
				        } else if (name.equalsIgnoreCase(HttpCommandFields.DEVELOPER_DEFINED)) {
				        	developerDefined = value;
				        } else {
				        	_logger.warn("Unrecognized field "+name+ " = "+value);
				        }
			        } catch (Exception e) {
			        	_logger.warn("Exception " + e.getMessage() + "ooccured while processing field name= "+name +" with value= "+value);
			        }
			    } else {
			        _logger.debug("File field " + name + " with file name "
			            + item.getName() + " detected.");

			        FileItemHeaders h = item.getHeaders();
			        if (h == null) {
			        	_logger.debug(item.getName() +" has no headers");
			        } else {
			        	_logger.debug(item.getName() +" has the following headers");
				        Iterator it = h.getHeaderNames();
						while (it.hasNext()) {
						    String hname = (String) it.next();
						    String head =h.getHeader(hname);
						    _logger.debug(hname+" = "+head);
						}
			        }

			        // Process the input stream
				    if (name.equals("audio") ) {
				    	audio = stream;
				    	//try {
			    			//log the http request parameters to the database
		    		        SpeechRequestDTO hr = new SpeechRequestDTO();
			    			if (serviceLogEnabled) {
			    		        hr.setProtocol(request.getProtocol());
				    		    hr.setScheme(request.getScheme());	
				    		    hr.setMethod(request.getMethod());
				    		    hr.setContextPath(request.getContextPath());	
				    		    hr.setRemoteAddr(request.getRemoteAddr());
				    		    hr.setRemoteHost(request.getRemoteHost());
				    		    hr.setRemotePort(request.getRemotePort());
				    		    hr.setLocalAddr(request.getLocalAddr());
				    		    hr.setLocalName(request.getLocalName());
				    		    hr.setLocalPort(request.getLocalPort());
				    		    hr.setLocale(request.getLocale().toString()); 
				    			hr.setDate(new Date());
				    			hr.setDeveloperId(developerId);
				    			hr.setUserId(userId);
				    			hr.setDevDefined(developerDefined);
			    			    //ServiceLogger.logHttpRequest(hr);
			    			}
			    			_logger.debug("SESSIONID: "+request.getRequestedSessionId());
			    			//if format not specified, then set it to null.  that indicates that the front end should  try to extract it from the input streams header
			    			if (formatSpecified) {
				    		   _logger.debug("recognizing audio!  Sample rate= "+af.getSampleRate()+", bigEndian= "+ af.isBigEndian()+", bytes per value= "+af.getSampleSizeInBits()+", encoding= "+af.getEncoding());
			    			} else {			    				
			    				af = null;
			    			}
				    		_logger.debug("continuous: "+continuous+" lmflag: "+lmFlag+ " endpointing: "+doEndpointing);
				    		if (continuous) {
						        
				    			response.setCharacterEncoding("UTF-8");
								PrintWriter out = response.getWriter();	
				    			//OutputStream out = response.getOutputStream();
				    			response.setContentType("text/plain");
				    			//response.setHeader("Content-Disposition", "attachment; filename=results.txt'");			
				    			response.setHeader("Transfer-coding","chunked");
					    		if (gMode.equalsIgnoreCase("lm")) {
					    			textResult = recognizerService.Transcribe(audio,contentType,af,outMode,out,response,amId,lmId,dictId,hr);
					    		} else {
							        _logger.debug("recognition result is null");
								    out.println("recognition result is null");
					    		}
					    		audio.close();
					    		request.getInputStream().close();
					    		long stop2 =  System.currentTimeMillis();
				    			_logger.debug("Done! " + stop2 +" ("+(stop2-start) +")" );
				    		} else {
				    			long stop = System.currentTimeMillis();
				    			_logger.debug("Calling recognizer Service" + stop +" ("+(stop-start) +")" );
				    		
				        	
					          	if (gMode.equalsIgnoreCase("simple")) {
					          		String jsgfGrammar = simpleToJsgf(grammarString);
					          		_logger.debug("New Gram:\n"+jsgfGrammar);
					    	        result = recognizerService.Recognize(audio, jsgfGrammar,contentType,af,outMode, doEndpointing,cmnBatch,    	
					    	            	doOog, oogBranchProb, phoneInsertionProb,amId,lmId,dictId,hr);
					          	} else if (gMode.equalsIgnoreCase("jsgf") ) {
					    	        result = recognizerService.Recognize(audio, grammarString,contentType,af,outMode, doEndpointing,cmnBatch,    	
					    	            	doOog, oogBranchProb, phoneInsertionProb,amId,lmId,dictId,hr);
					          	}  else if (gMode.equalsIgnoreCase("lm")) {
					    			result = recognizerService.Recognize(audio,contentType,af,outMode, doEndpointing,cmnBatch,amId,lmId,dictId,hr);
					          	} else {
					          		_logger.warn("Unrecognized grammar mode: "+gMode+" using defualt language model");
					    			result = recognizerService.Recognize(audio,contentType,af,outMode, doEndpointing,cmnBatch,amId,lmId,dictId,hr);
					          	}
					          		


				    						    			
				    		}

				    } else if (name.equals("grammar")) {
						grammarString = readInputStreamAsString(stream);
				    }
			    }
			}

				
			// store file list and pass control to view jsp
			//request.setAttribute("fileUploadList", filenames);
			//this.getServletContext().getRequestDispatcher("/speechup_result.jsp").forward(request, response);

		} catch (IOFileUploadException e) {
			e.printStackTrace();
			result = new Utterance();
			result.setRCode("IOError");
			result.setRMessage(e.getMessage());
			throw (IOException) e.getCause();
		} catch (FileUploadException e) {
			e.printStackTrace();
			result = new Utterance();
			result.setRCode("UploadError");
			result.setRMessage(e.getMessage());
			throw new ServletException(e.getMessage(), e);
		
		} catch (NoSuchElementException e) {	
			_logger.warn(e.getMessage());
			result = new Utterance();
			result.setRCode("PoolExhausted");
			result.setRMessage(e.getMessage());
			
		} catch (Exception e) {	
			_logger.debug("Exception 1");
			result = new Utterance();
			result.setRCode("Error");
			result.setRMessage(e.getMessage());
			e.printStackTrace();
		} finally {

			audio.close();
			request.getInputStream().close();

			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/plain");
			PrintWriter out = response.getWriter();	
			if (result == null) {									
				result = new Utterance();
				result.setRCode("NullResult");
				result.setRMessage("recognition result is null");
			}
			
			if (outMode == OutputFormat.json) {
				textResult = gson.toJson(result);
				_logger.debug(textResult);
				out.println(textResult);
		    	/*try {
		            FileOutputStream fos = new FileOutputStream("c:\\temp\\test-server.txt");
		            Writer out2 = new OutputStreamWriter(fos, "UTF8");
		            out2.write(textResult);
		            out2.close();
		        } 
		        catch (IOException e) {
		            e.printStackTrace();
		        }*/
			} else if (outMode == OutputFormat.text) {	
				textResult = result.getText();
				_logger.debug(textResult);
				out.println(textResult);								
			} else {
				textResult = result.getText();
				_logger.warn("Unsupported output format, using text +outMode");
				_logger.debug(textResult);
				out.println(textResult);	
			}
			long stop2 =  System.currentTimeMillis();
			_logger.debug("Done! " + stop2 +" ("+(stop2-start) +")" );
		    //String filename = Long.toString(System.currentTimeMillis()) + ".wav";
			//writeStreamToFile2(audio,filename);
			
			
		}
	}



	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendRedirect(response.encodeRedirectURL("/")); // TODO: allow redirect location to be configured
	}
	
	public  String readInputStreamAsString(InputStream in) throws IOException {

		
	    StringBuffer buffer = new StringBuffer();
		InputStreamReader isr = new InputStreamReader(in,"UTF8");
		Reader inReader = new BufferedReader(isr);
		int ch;
		while ((ch = inReader.read()) > -1) {
			buffer.append((char)ch);
		}
		inReader.close();
		_logger.debug(buffer.toString());
		return buffer.toString();
		
		/* Old 
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
		*/
	}

	
	private String simpleToJsgf(String grammarString) {

	    StringBuffer result = new StringBuffer("#JSGF V1.0;\ngrammar simple;\npublic <main> =(");

	    result.append(grammarString.replace(",", "|"));
	    result.append(");\n");

	    return result.toString();

    }
	
	public void writeStreamToFile2(InputStream inputStream, String fileName) {
		
	  	File outWavFile = new File(fileName);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);

    	//TODO: Assuming it is a wav file.  Should get the actual type of input file
    	AudioFileFormat.Type outputType = null;
    	AudioFileFormat.Type[] typesSupported = AudioSystem.getAudioFileTypes();
    	for (AudioFileFormat.Type aTypesSupported : typesSupported) {
    		if (aTypesSupported.getExtension().equals("wav")) {
    			outputType =  aTypesSupported;
    		}
    	}
        //AudioFormat audioFormat = audioStream.getFormat();
		
		_logger.debug("Out of the the Loop "+baos.size());
		//write it to the new file
        int bitsPerSample = 16;
        int sampleRate = 8000;
        boolean isBigEndian = true;
        boolean isSigned = true;
	
		//AudioFormat outFormat = new AudioFormat(audioFormat.getFrameRate(),audioFormat.getSampleSizeInBits(), 1, true, true);
		//AudioInputStream ais = new AudioInputStream(bais, outFormat, outAudioData.length / audioFormat.getFrameSize());
		//_logger.debug(audioFormat.toString());

		
	  	int nBytesRead=0;
		while (nBytesRead != -1){  		

			//read the data from the file
			try{
				nBytesRead = inputStream.read(abData, 0, abData.length);
				_logger.debug("Read "+nBytesRead+ " bytes");
				if (nBytesRead >0) {
	    			dos.write(abData, 0, nBytesRead);
	    			_logger.debug("then wrote them ");
				}
			}catch (IOException e){
				e.printStackTrace();
			}

		}
		byte[] outAudioData = baos.toByteArray();
		ByteArrayInputStream bais = new ByteArrayInputStream(outAudioData);	  
        AudioFormat wavFormat = new AudioFormat(sampleRate, bitsPerSample, 1, isSigned, isBigEndian);
        AudioInputStream ais = new AudioInputStream(bais, wavFormat, outAudioData.length / wavFormat.getFrameSize());

		if (AudioSystem.isFileTypeSupported(outputType, ais)) {
			try {
				_logger.debug("writing file "+outWavFile.getCanonicalPath());
				AudioSystem.write(ais, outputType, outWavFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			_logger.warn("output type not supported..."); 
		}
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
			_logger.debug("Closing streams");
			in.close(); 
			out.close(); 
		} 
		catch (Exception e) { 
			_logger.warn("upload Exception"); e.printStackTrace(); 
			e.printStackTrace();
		} 
	}

}
