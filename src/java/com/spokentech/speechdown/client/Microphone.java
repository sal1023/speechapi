/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.client;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.frontend.util.Utterance;
import edu.cmu.sphinx.util.props.*;

import javax.sound.sampled.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p/> A Microphone captures audio data from the system's underlying audio input systems. Converts these audio data
 * into Data objects. When the method <code>startRecording()</code> is called, a new thread will be created and used to
 * capture audio, and will stop when <code>stopRecording()</code> is called. Calling <code>getData()</code> returns the
 * captured audio data as Data objects. </p> <p/> This Microphone will attempt to obtain an audio device with the format
 * specified in the configuration. If such a device with that format cannot be obtained, it will try to obtain a device
 * with an audio format that has a higher sample rate than the configured sample rate, while the other parameters of the
 * format (i.e., sample size, endianness, sign, and channel) remain the same. If, again, no such device can be obtained,
 * it flags an error, and a call <code>startRecording</code> returns false. </p>
 */
public class Microphone extends BaseDataProcessor {

	private static Log logger =  LogFactory.getLog(Microphone.class.getName());

    /** SphinxProperty for the sample rate of the data. */
    @S4Integer(defaultValue = 16000)
    public static final String PROP_SAMPLE_RATE = "sampleRate";

    /**
     * Sphinx property that specifies whether or not the microphone will release the audio between utterances.  On
     * certain systems (linux for one), closing and reopening the audio does not work too well. The default is false for
     * Linux systems, true for others.
     */
    @S4Boolean(defaultValue = true)
    public final static String PROP_CLOSE_BETWEEN_UTTERANCES = "closeBetweenUtterances";

    /**
     * The Sphinx property that specifies the number of milliseconds of audio data to read each time from the underlying
     * Java Sound audio device.
     */
    @S4Integer(defaultValue = 10)
    public final static String PROP_MSEC_PER_READ = "msecPerRead";

    /** SphinxProperty for the number of bits per value. */
    @S4Integer(defaultValue = 16)
    public static final String PROP_BITS_PER_SAMPLE = "bitsPerSample";

    /** Property specifying the number of channels. */
    @S4Integer(defaultValue = 1)
    public static final String PROP_CHANNELS = "channels";

    /** Property specify the endianness of the data. */
    @S4Boolean(defaultValue = true)
    public static final String PROP_BIG_ENDIAN = "bigEndian";

    /** Property specify whether the data is signed. */
    @S4Boolean(defaultValue = true)
    public static final String PROP_SIGNED = "signed";

    /**
     * The Sphinx property that specifies whether to keep the audio data of an utterance around until the next utterance
     * is recorded.
     */
    @S4Boolean(defaultValue = false)
    public final static String PROP_KEEP_LAST_AUDIO = "keepLastAudio";

    /**
     * The Sphinx property that specifies how to convert stereo audio to mono. Currently, the possible values are
     * "average", which averages the samples from at each channel, or "selectChannel", which chooses audio only from
     * that channel. If you choose "selectChannel", you should also specify which channel to use with the
     * "selectChannel" property.
     */
    @S4String(defaultValue = "average", range = {"average", "selectChannel"})
    public final static String PROP_STEREO_TO_MONO = "stereoToMono";

    /** The Sphinx property that specifies the channel to use if the audio is stereo */
    @S4Integer(defaultValue = 0)
    public final static String PROP_SELECT_CHANNEL = "selectChannel";

    /**
     * The Sphinx property that specifies the mixer to use.  The value can be "default," (which means let the
     * AudioSystem decide), "last," (which means select the last Mixer supported by the AudioSystem), which appears to
     * be what is often used for USB headsets, or an integer value which represents the index of the Mixer.Info that is
     * returned by AudioSystem.getMixerInfo(). To get the list of Mixer.Info objects, run the AudioTool application with
     * a command line argument of "-dumpMixers".
     *
     * @see edu.cmu.sphinx.tools.audio.AudioTool
     */
    @S4String(defaultValue = "default")
    public final static String PROP_SELECT_MIXER = "selectMixer";


