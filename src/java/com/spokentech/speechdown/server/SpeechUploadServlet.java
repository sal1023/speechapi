package com.spokentech.speechdown.server;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.fileupload.FileItem;
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


/**
 * Servlet for uploading audio for speech recognition processing.
 *
 * @author nielsgodfredsen
 */
@SuppressWarnings("serial")
public class SpeechUploadServlet extends HttpServlet {

	private static Logger _logger = Logger.getLogger(SpeechUploadServlet.class);
	/** */
	private DiskFileItemFactory factory;
	private File destinationDir;
	
	SynthesizerService synthesizerService;
	RecognizerService recognizerService;

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
	        _logger.info(key + " -- " + request.getHeader(key));
	      }

		
		// Check that we have a file upload request
		if (!ServletFileUpload.isMultipartContent(request)) {
			_logger.info("redirecting to / because not multipart content");
			response.sendRedirect(response.encodeRedirectURL("/")); // TODO: allow redirect location to be configured
			return;
		}

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(this.factory);

		int count = 0;
		InputStream audio = null;
		InputStream grammar = null;
		// Parse the request
		try {
			List<FileItem> items = upload.parseRequest(request);
			List<String> filenames = new LinkedList<String>();
			for (FileItem item : items) {
			    String name = item.getFieldName();
			    InputStream stream = item.getInputStream();
			    if (name.equals("audio") ) {
			    	audio = stream;
			    } else if (name.equals("grammar")) {
			    	grammar=stream;
			    }
	
			    _logger.info("Fileitem #"+(count++)+" FieldName:"+item.getFieldName()+ " content type: "+item.getContentType()+" item name: "+item.getName());
				if (item.isFormField()) {
					// TODO: log values
			        _logger.info("Form field " + name + " with value " + Streams.asString(stream) + " detected.");

				} else {

					//Write to a file
					/*
					String filename = Long.toString(System.currentTimeMillis()) + '.' + item.getName();					
					File file = new File(this.destinationDir, filename);
					try {
						item.write(file);
						filenames.add(filename);
					} catch (Exception e) {
						throw new ServletException(e.getMessage(), e);
					}
					*/
					
			
					
				}
			}
			//_logger.info(audio);
			//_logger.info(grammar);

			//Use the stream. Send it to the recognizer
			
			String grammarString = readInputStreamAsString(grammar);
			//AudioInputStream as = null;
            //try {
	        //    as = AudioSystem.getAudioInputStream(audio);
            //} catch (UnsupportedAudioFileException e1) {
	        //    // TODO Auto-generated catch block
	        //    e1.printStackTrace();
            //}
			
			RecognitionResult result = null;
			try {
			   result = recognizerService.Recognize(audio, grammarString,8000,false,2,AudioFormat.Encoding.PCM_SIGNED);

			}catch (Exception e) {
				e.printStackTrace();
			}
			
			_logger.info("recognition result: "+result.getText());
			
			/*
			try {
				request.getAudio().getInputStream();
				File f = new File("recog"+(counter++)+".wav");
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));

				BufferedInputStream in = new BufferedInputStream(request.getAudio().getInputStream());

				byte[] buffer = new byte[256]; 
				while (true) { 
					int bytesRead = in.read(buffer);
					//_logger.trace("Read "+ bytesRead + "bytes.");
					if (bytesRead == -1) break; 
					out.write(buffer, 0, bytesRead); 
				} 
				_logger.info("Closing streams");
				in.close(); 
				out.close(); 
			} 
			catch (Exception e) { 
				System.out.println("upload Exception"); e.printStackTrace(); 
				System.out.println(e); 
			} 
			*/
			
			PrintWriter out = response.getWriter();		
			out.println(result.toString());
			
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
		return buf.toString();
	}


}
