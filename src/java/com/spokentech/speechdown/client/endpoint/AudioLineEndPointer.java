/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.client.util.CircularDArrayBuffer;
import com.spokentech.speechdown.common.SpeechEventListener;



/** This Thread records audio, and caches them in an audio buffer. */
@Deprecated 
class AudioLineEndPointer implements Runnable {
	
	private static int CONSECUTIVE_LOWS_NEEDED_FOR_ENDSPEECH = 40;
	private static int CONSECUTIVE_HIGHS_NEEDED_FOR_STARTSPEECH = 20;
    private static boolean signed = true;

	private static Logger _logger = Logger.getLogger(AudioLineEndPointer.class);
	
    private long totalSamplesRead = 0;

    private TargetDataLine aline;
    private OutputStream ostream;
    private SpeechEventListener listener;

	private Thread t;




    /** Starts the thread, and waits for recorder to be ready */
    public void start(TargetDataLine audioLine, OutputStream outputStream, SpeechEventListener listener) {
        aline = audioLine;
        ostream = outputStream;
        this.listener = listener;
		aline.start();
    	t=new Thread(this);
		t.start();
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
    	double startThreshold = 200.0;
       	double endThreshold = 50.0;
    	int thresholdFrameSize = 20; //in ms
    	int tFrameSize = (int)(((float) thresholdFrameSize)*format.getSampleRate()/1000.0);
    	CircularDArrayBuffer tFrame = new CircularDArrayBuffer(tFrameSize);
    	long count=0;
    	double startTime =0.0;
    	double stopTime = 0.0;
    	boolean speechStarted = false;
    	boolean speechEnded = false;
    	int endCount = 0;
       	int startCount = 0;

    	while (!speechEnded) {
    		_logger.debug("trying to read: " + data.length);
    		int numBytesRead = aline.read(data, 0, data.length);
    		_logger.debug(" ...read: " + numBytesRead);
    		int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
    		totalSamplesRead += (numBytesRead / sampleSizeInBytes);

    		if (numBytesRead > 0) {
    			
    			//convert it to double data
    			double[] samples;
    			if (format.isBigEndian()) {
    				samples = bytesToValues
    				(data, 0, numBytesRead, format.getSampleSizeInBits()/8, signed);
    			} else {
    				samples = littleEndianBytesToValues
    				(data, 0, numBytesRead, format.getSampleSizeInBits()/8, signed);
    			}

    			//loop thru and look for threshold crossings
    			//threshold frames, are a sliding window of data samples.  rms is compared to threshold
    			_logger.debug("Looping thru the double array with "+samples.length + " samples");
    			for (double d: samples) {
    				count++;
    				tFrame.add(d);
    				double[] tframe = tFrame.getAll();
    				double rms = 0.0;
    				if (tframe != null) {
    					rms = rootMeanSquare(tframe);
    				} else {
    					_logger.debug("Got null for threshold frame.  so using the single current value: "+d);
    					rms = d;
    				}
    				//_logger.debug(count+" "+rms+" "+d);
    				if (!speechStarted) {								// no speech yet so check for the start
    					if (rms > startThreshold) {
    						startCount = startCount+1;
    						if (startCount > CONSECUTIVE_HIGHS_NEEDED_FOR_STARTSPEECH) {
    						    speechStarted = true;
    						    listener.speechStarted();
    						    startTime = ((double)count) / format.getSampleRate();
    						    _logger.debug("Speech started at count: "+count+ " ("+startTime+" secs)");
    						}
    					} else {
    						startCount = 0;
    					}
    				} else {											// speech started so check for end
    					if (!speechEnded) {
    						_logger.trace(count+" "+rms);
        					if (rms <= endThreshold) {
        						endCount = endCount+1;
        						if (endCount > CONSECUTIVE_LOWS_NEEDED_FOR_ENDSPEECH) {
        						    speechEnded = true;
        						    listener.speechEnded();
        						    stopTime = ((double)count) / format.getSampleRate();
        						    double endTime = stopTime-startTime;
        						    _logger.debug("Speech stopped at count: "+count+ " ("+stopTime+" secs) " + endTime);
        						} 
        					} else {
        						endCount = 0;
        					}
    					}
    				}

    			}
    			//_logger.debug(speechStarted+" "+speechEnded);
    			
    			if (speechStarted) {
    				//always write the entire buffer (even if we find the end we can send a little extra back to the recognizer or if we found the start inside this
    				// buffer a little silence in the beginning should not hurt)
    				_logger.debug("Writing "+numBytesRead + "bytes");
    				try {
    					ostream.write(data, 0, numBytesRead);
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
    			}
    		}
    	}

		_logger.debug("Done! "+ totalSamplesRead);
		/* We close the output stream.
		 */
		try {
			ostream.close();
		} catch (IOException e) {
			e.printStackTrace();
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

    /**
     * Returns the logarithm base 10 of the root mean square of the
     * given samples.
     *
     * @param samples the samples
     *
     * @return the calculated log root mean square in log 10
     */
    private static double rootMeanSquare(double[] samples) {
        assert samples.length > 0;
        double sumOfSquares = 0.0f;
        double sample = 0.0;
        for (int i = 0; i < samples.length; i++) {
            sample = samples[i];
            sumOfSquares += sample * sample;
            
        }
        double rootMeanSquare = Math.sqrt((double)sumOfSquares/samples.length);
        //rootMeanSquare = Math.max(rootMeanSquare, 1);
        return (rootMeanSquare);
        //return (LogMath.log10((float)rootMeanSquare) * 20);
    }
}
