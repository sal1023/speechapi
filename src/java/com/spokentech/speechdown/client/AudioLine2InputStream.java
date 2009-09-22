package com.spokentech.speechdown.client;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;

import org.apache.log4j.Logger;

/** This Thread records audio, and caches them in an audio buffer. */
class AudioLine2InputStream extends Thread {
	
	private static Logger _logger = Logger.getLogger(AudioLine2InputStream.class);
	
    private static int audioBufferSize = 16000;

    private TargetDataLine aline;
    private PipedOutputStream ostream;
    private PipedInputStream istream;

    /**
     * @return the istream
     */
    public PipedInputStream getIstream() {
    	return istream;
    }



	/**
     * Creates the thread with the given name
     *
     * @param name the name of the thread
     */
    public AudioLine2InputStream(String name, TargetDataLine audioLine) {
        super(name);
        aline = audioLine;

		//Setup a piped stream to get an input stream that can be used for feeding the chunk encoded post 
        ostream = new PipedOutputStream();
    	
        try {
	        istream = new PipedInputStream(ostream,audioBufferSize);
        } catch (IOException e3) {
	        // TODO Auto-generated catch block
	        e3.printStackTrace();
        }

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

    	byte[] data = new byte[aline.getBufferSize() / 5];
    	AudioFormat	format = aline.getFormat();
    	boolean speechEnded = false;
    	int totalSamplesRead = 0;
    	    	
		while (!speechEnded) {
    		_logger.debug("trying to read: " + data.length);
    		int numBytesRead = aline.read(data, 0, data.length);
    		_logger.debug(" ...read: " + numBytesRead);
    		int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
    		totalSamplesRead += (numBytesRead / sampleSizeInBytes);

    		if (numBytesRead > 0) {

				_logger.debug("Writing "+numBytesRead + "bytes");
				try {
					ostream.write(data, 0, numBytesRead);
				} catch (IOException e) {
					e.printStackTrace();
				}
    			
    		}
    	}

		_logger.debug("Done! "+ totalSamplesRead);

		try {
			ostream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 	
    }
    

  
}