    private AudioFormat finalFormat;
    private AudioInputStream audioStream = null;
    private TargetDataLine audioLine = null;
    private DataList audioList;
    private Utterance currentUtterance;
    private boolean doConversion = false;
    private int audioBufferSize = 160000;
    private volatile boolean recording = false;
    private volatile boolean utteranceEndReached = true;
    private RecordingThread recorder;

    // Configuration data

    private AudioFormat desiredFormat;
    private boolean closeBetweenUtterances;
    private boolean keepDataReference;
    private boolean signed;
    private boolean bigEndian;
    private int frameSizeInBytes;
    private int msecPerRead;
    private int selectedChannel;
    private String selectedMixerIndex;
    private String stereoToMono;
    private int sampleRate;


    public Microphone(AudioFormat desiredFormat) {
	    super();
	    this.desiredFormat = desiredFormat;
        closeBetweenUtterances = true;
        msecPerRead = 10;
        keepDataReference = false;
        stereoToMono = "average";
        selectedChannel = 0;
        selectedMixerIndex = "default";
        sampleRate = (int) desiredFormat.getSampleRate();
    }
    
    
    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        //logger = ps.getLogger();

        sampleRate = ps.getInt(PROP_SAMPLE_RATE);

        int sampleSizeInBits = ps.getInt(PROP_BITS_PER_SAMPLE);

        int channels = ps.getInt(PROP_CHANNELS);
        bigEndian = ps.getBoolean(PROP_BIG_ENDIAN);
        signed = ps.getBoolean(PROP_SIGNED);

        desiredFormat = new AudioFormat
                ((float) sampleRate, sampleSizeInBits, channels, signed, bigEndian);

