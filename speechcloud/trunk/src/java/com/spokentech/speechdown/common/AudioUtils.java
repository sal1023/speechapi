/*
 * NetworkSpeech CLI - Lightweight Command Line Interface Programs   
 * 
 * Copyright (C) 2009-2010 SpokenTech - http://www.spokentech.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307, USA.
 *
 * Contact: salord@users.sourceforge.net
 *
 */

package com.spokentech.speechdown.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;


/**
 * The Class AudioUtils.
 */
public class AudioUtils {
	

	private static Logger _logger = Logger.getLogger(AudioUtils.class);
    
	
	/**
	 * Read sampled audio data from the specified stream and play it.
	 * 
	 * @param ain the ain
	 * 
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws UnsupportedAudioFileException the unsupported audio file exception
	 * @throws LineUnavailableException the line unavailable exception
	 */
    public  static void streamAudioToSpeaker(AudioInputStream ain)
        throws IOException, UnsupportedAudioFileException,
               LineUnavailableException
    {
        SourceDataLine line = null;   // And write it here.

        try {
     
            // Get information about the format of the stream
            AudioFormat format = ain.getFormat( );
            DataLine.Info info=new DataLine.Info(SourceDataLine.class,format);

            // If the format is not supported directly (i.e. if it is not PCM
            // encoded), then try to transcode it to PCM.
            if (!AudioSystem.isLineSupported(info)) {
                // This is the PCM format we want to transcode to.
                // The parameters here are audio format details that you
                // shouldn't need to understand for casual use.
                AudioFormat pcm =
                    new AudioFormat(format.getSampleRate( ), 16,
                                    format.getChannels( ), true, false);

                // Get a wrapper stream around the input stream that does the
                // transcoding for us.
                ain = AudioSystem.getAudioInputStream(pcm, ain);

                // Update the format and info variables for the transcoded data
                format = ain.getFormat( ); 
                info = new DataLine.Info(SourceDataLine.class, format);
            }

            // Open the line through which we'll play the streaming audio.
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);  

            // Allocate a buffer for reading from the input stream and writing
            // to the line.  Make it large enough to hold 4k audio frames.
            // Note that the SourceDataLine also has its own internal buffer.
            int framesize = format.getFrameSize( );
            byte[  ] buffer = new byte[4 * 1024 * framesize]; // the buffer
            int numbytes = 0;                               // how many bytes

            // We haven't started the line yet.
            boolean started = false;

            for(;;) {  // We'll exit the loop when we reach the end of stream
                // First, read some bytes from the input stream.
                int bytesread=ain.read(buffer,numbytes,buffer.length-numbytes);
                // If there were no more bytes to read, we're done.
                if (bytesread == -1) break;
                numbytes += bytesread;
                
                // Now that we've got some audio data to write to the line,
                // start the line, so it will play that data as we write it.
                if (!started) {
                    line.start( );
                    started = true;
                }
                
                // We must write bytes to the line in an integer multiple of
                // the framesize.  So figure out how many bytes we'll write.
                int bytestowrite = (numbytes/framesize)*framesize;
                
                // Now write the bytes. The line will buffer them and play
                // them. This call will block until all bytes are written.
                line.write(buffer, 0, bytestowrite);
                
                // If we didn't have an integer multiple of the frame size, 
                // then copy the remaining bytes to the start of the buffer.
                int remaining = numbytes - bytestowrite;
                if (remaining > 0)
                    System.arraycopy(buffer,bytestowrite,buffer,0,remaining);
                numbytes = remaining;
            }

            // Now block until all buffered sound finishes playing.
            line.drain( );
        }
        finally { // Always relinquish the resources we use
            if (line != null) line.close( );
            if (ain != null) ain.close( );
        }
    }


    /**
     * Stream audio to speaker.
     * 
     * @param url the url
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws UnsupportedAudioFileException the unsupported audio file exception
     * @throws LineUnavailableException the line unavailable exception
     */
    public static void streamAudioToSpeaker(URL url) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
    	streamAudioToSpeaker(AudioSystem.getAudioInputStream(url));
    }

	/**
	 * Write stream to file.
	 * 
	 * @param inStream the in stream
	 * @param fileName the file name
	 */
	public static void writeStreamToFile(InputStream inStream, String fileName) {
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
	
	/**
	 * Write stream to standar out.
	 * 
	 * @param inStream the in stream
	 */
	public static void writeStreamToStandarOut(InputStream inStream) {
		try {

			BufferedOutputStream out = new BufferedOutputStream(System.out);
	
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
