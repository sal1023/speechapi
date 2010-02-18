package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import org.apache.log4j.Logger;

import com.spokentech.speechdown.client.util.CircularDArrayBuffer;

/** This Thread records audio, and caches them in an audio buffer. */
public class AudioStreamEndPointer extends EndPointerBase {

	
	private static int CONSECUTIVE_LOWS_NEEDED_FOR_ENDSPEECH = 40;
	private static int CONSECUTIVE_HIGHS_NEEDED_FOR_STARTSPEECH = 20;
    private static boolean signed = true;

	private static Logger _logger = Logger.getLogger(AudioStreamEndPointer.class);
	
    private long totalSamplesRead = 0;


    
    protected int sampleRate;
    protected int bytesPerValue;
    protected boolean bigEndian;
    protected boolean signedData;

    private boolean utteranceEndSent = false;
    private boolean utteranceStarted = false;
    
	public AudioStreamEndPointer(int bufferSize) {
	    super(bufferSize);
    }
	
	   
	public AudioStreamEndPointer() {
	    super();
    }

    
    /**
     * Sets the InputStream from which this StreamDataSource reads.
     *
     * @param inputStream the InputStream from which audio data comes
     * @param streamName  the name of the InputStream
     */
    @Override 
    public void setInputStream(InputStream inputStream) {
        astream = inputStream;
        streamEndReached = false;
        utteranceEndSent = false;
        utteranceStarted = false;

        if (!(inputStream instanceof AudioInputStream)) 
            throw new RuntimeException("Not an  audio input stream");

        AudioFormat format = ((AudioInputStream) inputStream).getFormat();
        sampleRate = (int) format.getSampleRate();
        bigEndian = format.isBigEndian();

        String s = format.toString();
        _logger.debug("input format is " + s);

        if (format.getSampleSizeInBits() % 8 != 0)
            throw new Error("StreamDataSource: bits per sample must be a multiple of 8.");
        bytesPerValue = format.getSampleSizeInBits() / 8;

        // test wether all files in the stream have the same format

        AudioFormat.Encoding encoding = format.getEncoding();
        if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED))
            signedData = true;
        else if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED))
            signedData = false;
        else
            throw new RuntimeException("used file encoding is not supported");


    }
   

    public void doEndpointing() {
    	
    	// Read the next chunk of data from the TargetDataLine.
    	//byte[] data = new byte[frameSizeInBytes];

    	// TODO: intelligent size
    	//byte[]	abBuffer = new byte[65536];
    	//byte[] data = new byte[astream.getBufferSize() / 5];

    	AudioFormat	format = ((AudioInputStream)astream).getFormat();
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


        byte[] samplesBuffer = new byte[bytesToRead];
        
    	while ((!speechEnded) && (!streamEndReached)) {
    		_logger.debug("trying to read: " + samplesBuffer.length);
    		//int numBytesRead = astream.read(data, 0, data.length);
    		
            int read = 0;
            int totalRead = 0;
            long collectTime = System.currentTimeMillis();
            long totalValuesRead = 0;
            long firstSample = totalValuesRead;
            do {
                try {
	                read = astream.read(samplesBuffer, totalRead, bytesToRead - totalRead);
                } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                	streamEndReached = true;
                }
                if (read > 0) {
                    totalRead += read;
                }
            //} while (read != -1 && totalRead < bytesToRead);
            } while (read != -1 && totalRead < bytesToRead && (!streamEndReached));

            if (totalRead <= 0) {
                //closeInputDataStream();
                speechEnded = true;
            }
            // shrink incomplete frames
            totalValuesRead += (totalRead / bytesPerValue);
            if (totalRead < bytesToRead) {
                totalRead = (totalRead % 2 == 0)
                        ? totalRead + 2
                        : totalRead + 3;
                byte[] shrinkedBuffer = new byte[totalRead];
                System
                        .arraycopy(samplesBuffer, 0, shrinkedBuffer, 0,
                                totalRead);
                samplesBuffer = shrinkedBuffer;
                //closeInputDataStream();
            }
            
    		_logger.debug(" ...read: " + totalRead);
    		int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
    		totalSamplesRead += (totalRead / sampleSizeInBytes);

    		if (totalRead > 0) {
    			
    			//convert it to double data
    			double[] samples;
    			if (format.isBigEndian()) {
    				samples = bytesToValues
    				(samplesBuffer, 0, totalRead, format.getSampleSizeInBits()/8, signed);
    			} else {
    				samples = littleEndianBytesToValues
    				(samplesBuffer, 0, totalRead, format.getSampleSizeInBits()/8, signed);
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
    				_logger.debug("Writing "+totalRead + "bytes");
    				try {
    					ostream.write(samplesBuffer, 0, totalRead);
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
    			}
    		}
    	}

		_logger.debug("Done! "+ totalSamplesRead);
		
		// close the input stream
		//closeInputDataStream();
		
		// Close the output stream. 
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


	@Override
    public boolean requiresServerSideEndPointing() {
	    return false;
    }
}
