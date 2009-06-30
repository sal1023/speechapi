package com.spokentech.speechdown.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.cmu.sphinx.util.LogMath;

/**
 * CLI program that removes silences from an audio file
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@users.spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
public class SilenceCutter {
    private static Logger _logger = Logger.getLogger(SilenceCutter.class);
    public static final String CRLF = "\r\n";
    
    
    //public enum SilenceState {
    //    waitingForInviteResponse,
    //    inviteTimedOut,
    //    inviteResponseReceived}
    

    public static final String HELP_OPTION = "help";
    public static final String MAX_SILENCE = "s";
    public static final String THRESHOLD_FRAME = "f";
    public static final String SILENCE_THRESHOLD = "t";
    public static final String PLAY_AUDIO = "p";         
       
    private static boolean signed = true;

 	private static final int	EXTERNAL_BUFFER_SIZE = 128000;


    private static boolean play = false;
    
    private static int thresholdFrameSize =1;
    private static int maxSilence = 200;
    private static int silenceThreshold = 10;
    
    
    private static boolean inSpeechFlag = false;
    
    private static Options getOptions() {

        Options options = new Options();
        Option option = new Option(HELP_OPTION, "print this message");
        options.addOption(option);

        option = new Option(MAX_SILENCE, true, "The maximum length of silence (ms). Default="+maxSilence);
        options.addOption(option);
        
        option = new Option(THRESHOLD_FRAME, true, "The size of the threshold frame (ms). Default="+thresholdFrameSize);
        options.addOption(option);
        
        
        option = new Option(THRESHOLD_FRAME, true, "The size of the threshold frame (ms). Default="+thresholdFrameSize);
        options.addOption(option);

        option = new Option(SILENCE_THRESHOLD, true, "The threshold in counts. Default="+silenceThreshold);
        options.addOption(option);


        
        option = new Option(PLAY_AUDIO, false, "Flag that plays the audio to the local speaker while cutting the silence.");
        options.addOption(option);
        return options;
   }
    
    
    public static void main (String[] args) {

    	 AudioInputStream audioStream = null;
    	_logger.debug("Starting Remove Silence ...");

    	// setup a shutdown hook to cleanup and send a SIP bye message even if there is a 
    	// unexpected crash (ie ctrl-c)
    	Runtime.getRuntime().addShutdownHook(new Thread() {
    		public void run() {
    			_logger.debug("Caught the shutting down hook ...");
    		}
    	});


    	//get the command line args
    	CommandLine line = null;
    	Options options = getOptions();
    	try {
    		CommandLineParser parser = new GnuParser();
    		line = parser.parse(options, args, true);
    		args = line.getArgs();
    	} catch (ParseException e) {
    		e.printStackTrace();
    	}

    	//if (args.length != 2 || line.hasOption(HELP_OPTION)) {
    	if (line.hasOption(HELP_OPTION)) {
    		HelpFormatter formatter = new HelpFormatter();
    		formatter.printHelp("RemoveSilence [options] <input-file> <output-file>", options);
    		return;
    	}
    	
    	String inName = args[0];
    	String outName = args[1];


    	play = line.hasOption(PLAY_AUDIO);

    	String opt = null;
    	if (line.hasOption(MAX_SILENCE)) {
    		opt = line.getOptionValue(MAX_SILENCE);
    		_logger.debug("has silence count option: "+opt);
    		try {
    			maxSilence = Integer.parseInt(opt);
    		} catch (Exception e) {
    			_logger.debug(e, e);
    		}
    	}

    	if (line.hasOption(THRESHOLD_FRAME)) {
    		opt = line.getOptionValue(THRESHOLD_FRAME);
    		_logger.debug("has replace count option: "+opt);
    		try {
    			thresholdFrameSize = Integer.parseInt(opt);
    		} catch (Exception e) {
    			_logger.debug(e, e);
    		}
    	}

    	if (line.hasOption(SILENCE_THRESHOLD)) {
    		opt = line.getOptionValue(SILENCE_THRESHOLD);
    		_logger.debug("has replace count option: "+opt);
    		try {
    			silenceThreshold = Integer.parseInt(opt);
    		} catch (Exception e) {
    			_logger.debug(e, e);
    		}
    	}
    	
    	_logger.debug("max silence:" +maxSilence+" threshold frame size: "+thresholdFrameSize+ " silence threshold: "+silenceThreshold);
    	
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	DataOutputStream dos = new DataOutputStream(baos);


    	File soundFile = new File(inName);
    	File outWavFile = new File(outName);

    	//TODO: Assuming it is a wav file.  Should get the actual type of input file
    	AudioFileFormat.Type outputType = null;
    	AudioFileFormat.Type[] typesSupported = AudioSystem.getAudioFileTypes();
    	for (AudioFileFormat.Type aTypesSupported : typesSupported) {
    		if (aTypesSupported.getExtension().equals("wav")) {
    			outputType =  aTypesSupported;
    		}
    	}

    	// read in the sound file.
    	AudioInputStream	audioInputStream = null;
    	try {
    		audioInputStream = AudioSystem.getAudioInputStream(soundFile);
    	} catch (Exception e) {
    		e.printStackTrace();
    		System.exit(1);
    	}

    	AudioFormat	audioFormat = audioInputStream.getFormat();

    	SourceDataLine	sline = null;
    	DataLine.Info	info = new DataLine.Info(SourceDataLine.class, audioFormat);
		if (play) {
	    	try {
	    		sline = (SourceDataLine) AudioSystem.getLine(info);
	    		sline.open(audioFormat);
	
	    	} catch (LineUnavailableException e){
	    		e.printStackTrace();
	    		System.exit(1);
	    	}catch (Exception e){
	    		e.printStackTrace();
	    		System.exit(1);
	    	}
	
	    	sline.start();
		}
    
    	int	nBytesRead = 0;
    	byte[]	abData = new byte[EXTERNAL_BUFFER_SIZE];
        int  count = 0;
        
        
        float rate = audioFormat.getSampleRate();
		double threshold = (double)silenceThreshold  ;
		
        int bufferSize = (int)(((float) maxSilence)*rate/1000.0);
        int tFrameSize = (int)(((float) thresholdFrameSize)*rate/1000.0);
        
        _logger.debug("Silence buffer size: "+ bufferSize+ " threshold frame size: "+ tFrameSize);

        CircularDArrayBuffer tFrame = new CircularDArrayBuffer(tFrameSize);
        CircularDArrayBuffer cbuf = new CircularDArrayBuffer(bufferSize);
        boolean flag = false;
        long dropStart = 0;

    	while (nBytesRead != -1){  		

    		//read the data from the file
    		try{
    			nBytesRead = audioInputStream.read(abData, 0, abData.length);
    		}catch (IOException e){
    			e.printStackTrace();
    		}
    		//_logger.debug("Read "+ nBytesRead + " bytes");
    		
    		if (nBytesRead > 0) {
	    		//convert it to double data
	            double[] samples;
	            if (audioFormat.isBigEndian()) {
	                samples = bytesToValues
	                        (abData, 0, nBytesRead, audioFormat.getSampleSizeInBits()/8, signed);
	            } else {
	                samples = littleEndianBytesToValues
	                        (abData, 0, nBytesRead, audioFormat.getSampleSizeInBits()/8, signed);
	            }


	    		for (double d: samples) {
	    			_logger.trace(count + " : "+d);
	    			count++;
	    			tFrame.add(d);
	    			double[] tframe = tFrame.getAll();
	    			double rms = 0.0;
	    			if (tframe != null) {
	    				 rms = rootMeanSquare(tframe);
	    			} else {
	    				_logger.info("Got null for threshold frame.  so using the single current value: "+d);
	    				rms = d;
	    			}
	    			//if (rms == 0.0) {
	    			//   _logger.info(d+ " "+rms+tframe.length);
	    			//}


	    			if (rms <= threshold) {		//in Silence
    					cbuf.add(d);
	    				if (inSpeechFlag) {								//transitioned to silence
	    					//_logger.debug("tansitioned to silence "+ count+" "+rms+" "+threshold+" d "+d+" cbuf len"+cbuf.getLength());
	    					inSpeechFlag = false;	    					
	    				}
	    				
	    				//for console output showing what was removed
	    				if ((cbuf.getLength() == bufferSize)&& (flag == false)){
	    					flag = true;
	    					dropStart = count;
	    					//_logger.debug("dropping a silence data points starting at "+count);
	    				}
	    				
	    			} else {							// in speech
	    				if (inSpeechFlag) {				// still in speech
	    					// just write the current value
		                    try {
		                        dos.writeShort(new Short((short) d));
		                    } catch (IOException e) {
		                        e.printStackTrace();
		                    }

	    				} else {						// transitioned to speech
	    					//_logger.debug("tansitioned to speech "+ count+ " "+rms+" "+(double)silenceThreshold);
	    					inSpeechFlag = true;
    					
	    					// console out put showing what was removed
	    					if (flag) {
	    						flag = false;
	    						long dropped = count - dropStart;
	    						double dropDur = (double)dropped / rate;
	    						double dropt1 =  (double)(dropStart - cbuf.getLength()) /rate ;
	    						_logger.info("Dropping " + dropDur + "seconds starting at " +dropt1 + " seconds into recording");
	    					}

	    					//loop thru circular buffer and write to file.
	    					for (int i=0;i<cbuf.getLength();i++) {
	    						//TODO: check if I am missing one in this loop
	    						double val = cbuf.get();
			                    try {
			                        dos.writeShort(new Short((short) val));
			                    } catch (IOException e) {
			                        e.printStackTrace();
			                    }
	    					}
	    					//write this current value too
		                    try {
		                        dos.writeShort(new Short((short) d));
		                    } catch (IOException e) {
		                        e.printStackTrace();
		                    }
	    				}
	    			}
	
	    		}
	    		
	            
	            // write it to the speaker (for monitoring)
	    		if (play) {
	    			if (nBytesRead >= 0){
	    				int	nBytesWritten = sline.write(abData, 0, nBytesRead);
	    			}
	    		}
	    		
	    		//write it to the new file
	    		 byte[] outAudioData = baos.toByteArray();
	    	     ByteArrayInputStream bais = new ByteArrayInputStream(outAudioData);	    		
	    	    AudioFormat outFormat = new AudioFormat(audioFormat.getFrameRate(),audioFormat.getSampleSizeInBits(), 1, true, true);
	    		AudioInputStream ais = new AudioInputStream(bais, outFormat, outAudioData.length / audioFormat.getFrameSize());
	    		//_logger.debug(audioFormat.toString());
	    		
	    		if (AudioSystem.isFileTypeSupported(outputType, ais)) {
	    			try {
	    				AudioSystem.write(ais, outputType, outWavFile);
	    			} catch (IOException e) {
	    				e.printStackTrace();
	    			}
	    		} else {
	    			_logger.info("output type not supported..."); 
	    		}
    		} else
    			_logger.debug("read Non positove number of bytes "+nBytesRead);
    	}

    	//close the lines
		if (play) {
	    	sline.drain();
	    	sline.close();
		}
    	System.exit(0);
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
        double rootMeanSquare = Math.sqrt((double)sumOfSquares)/samples.length;
        //rootMeanSquare = Math.max(rootMeanSquare, 1);
        return (rootMeanSquare);
        //return (LogMath.log10((float)rootMeanSquare) * 20);
    }
    
}
