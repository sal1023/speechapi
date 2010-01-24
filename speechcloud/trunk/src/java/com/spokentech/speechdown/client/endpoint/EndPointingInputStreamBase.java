/**
 * 
 *
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.spokentech.speechdown.client.SpeechEventListenerDecorator;
import com.spokentech.speechdown.client.util.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;

// TODO: Auto-generated Javadoc
/**
 * The Class EndPointingInputStreamBase.  Abstract class implements common methods
 */
public abstract class EndPointingInputStreamBase implements EndPointingInputStream {
	


	private static Logger _logger = Logger.getLogger(EndPointingInputStreamBase.class.getName());
	
    private static int audioBufferSize = 160000;
    
	protected /*static*/ Timer _timer = new Timer();
	protected TimerTask _noInputTimeoutTask;
    
	protected SpeechEventListener _listener;

	protected volatile short _state = COMPLETE;
	
	//Setup a piped stream to get an input stream that can be used for feeding the chunk encoded post 
	protected PipedOutputStream	outputStream;
	protected PipedInputStream inputStream;
	
	protected EndPointer ep;;
	
	protected InputStream  stream;
	protected AFormat  format;

	private boolean busyFlag = false;
	
	
    public EndPointingInputStreamBase(EndPointer ep) {
	    super();
	    this.ep = ep;
		_logger.setLevel(Level.INFO);
	       
    }
	
	/**
	 * Setup piped stream.  The piped stream is used internally to readt the input stream andcheck for enpoints and pipe it back out to the user of the 
	 * endpointed audio stream
	 */
	public void setupPipedStream() {

		outputStream = new PipedOutputStream();
	    try {
	        //inputStream = new PipedInputStream(outputStream,audioBufferSize);
	        inputStream = new PipedInputStream(outputStream);
	    } catch (IOException e3) {
	        // TODO Auto-generated catch block
	        e3.printStackTrace();
	    }
	}
	
	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#getInputStream()
	 */
	public InputStream getInputStream() {
		return inputStream;
	}
	
	
	
	/* (non-Javadoc)
	 * @see com.spokentech.speechdown.client.endpoint.EndPointingInputStream#startAudioTransfer(long, com.spokentech.speechdown.client.SpeechEventListener)
	 */
	public void startAudioTransfer(long timeout, SpeechEventListener listener) throws InstantiationException, IOException {
		
		_listener = new Listener(listener);
		
		//setup the outputstream pipe
        setupPipedStream();
		
	    // start the endpointer thread
     	ep.start(stream, format, outputStream, _listener);

		if (timeout > 0)
			startInputTimers(timeout);
		
		_state = WAITING_FOR_SPEECH;
	}

	

	public synchronized boolean inUse() {
		return (busyFlag || ep.inUse());
	}

	public synchronized boolean checkAndSetIfInUse() {
		if ((!busyFlag || !ep.inUse())) {
		   return false;
		} else {
			busyFlag = true;
			return false;
		}
	}

    public synchronized void stopAudioTransfer() {
    	_logger.fine("Stopping stream");
    	if (ep != null) {
    		ep.stopRecording();
    	}
    	if (_timer !=null) 
    	   _timer.cancel();
		busyFlag = false;

    }
	
    /**
     * The Class NoInputTimeoutTask.
     */
    public class NoInputTimeoutTask extends TimerTask {

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            synchronized (EndPointingInputStreamBase.this) {
                _noInputTimeoutTask = null;
                if (_state == WAITING_FOR_SPEECH) {
                    _state = COMPLETE;
                    stopAudioTransfer();
                    if (_listener != null) {
                    	_listener.noInputTimeout();
                    }
                }
            }
        }
        
    }
    
    

   /**
   	 * The Class Listener.
   	 */
   	protected class Listener extends SpeechEventListenerDecorator {

	        /**
        	 * TODOC.
        	 * 
        	 * @param speechEventListener the speech event listener
        	 */
	        public Listener(SpeechEventListener speechEventListener) {
	            super(speechEventListener);
	        }

	        /* (non-Javadoc)
	         * @see org.speechforge.cairo.server.recog.RecogListener#speechStarted()
	         */
	        @Override
	        public void speechStarted() {

	            _logger.fine("speechStarted()");

	            synchronized (EndPointingInputStreamBase.this) {
	                if (_state == WAITING_FOR_SPEECH) {
	                    _state = SPEECH_IN_PROGRESS;
	                }
	                if (_noInputTimeoutTask != null) {
	                    _noInputTimeoutTask.cancel();
	                    _noInputTimeoutTask = null;
	                }
	            }
	            super.speechStarted();
	        }

	        /* (non-Javadoc)
        	 * @see com.spokentech.speechdown.client.SpeechEventListenerDecorator#speechEnded()
        	 */
        	public void speechEnded() {
	            _logger.fine("speechEnded()");

	            synchronized (EndPointingInputStreamBase.this) {

	            	_state = COMPLETE;
	            }
            	stopAudioTransfer();
	            super.speechEnded();
	        }

	        /* (non-Javadoc)
        	 * @see com.spokentech.speechdown.client.SpeechEventListenerDecorator#noInputTimeout()
        	 */
        	public void noInputTimeout() {
	            _logger.fine("no input timeout()");
	            synchronized (EndPointingInputStreamBase.this) {
	            	stopAudioTransfer();
	            	_state = COMPLETE;
	            }	   
	            super.noInputTimeout();
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
    	 * 
    	 * @return a double array, or <code>null</code> if byteArray is of zero length
    	 * 
    	 * @throws java.lang.ArrayIndexOutOfBoundsException     	 * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
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
    	 * 
    	 * @return a double array, or <code>null</code> if byteArray is of zero length
    	 * 
    	 * @throws java.lang.ArrayIndexOutOfBoundsException     	 * @throws ArrayIndexOutOfBoundsException the array index out of bounds exception
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
	    public static double rootMeanSquare(double[] samples) {
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
	    

	    public EndPointer getEndPointer() {
		    return ep;
	    }

	   
}