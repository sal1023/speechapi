/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.common.sphinx;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.util.props.*;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;


/**
 * Stores audio data into numbered (MS-)wav files.
 * TODO: currently the WavWriter is only able to write data in bigEndian, 
 * support for littleEndian would be nice
 * TODO: currently the WavWriter buffers all audio data until a DataEndSignal occurs.
 *
 * @author Holger Brandl
 */
public class WavWriter extends BaseDataProcessor {

    /**
     * The pathname which must obey the pattern: pattern + i + .wav. Only the pattern is required here (e.g.
     * wavdump/file). After each DataEndSignal the smalles unused 'i' is determined.
     */
    @S4String
    public static final String PROP_OUT_FILE_NAME_PATTERN = "outFilePattern";

    @S4Boolean(defaultValue = false)

    public static final String PROP_IS_COMPLETE_PATH = "isCompletePath";
    private boolean isCompletePath;

    /** The default value for PROP_RAND_STREAM_START */
    private String dumpFilePath;

    /** SphinxProperty for the number of bits per value. */
    @S4Integer(defaultValue = 16)
    public static final String PROP_BITS_PER_SAMPLE = "bitsPerSample";
    /** Default value for PROP_BITS_PER_SAMPLE. */
    private int bitsPerSample = 16;

    /** The SphinxProperty specifying whether the input data is big-endian. */
    @S4Boolean(defaultValue = true)
    public static final String PROP_BIG_ENDIAN_DATA = "bigEndianData";
    /** The default value for PROP_IS_DATA_BIG_ENDIAN. */
    private boolean isBigEndian = true;

    /** The SphinxProperty specifying whether the input data is signed. */
    @S4Boolean(defaultValue = true)
    public static final String PROP_SIGNED_DATA = "signedData";
    /** The default value of PROP_SIGNED_DATA. */
    private boolean isSigned = true;

    /** The SphinxProperty specifying whether the input data is signed. */
    @S4Boolean(defaultValue = false)
    public static final String PROP_CAPTURE_UTTERANCES = "captureUtterances";

	private static final String SPEECHAPI_URI = "http://localhost:8000/audio/";
	
    /** The default value of PROP_SIGNED_DATA. */
    protected boolean captureUtts;

	private ByteArrayOutputStream baos;
    private DataOutputStream dos;

    private int sampleRate;
    private boolean isInSpeech;
    
    private String wavFullPathName;
    private String wavFileName;

    
    private String developerId;

	private String wavUri;

	private long y=0;


    /**
     * @return the developerId
     */
    public String getDeveloperId() {
    	return developerId;
    }

	/**
     * @param developerId the developerId to set
     */
    public void setDeveloperId(String developerId) {
    	this.developerId = developerId;
    }

	/**
     * @return the captureUtts
     */
    public boolean isCaptureUtts() {
    	return captureUtts;
    }

	/**
     * @param captureUtts the captureUtts to set
     */
    public void setCaptureUtts(boolean captureUtts) {
    	this.captureUtts = captureUtts;
    }
    
    /**
     * @return the wavName
     */
    public String getWavUri() {
    	return wavUri;
    }
    
    /**
     * @return the wavName
     */
    public String getWavFileName() {
    	return wavFileName;
    }

	public WavWriter(String dumpFilePath, boolean isCompletePath, int bitsPerSample, boolean isBigEndian, boolean isSigned, boolean captureUtts) {
	    initLogger();

        this.dumpFilePath = dumpFilePath;
        this.isCompletePath = isCompletePath;

        this.bitsPerSample = bitsPerSample;
        if (bitsPerSample % 8 != 0) {
            throw new Error("StreamDataSource: bits per sample must be a multiple of 8.");
        }

        this.isBigEndian = isBigEndian;
        this.isSigned = isSigned;
        this.captureUtts = captureUtts;

        initialize();
    }

    /**
     * @return the bitsPerSample
     */
    public int getBitsPerSample() {
    	return bitsPerSample;
    }

	/**
     * @param bitsPerSample the bitsPerSample to set
     */
    public void setBitsPerSample(int bitsPerSample) {
    	this.bitsPerSample = bitsPerSample;
    }

	/**
     * @return the isBigEndian
     */
    public boolean isBigEndian() {
    	return isBigEndian;
    }

