package com.spokentech.speechdown.server.tts;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.Locale;
import java.lang.reflect.Method;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFileFormat.Type;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.server.SynthesizerService;
import com.spokentech.speechdown.server.domain.HttpRequest;
import com.spokentech.speechdown.server.domain.SynthRequest;
import com.spokentech.speechdown.server.util.ServiceLogger;

import de.dfki.lt.mary.Mary;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.Request;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.util.MaryAudioUtils;

public class MarySynthesizerService implements SynthesizerService {
	
    static Logger _logger = Logger.getLogger(MarySynthesizerService.class);
	private File promptDir;
	Mary mary;
	String prefix;
	String maryDir;
	private boolean recordingEnabled;
  
	/**
     * @return the recordingEnabled
     */
    public boolean isRecordingEnabled() {
    	return recordingEnabled;
    }

	/**
     * @param recordingEnabled the recordingEnabled to set
     */
    public void setRecordingEnabled(boolean recordingEnabled) {
    	this.recordingEnabled = recordingEnabled;
    }

	private String recordingFilePath;

	/**
     * @return the maryDir
     */
    public String getMaryDir() {
    	return maryDir;
    }

	/**
     * @param maryDir the maryDir to set
     */
    public void setMaryDir(String maryDir) {
    	this.maryDir = maryDir;
    	System.setProperty("mary.base", maryDir);
    }

	/**
     * @return the promptDir
     */
    public File getPromptDir() {
    	return promptDir;
    }

	/**
     * @param promptDir the promptDir to set
     */
    public void setPromptDir(File promptDir) {
    	this.promptDir = promptDir;
    }

    
	/**
     * @return the prefix
     */
    public String getPrefix() {

		return prefix;
    }

	/**
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
    	this.prefix = prefix;
    }


    private  void addJarsToClasspath() throws Exception {
    	//File jarDir = new File(MaryProperties.maryBase()+"\\java");
    	_logger.info("mary base:"+maryDir+"     "+maryDir+"\\java");
    	File jarDir = new File(maryDir+"/java");
    	File[] jarFiles = jarDir.listFiles(new FilenameFilter() {
    		public boolean accept(File dir, String name) {
    			return name.endsWith(".jar");
    		}
    	});
    	_logger.info("jarfiles: "+jarFiles);
    	_logger.info("# jarfiles: "+jarFiles.length);
    	assert jarFiles != null;
    	URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
    	Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
    	method.setAccessible(true);

    	for (int i=0; i<jarFiles.length; i++) {
    		URL jarURL = new URL("file:"+jarFiles[i].getPath());
    		method.invoke(sysloader, new Object[] {jarURL});
    	}
    }


	public void startup() {

		try {
			addJarsToClasspath();
	        MaryProperties.readProperties();
			//mary = new Mary();
	        Mary.startup();
        } catch (Exception e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } 
    }
	
	public void shutdown() {
		
	}



	public void streamTTS(String text, AudioFormat format,String mime, String voiceName, OutputStream out,HttpRequest hr) throws UnsupportedAudioFileException {
    	

        String  inputTypeName = "TEXT_EN";
        String  outputTypeName = "AUDIO";

        long  start = System.nanoTime();

        MaryDataType inputType = MaryDataType.get(inputTypeName);
        MaryDataType outputType = MaryDataType.get(outputTypeName);
        
        Voice voice = null;

        if (voiceName != null)
            voice = Voice.getVoice(voiceName);
        else if (inputType.getLocale() != null)
            voice = Voice.getDefaultVoice(inputType.getLocale());
        
        String audioTypeName = "WAVE";
		if (mime.equals("audio/x-wav")) {
			audioTypeName = "WAVE"; 
       	} else if (mime.equals("audio/x-au")) {
       		audioTypeName = "AU";
       } else if (mime.equals("audio/mpeg")) {
    	   audioTypeName = "MP3";
       	} else {
       		 _logger.warn("Unsupported fileformat: "+mime);
       	}


        AudioFileFormat audioFileFormat = null;
        if (outputType.equals(MaryDataType.get("AUDIO"))) {

            AudioFileFormat.Type audioType = MaryAudioUtils.getAudioFileFormatType(audioTypeName);
            
            //AudioFormat audioFormat = null;
            if (audioType.toString().equals("MP3")) {
                if (!MaryAudioUtils.canCreateMP3())
                    throw new UnsupportedAudioFileException("Conversion to MP3 not supported.");
                //audioFormat = MaryAudioUtils.getMP3AudioFormat();
            //} else {
                //Voice ref = (voice != null) ? voice : Voice.getDefaultVoice(Locale.ENGLISH);
                //audioFormat = ref.dbAudioFormat();
            }
            _logger.debug("***: "+format.toString());
            audioFileFormat = new AudioFileFormat(audioType, format, AudioSystem.NOT_SPECIFIED);
        }
        Request request = new Request(inputType, outputType, voice, "", "", 1, audioFileFormat);
 
        try {
        	ByteArrayInputStream bs = new ByteArrayInputStream(text.getBytes());
	        request.readInputData( new InputStreamReader(bs, "UTF-8"));
	        request.process();
	        request.writeOutputData(out);



			if (recordingEnabled) {
		        float sr = request.getAudioFileFormat().getFormat().getSampleRate();
		        //TODO: streamlen is always coming out to be zero.
				float flen = request.getAudioFileFormat().getByteLength();
				//float flen = request.getAudio().getFrameLen();
				float sampleSize = request.getAudioFileFormat().getFormat().getSampleSizeInBits()/8;
				int streamLen = (int) (flen/(sr * sampleSize));
				
				long stop = System.nanoTime();
				long wall = (stop - start)/1000000;
				double ratio = (double)wall/(double)streamLen;
				
			    hr.getSynth().setStreamLen(streamLen);
			    hr.getSynth().setWallTime(wall);
				ServiceLogger.logHttpRequest(hr);

			}
			
			
	        ///ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        // The byte array constitutes a full wave file, including the headers.
	        // And now, play the audio data:
	        ///request.writeOutputData(baos);

	        ///AudioInputStream ais = AudioSystem.getAudioInputStream(
	        ///    new ByteArrayInputStream(baos.toByteArray()));
	        ///writeStreamToFile(ais,"c:/tmp/sal.mp3"); 
	        ///AudioFormat x = request.getAudioFileFormat().getFormat();

	        ///_logger.info("****: "+x);
	        
        } catch (FileNotFoundException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (Exception e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        
    	
    }

	@Override
    public File ttsFile(String text, AudioFormat format, String mime) {
	    // TODO Auto-generated method stub
	    _logger.warn("TTS File method not implemented yet");
	    return null;
    }

	@Override
    public String ttsURL(String text, AudioFormat format, String mime) {

	    _logger.warn("TTS URL method not implemented yet");
	    return null;
    }
    
	private void writeStreamToFile(InputStream inStream, String fileName) {
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
			_logger.info("Closing streams");
			in.close(); 
			out.close(); 
		} 
		catch (Exception e) { 
			_logger.warn("upload Exception"); e.printStackTrace(); 
			e.printStackTrace();
		} 
	}
    
}