        closeBetweenUtterances = ps.getBoolean(PROP_CLOSE_BETWEEN_UTTERANCES);
        msecPerRead = ps.getInt(PROP_MSEC_PER_READ);
        keepDataReference = ps.getBoolean(PROP_KEEP_LAST_AUDIO);
        stereoToMono = ps.getString(PROP_STEREO_TO_MONO);
        selectedChannel = ps.getInt(PROP_SELECT_CHANNEL);
        selectedMixerIndex = ps.getString(PROP_SELECT_MIXER);
    }


    /**
     * Constructs a Microphone with the given InputStream.
     *
     * @throws IOException if an I/O error occurs
     */
    public void initialize() {
        super.initialize();
        audioList = new DataList();

        DataLine.Info info
                = new DataLine.Info(TargetDataLine.class, desiredFormat);

        /* If we cannot get an audio line that matches the desired
         * characteristics, shoot for one that matches almost
         * everything we want, but has a higher sample rate.
         */
        if (!AudioSystem.isLineSupported(info)) {
        	logger.debug(desiredFormat + " not supported");
            AudioFormat nativeFormat
                    = DataUtil.getNativeAudioFormat(desiredFormat,
                    getSelectedMixer());
            if (nativeFormat == null) {
            	 logger.debug("couldn't find suitable target audio format");
            } else {
                finalFormat = nativeFormat;

                /* convert from native to the desired format if supported */
                doConversion = AudioSystem.isConversionSupported
                        (desiredFormat, nativeFormat);

                if (doConversion) {
                	 logger.debug
                            ("Converting from " + finalFormat.getSampleRate()
                                    + "Hz to " + desiredFormat.getSampleRate() + "Hz");
                } else {
                	 logger.debug
                            ("Using native format: Cannot convert from " +
                                    finalFormat.getSampleRate() + "Hz to " +
                                    desiredFormat.getSampleRate() + "Hz");
                }
            }
        } else {
        	 logger.debug("Desired format: " + desiredFormat + " supported.");
            finalFormat = desiredFormat;
        }
    }




	/**
     * Gets the Mixer to use.  Depends upon selectedMixerIndex being defined.
     *
     * @see #newProperties
     */
    private Mixer getSelectedMixer() {
        if (selectedMixerIndex.equals("default")) {
            return null;
        } else {
            Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
            if (selectedMixerIndex.equals("last")) {
                return AudioSystem.getMixer(mixerInfo[mixerInfo.length - 1]);
            } else {
                int index = Integer.parseInt(selectedMixerIndex);
                return AudioSystem.getMixer(mixerInfo[index]);
            }
        }
    }


    /** Creates the audioLine if necessary and returns it. */
    private TargetDataLine getAudioLine() {
    	 logger.debug("Entering get audio line");
        if (audioLine != null) {
            return audioLine;
        }

        /* Obtain and open the line and stream.
        */
        try {
            /* The finalFormat was decided in the initialize() method
             * and is based upon the capabilities of the underlying
             * audio system.  The final format will have all the
             * desired audio characteristics, but may have a sample
             * rate that is higher than desired.  The idea here is
             * that we'll let the processors in the front end (e.g.,
             * the FFT) handle some form of downsampling for us.
             */
        	 logger.debug("Final format: " + finalFormat);

            DataLine.Info info = new DataLine.Info(TargetDataLine.class,
                    finalFormat);

            /* We either get the audio from the AudioSystem (our
             * default choice), or use a specific Mixer if the
             * selectedMixerIndex property has been set.
             */
            Mixer selectedMixer = getSelectedMixer();
            if (selectedMixer == null) {
                audioLine = (TargetDataLine) AudioSystem.getLine(info);
            } else {
                audioLine = (TargetDataLine) selectedMixer.getLine(info);
            }

            /* Add a line listener that just traces
             * the line states.
             */
            audioLine.addLineListener(new LineListener() {
                public void update(LineEvent event) {
                	 logger.debug("line listener " + event);
                }
            });
        } catch (LineUnavailableException e) {
            logger.info("microphone unavailable " + e.getMessage());
        }

        return audioLine;
    }


    /**
     * Opens the audio capturing device so that it will be ready for capturing audio. Attempts to create a converter if
     * the requested audio format is not directly available.
     *
     * @return true if the audio capturing device is opened successfully; false otherwise
     */
    private boolean open() {
        TargetDataLine audioLine = getAudioLine();
        if (audioLine != null) {
            if (!audioLine.isOpen()) {
            	 logger.debug("open");
                try {
                    audioLine.open(finalFormat, audioBufferSize);
                } catch (LineUnavailableException e) {
                    logger.warn("Can't open microphone " + e.getMessage());
                    return false;
                }

                audioStream = new AudioInputStream(audioLine);
                if (doConversion) {
                    audioStream = AudioSystem.getAudioInputStream
                            (desiredFormat, audioStream);
                    assert (audioStream != null);
                }

                /* Set the frame size depending on the sample rate.
                 */
                float sec = ((float) msecPerRead) / 1000.f;
                frameSizeInBytes =
                        (audioStream.getFormat().getSampleSizeInBits() / 8) *
                                (int) (sec * audioStream.getFormat().getSampleRate());

                logger.debug("Frame size: " + frameSizeInBytes + " bytes");
            }
            return true;
        } else {
            logger.warn("Can't find microphone");
            return false;
        }
    }


    /**
     * Returns the format of the audio recorded by this Microphone. Note that this might be different from the
     * configured format.
     *
     * @return the current AudioFormat
     */
    public AudioFormat getAudioFormat() {
        return finalFormat;
    }


    /**
     * Returns the current Utterance.
     *
     * @return the current Utterance
     */
    public Utterance getUtterance() {
        return currentUtterance;
    }


    /**
     * Returns true if this Microphone is recording.
     *
     * @return true if this Microphone is recording, false otherwise
     */
    public boolean isRecording() {
        return recording;
    }


    /**
     * Starts recording audio. This method will return only when a START event is received, meaning that this Microphone
     * has started capturing audio.
     *
     * @return true if the recording started successfully; false otherwise
     */
    public synchronized boolean startRecording() {
        if (recording) {
            return false;
        }
        if (!open()) {
            return false;
        }
        utteranceEndReached = false;
        if (audioLine.isRunning()) {
            logger.warn("Whoops: audio line is running");
        }
        assert (recorder == null);
        recorder = new RecordingThread("Microphone");
        recorder.start();
        recording = true;
        return true;
    }


    /**
     * Stops recording audio. This method does not return until recording has been stopped and all data has been read
     * from the audio line.
     */
    public synchronized void stopRecording() {
        if (audioLine != null) {
            if (recorder != null) {
                recorder.stopRecording();
                recorder = null;
            }
            recording = false;
        }
    }


    /** This Thread records audio, and caches them in an audio buffer. */
    class RecordingThread extends Thread {

        private boolean done = false;
        private volatile boolean started = false;
        private long totalSamplesRead = 0;
        private final Object lock = new Object();


        /**
         * Creates the thread with the given name
         *
         * @param name the name of the thread
         */
        public RecordingThread(String name) {
            super(name);
        }


        /** Starts the thread, and waits for recorder to be ready */
        public void start() {
            started = false;
            super.start();
            waitForStart();
        }


        /**
         * Stops the thread. This method does not return until recording has actually stopped, and all the data has been
         * read from the audio line.
         */
        public void stopRecording() {
            audioLine.stop();
            try {
                synchronized (lock) {
                    while (!done) {
                        lock.wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // flush can not be called here because the audio-line might has been set to  null already by the mic-thread 
//    	    audioLine.flush();
        }


        /** Implements the run() method of the Thread class. Records audio, and cache them in the audio buffer. */
        public void run() {
            totalSamplesRead = 0;
            logger.debug("started recording");

            if (keepDataReference) {
                currentUtterance = new Utterance
                        ("Microphone", audioStream.getFormat());
            }

            audioList.add(new DataStartSignal(sampleRate));
            logger.debug("DataStartSignal added");
            try {
                audioLine.start();
                while (!done) {
                    Data data = readData(currentUtterance);
                	//logger.debug("->read data ");
                    if (data == null) {
                    	logger.debug("->data null");
                        done = true;
                        break;
                    }
                    audioList.add(data);
                }
                audioLine.flush();
                if (closeBetweenUtterances) {
                    /* Closing the audio stream *should* (we think)
                     * also close the audio line, but it doesn't
                     * appear to do this on the Mac.  In addition,
                     * once the audio line is closed, re-opening it
                     * on the Mac causes some issues.  The Java sound
                     * spec is also kind of ambiguous about whether a
                     * closed line can be re-opened.  So...we'll go
                     * for the conservative route and never attempt
                     * to re-open a closed line.
                     */
                    audioStream.close();
                    audioLine.close();
                    System.err.println("set to null");
                    audioLine = null;
                }
            } catch (IOException ioe) {
                logger.warn("IO Exception " + ioe.getMessage());
                ioe.printStackTrace();
            }
            long duration = (long)
                    (((double) totalSamplesRead /
                            (double) audioStream.getFormat().getSampleRate()) * 1000.0);

            audioList.add(new DataEndSignal(duration));
            logger.debug("DataEndSignal ended");
            logger.debug("stopped recording");

            synchronized (lock) {
                lock.notify();
            }
        }


        /** Waits for the recorder to start */
        private synchronized void waitForStart() {
            // note that in theory we coulde use a LineEvent START
            // to tell us when the microphone is ready, but we have
            // found that some javasound implementations do not always
            // issue this event when a line  is opened, so this is a
            // WORKAROUND.

            try {
                while (!started) {
                    wait();
                }
            } catch (InterruptedException ie) {
                logger.warn("wait was interrupted");
            }
        }


        /**
         * Reads one frame of audio data, and adds it to the given Utterance.
         *
         * @return an Data object containing the audio data
         */
        private Data readData(Utterance utterance) throws IOException {


            // Read the next chunk of data from the TargetDataLine.
            byte[] data = new byte[frameSizeInBytes];

            int channels = audioStream.getFormat().getChannels();
            long collectTime = System.currentTimeMillis();
            long firstSampleNumber = totalSamplesRead / channels;

            int numBytesRead = audioStream.read(data, 0, data.length);

        	//logger.debug("channels: "+channels+" "+numBytesRead+" "+frameSizeInBytes+" "+bigEndian);
            
            //  notify the waiters upon start
            if (!started) {
                synchronized (this) {
                    started = true;
                    notifyAll();
                }
            }

            //if (logger.isLoggable()) {
            //    logger.finest("Read " + numBytesRead
            //            + " bytes from audio stream.");
            //}
            if (numBytesRead <= 0) {
                return null;
            }
            int sampleSizeInBytes =
                    audioStream.getFormat().getSampleSizeInBits() / 8;
            totalSamplesRead += (numBytesRead / sampleSizeInBytes);

            if (numBytesRead != frameSizeInBytes) {

                if (numBytesRead % sampleSizeInBytes != 0) {
                    throw new Error("Incomplete sample read.");
                }

                byte[] shrinked = new byte[numBytesRead];
                System.arraycopy(data, 0, shrinked, 0, numBytesRead);
                data = shrinked;
            }

            if (keepDataReference) {
                utterance.add(data);
            }

            double[] samples;

            if (bigEndian) {
                samples = DataUtil.bytesToValues
                        (data, 0, data.length, sampleSizeInBytes, signed);
            } else {
                samples = DataUtil.littleEndianBytesToValues
                        (data, 0, data.length, sampleSizeInBytes, signed);
            }

            if (channels > 1) {
                samples = convertStereoToMono(samples, channels);
            }
            logger.debug("++++++++++++++++++++++++++++read in "+samples.length+" samples");
            //for (double d: samples) {
            //   System.out.println(d);            	
            //}


            return (new DoubleData
                    (samples, (int) audioStream.getFormat().getSampleRate(),
                            collectTime, firstSampleNumber));
        }
    }


    /**
     * Converts stereo audio to mono.
     *
     * @param samples  the audio samples, each double in the array is one sample
     * @param channels the number of channels in the stereo audio
     */
    private double[] convertStereoToMono(double[] samples, int channels) {
        assert (samples.length % channels == 0);
        double[] finalSamples = new double[samples.length / channels];
        if (stereoToMono.equals("average")) {
            for (int i = 0, j = 0; i < samples.length; j++) {
                double sum = samples[i++];
                for (int c = 1; c < channels; c++) {
                    sum += samples[i++];
                }
                finalSamples[j] = sum / channels;
            }
        } else if (stereoToMono.equals("selectChannel")) {
            for (int i = selectedChannel, j = 0; i < samples.length;
                 i += channels, j++) {
                finalSamples[j] = samples[i];
            }
        } else {
            throw new Error("Unsupported stereo to mono conversion: " +
                    stereoToMono);
        }
        return finalSamples;
    }


    /** Clears all cached audio data. */
    public void clear() {
        audioList = new DataList();
    }


    /**
     * Reads and returns the next Data object from this Microphone, return null if there is no more audio data. All
     * audio data captured in-between <code>startRecording()</code> and <code>stopRecording()</code> is cached in an
     * Utterance object. Calling this method basically returns the next chunk of audio data cached in this Utterance.
     *
     * @return the next Data or <code>null</code> if none is available
     * @throws DataProcessingException if there is a data processing error
     */
    public Data getData() throws DataProcessingException {
        getTimer().start();

        Data output = null;

        if (!utteranceEndReached) {
            output = audioList.remove();
            /*if (output instanceof DoubleData) {
	            DoubleData d = (DoubleData) output;
	            for (double dd : d.getValues()) {
	            	System.out.println("dd "+dd);
	            }
            }*/
            if (output instanceof DataEndSignal) {
                utteranceEndReached = true;
            }
        }

        getTimer().stop();

        // signalCheck(output);

        return output;
    }


    /**
     * Returns true if there is more data in the Microphone. This happens either if getRecording() return true, or if
     * the buffer in the Microphone has a size larger than zero.
     *
     * @return true if there is more data in the Microphone
     */
    public boolean hasMoreData() {
        boolean moreData;
        synchronized (audioList) {
            moreData = (!utteranceEndReached || audioList.size() > 0);
        }
        return moreData;
    }
}


/** Manages the data as a FIFO queue */
class DataList {

    private List<Data> list;


    /** Creates a new data list */
    public DataList() {
        list = new LinkedList<Data>();
    }


    /**
     * Adds a data to the queue
     *
     * @param data the data to add
     */
    public synchronized void add(Data data) {
        list.add(data);
        notify();
    }


    /**
     * Returns the current size of the queue
     *
     * @return the size of the queue
     */
    public synchronized int size() {
        return list.size();
    }


    /**
     * Removes the oldest item on the queue
     *
     * @return the oldest item
     */
    public synchronized Data remove() {
        try {
            while (list.size() == 0) {
                // System.out.println("Waiting...");
                wait();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        Data data = list.remove(0);
        if (data == null) {
            System.out.println("DataList is returning null.");
        }
        return data;
    }
}