	/**
     * @param isBigEndian the isBigEndian to set
     */
    public void setBigEndian(boolean isBigEndian) {
    	this.isBigEndian = isBigEndian;
    }

	/**
     * @return the isSigned
     */
    public boolean isSigned() {
    	return isSigned;
    }

	/**
     * @param isSigned the isSigned to set
     */
    public void setSigned(boolean isSigned) {
    	this.isSigned = isSigned;
    }

	/**
     * @return the sampleRate
     */
    public int getSampleRate() {
    	return sampleRate;
    }

	/**
     * @param sampleRate the sampleRate to set
     */
    public void setSampleRate(int sampleRate) {
    	this.sampleRate = sampleRate;
    }

	public WavWriter() {
    }

    /*
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        dumpFilePath = ps.getString(WavWriter.PROP_OUT_FILE_NAME_PATTERN);
        isCompletePath = ps.getBoolean(PROP_IS_COMPLETE_PATH);

        bitsPerSample = ps.getInt(PROP_BITS_PER_SAMPLE);
        if (bitsPerSample % 8 != 0) {
            throw new Error("StreamDataSource: bits per sample must be a multiple of 8.");
        }

        isBigEndian = ps.getBoolean(PROP_BIG_ENDIAN_DATA);
        isSigned = ps.getBoolean(PROP_SIGNED_DATA);
        captureUtts = ps.getBoolean(PROP_CAPTURE_UTTERANCES);

        initialize();
    }
    
    @Override
    public Data getData() throws DataProcessingException {
        Data data = getPredecessor().getData();

        if (data instanceof DataStartSignal)
            sampleRate = ((DataStartSignal) data).getSampleRate();


        //TODO: Find out why the data and speech signals are not received in a relable way.
        //(was modified because the signals were not received in a reliable way for all situations, thus recording was unrelaible too)
        //if (data instanceof DataStartSignal || (data instanceof SpeechStartSignal && captureUtts)) {
        //    startRecording();
        //}


        if ((data instanceof DataEndSignal && !captureUtts) || (data instanceof SpeechEndSignal && captureUtts)) {
        //    stopRecording();
            isInSpeech = false;
        }

        if (data instanceof SpeechStartSignal)
            isInSpeech = true;

        if ((data instanceof DoubleData || data instanceof FloatData) && (isInSpeech || !captureUtts)) {

            DoubleData dd = data instanceof DoubleData ? (DoubleData) data : DataUtil.FloatData2DoubleData((FloatData) data);
            double[] values = dd.getValues();

            for (double value : values) {
                try {
                    dos.writeShort(new Short((short) value));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        	//System.out.println("data size is now: "+dos.size()+ " "+baos.size());
        //} else {
        	//System.out.println(data.getClass().getName()+" "+isInSpeech+" "+captureUtts);
        }

        return data;
    }

	public void stopRecording() {
	    if (isCompletePath) {
	        wavFullPathName = dumpFilePath;
	    } else {            	
	        StringBuffer fullpathname = new StringBuffer(dumpFilePath);
	        StringBuffer furi = new StringBuffer(SPEECHAPI_URI);
	        StringBuffer fname = new StringBuffer();

	        fullpathname.append("/");

	        if ((developerId == null) ||(developerId.length() <1)) {
	        	developerId = "guest";
	        }
	        //String fpath=fname.toString();
	        fullpathname.append(developerId);
	        furi.append(developerId);
	        fname.append(developerId);

	        fullpathname.append("-");
	        furi.append("-");
	        fname.append("-");

	        
	        String x = Long.toString(System.currentTimeMillis());
	        //int x = getNextFreeIndex(fullpathname.toString());
	        fullpathname.append(x);
	        furi.append(x);
	        fname.append(x);

	        fullpathname.append("-");
	        furi.append("-");
	        fname.append("-");

	        
	        y++;
	        fullpathname.append(y);
	        furi.append(y);
	        fname.append(y);
	        
	        fullpathname.append( ".wav");
	        furi.append( ".wav");
	        fname.append( ".wav");


	        wavFullPathName = fullpathname.toString();
	        wavUri = furi.toString();        	    
	        wavFileName = fname.toString();
	        
	        //System.out.println(wavFileName);
	        //System.out.println(wavUri);
	        //System.out.println(wavFullPathName);

	        
	    }
	    writeFile(wavFullPathName);
        isInSpeech = false;
    }

	public void startRecording() {
        isInSpeech = true;
	    baos = new ByteArrayOutputStream();
	    dos = new DataOutputStream(baos);
    }


    //TODO:  Find a more efficient way of getting a unique filename!
    private static int getNextFreeIndex(String outPattern) {
    	
    	
        int fileIndex = 0;
        while (new File(outPattern + fileIndex + ".wav").isFile())
            fileIndex++;

        return fileIndex;
    }


    /** Initializes this DataProcessor. This is typically called after the DataProcessor has been configured. */
    @Override
    public void initialize() {
        super.initialize();

        assert dumpFilePath != null;
 
        
    }


