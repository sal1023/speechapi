package com.spokentech.speechdown.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;


import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

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

import com.spokentech.speechdown.common.RecognitionResult;

import com.spokentech.speechdown.common.HttpCommandFields;

/**
 * Servlet for uploading audio for speech recognition processing.
 *
 * @author nielsgodfredsen
 */
@SuppressWarnings("serial")
public class SpeechUploadServlet extends HttpServlet {

	private static Logger _logger = Logger.getLogger(SpeechUploadServlet.class);


	private DiskFileItemFactory factory;
	private File destinationDir;
	
	SynthesizerService synthesizerService;
	RecognizerService recognizerService;

	
 	private static final int	EXTERNAL_BUFFER_SIZE = 3200;

	byte[]	abData = new byte[EXTERNAL_BUFFER_SIZE];
	
	
	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {

		super.init(config);

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
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		Enumeration hnames = request.getHeaderNames();
	    while (hnames.hasMoreElements()) {
	        String key = (String) hnames.nextElement();
	        _logger.debug(key + " -- " + request.getHeader(key));
	      }

		
		// Check that we have a file upload request
		if (!ServletFileUpload.isMultipartContent(request)) {
			_logger.info("redirecting to / because not multipart content");
			response.sendRedirect(response.encodeRedirectURL("/")); // TODO: allow redirect location to be configured
			return;
		}
		
	    //set the audio format parameters to default values
	    int sampleRate = 8000;
	    boolean bigEndian = true;
	    boolean lmFlag = false;
	    boolean continuous = false;
	    boolean doEndpointing = false;
	    int bytesPerValue = 2;
	    AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload();

		InputStream audio = null;
		String grammarString = null;
		RecognitionResult result = null;
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
			    	String value = Streams.asString(stream);
			        _logger.debug("Form field " + name + " with value " + value + " detected.");
			        try { 
				        if (name.equals(HttpCommandFields.SAMPLE_RATE_FIELD_NAME)) {
				        	sampleRate = Integer.parseInt(value);
			        	} else if (name.equals(HttpCommandFields.CONTINUOUS_FLAG)) {
				        	continuous  = Boolean.parseBoolean(value);
			        	} else if (name.equals(HttpCommandFields.ENDPOINTING_FLAG)) {
				        	doEndpointing  = Boolean.parseBoolean(value);
				        } else if (name.equals(HttpCommandFields.LANGUAGE_MODEL_FLAG)) {
				        	lmFlag = Boolean.parseBoolean(value);
				        } else if (name.equals(HttpCommandFields.BIG_ENDIAN_FIELD_NAME)) {
				        	bigEndian = Boolean.parseBoolean(value);
			        	} else if (name.equals(HttpCommandFields.BYTES_PER_VALUE_FIELD_NAME)) {
				        	bytesPerValue = Integer.parseInt(value);
		        		} else if (name.equals(HttpCommandFields.ENCODING_FIELD_NAME)) {
		        			if (value.equals(AudioFormat.Encoding.ALAW.toString())) {
		        				encoding = AudioFormat.Encoding.ALAW;
		        			} else if (value.equals(AudioFormat.Encoding.ULAW.toString())) {
		        				encoding = AudioFormat.Encoding.ULAW;
		        			} else if (value.equals(AudioFormat.Encoding.PCM_SIGNED.toString())) {
		        				encoding = AudioFormat.Encoding.PCM_SIGNED;
		        			} else if (value.equals(AudioFormat.Encoding.PCM_UNSIGNED.toString())) {
		        				encoding = AudioFormat.Encoding.PCM_UNSIGNED;
		        			} else {
		        				_logger.warn("Unsupported encoding: "+value);
		        			}
				        } else {
				        	_logger.warn("Unrecognized field "+name+ " = "+value);
				        }
			        } catch (Exception e) {
			        	_logger.warn("Exception " + e.getMessage() + "ooccured while processing field name= "+name +" with value= "+value);
			        }
			    } else {
			        _logger.debug("File field " + name + " with file name "
			            + item.getName() + " detected.");

			        
			        // Process the input stream
				    if (name.equals("audio") ) {
				    	audio = stream;
				    	try {
				    		_logger.debug("recognizing audio!  Sample rate= "+sampleRate+", bigEndian= "+ bigEndian+", bytes per value= "+bytesPerValue+", encoding= "+encoding.toString());
				    		_logger.debug("continuous: "+continuous+" lmflag: "+lmFlag+ " endpointing: "+doEndpointing);
				    		if (continuous) {
						        
								PrintWriter out = response.getWriter();	
				    			//OutputStream out = response.getOutputStream();
				    			response.setContentType("text/plain");
				    			//response.setHeader("Content-Disposition", "attachment; filename=results.txt'");			
				    			response.setHeader("Transfer-coding","chunked");
					    		if (lmFlag) {
					    			textResult = recognizerService.Transcribe(audio,contentType,sampleRate,bigEndian,bytesPerValue,encoding,out);
					    		} else {
							        _logger.debug("recognition result is null");
								    out.println("recognition result is null");
					    		}
				    		} else {

					    		if (lmFlag) {
					    			result = recognizerService.Recognize(audio,contentType,sampleRate,bigEndian,bytesPerValue,encoding,doEndpointing);
					    		} else {
					    	        result = recognizerService.Recognize(audio, grammarString,contentType,sampleRate,bigEndian,bytesPerValue,encoding,doEndpointing);

					    	    }

								PrintWriter out = response.getWriter();	
								if (result == null) {
									textResult = null;
									_logger.debug("recognition result is null");
									out.println("recognition result is null");
								}else {
									textResult = result.toString();
									_logger.debug(textResult);
									out.println(textResult);
								}
					    		
					    	    //String filename = Long.toString(System.currentTimeMillis()) + ".wav";
					    		//writeStreamToFile2(audio,filename);
				    		}
				    	} catch (Exception e) {
				    		e.printStackTrace();
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
			throw (IOException) e.getCause();
		} catch (FileUploadException e) {
			throw new ServletException(e.getMessage(), e);
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