    private static AudioFileFormat.Type getTargetType(String extension) {
        AudioFileFormat.Type[] typesSupported = AudioSystem.getAudioFileTypes();

        for (AudioFileFormat.Type aTypesSupported : typesSupported) {
            if (aTypesSupported.getExtension().equals(extension)) {
                return aTypesSupported;
            }
        }

        return null;
    }


    /**
     * Converts a big-endian byte array into an array of doubles. Each consecutive bytes in the byte array are converted
     * into a double, and becomes the next element in the double array. The size of the returned array is
     * (length/bytesPerValue). Currently, only 1 byte (8-bit) or 2 bytes (16-bit) samples are supported.
     *
     * @param values
     * @param bytesPerValue the number of bytes per value
     * @param signedData    whether the data is signed
     * @return a double array, or <code>null</code> if byteArray is of zero length
     * @throws ArrayIndexOutOfBoundsException
     */
    public static byte[] valuesToBytes(double[] values, int bytesPerValue, boolean signedData)
            throws ArrayIndexOutOfBoundsException {

        byte[] byteArray = new byte[bytesPerValue * values.length];

        int byteArInd = 0;

        for (double value : values) {
            int val = (int) value;


            for (int j = bytesPerValue - 1; j >= 0; j++) {
                byteArray[byteArInd + j] = (byte) (val & 0xff);
                val = val >> 8;
            }

            byteArInd += bytesPerValue;
        }

        return byteArray;
    }


    public static AudioInputStream convertDoublesToAudioStream(double[] values, int sampleRate) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        for (double value : values) {
            try {
                dos.writeShort(new Short((short) value));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        AudioFormat wavFormat = new AudioFormat(sampleRate, 16, 1, true, true);
        byte[] abAudioData = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(abAudioData);

        return new AudioInputStream(bais, wavFormat, abAudioData.length / wavFormat.getFrameSize());
    }


    /** Writes a given double array into a wav file (given the sample rate of the signal).
     * @param signal
     * @param sampleRate
     * @param targetFile*/
    public static void writeWavFile(double[] signal, int sampleRate, File targetFile) {
        AudioInputStream ais = WavWriter.convertDoublesToAudioStream(signal, sampleRate);
        AudioFileFormat.Type outputType = getTargetType("wav");

        try {
            AudioSystem.write(ais, outputType, targetFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * Writes the current stream to disc; override this method if you want to take 
    * additional action on file writes
    *
    * @param wavName name of the file to be written
    */
    protected void writeFile(String wavName) {
    	//System.out.println("writing file "+wavName+" "+baos.size()+" "+dos.size());
        AudioFormat wavFormat = new AudioFormat(sampleRate, bitsPerSample, 1, isSigned, true);
        AudioFileFormat.Type outputType = getTargetType("wav");

        try {
	        dos.flush();
	        dos.close();

        } catch (IOException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
        }
    	//System.out.println("writing file "+wavName+" "+baos.size()+" "+dos.size());
        byte[] abAudioData = baos.toByteArray();
    	//System.out.println("writing file "+wavName+" "+abAudioData.length);
        ByteArrayInputStream bais = new ByteArrayInputStream(abAudioData);
        AudioInputStream ais = new AudioInputStream(bais, wavFormat, abAudioData.length / wavFormat.getFrameSize());

        File outWavFile = new File(wavName);

        if (AudioSystem.isFileTypeSupported(outputType, ais)) {
            try {
                AudioSystem.write(ais, outputType, outWavFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }   	
    }



}
