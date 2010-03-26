package com.spokentech.speechdown.common;

public class HttpCommandFields {
	
	// Recognizer  field names
    public static final String LANGUAGE_MODEL_FLAG = "lmFlag";
    public static final String CONTINUOUS_FLAG = "continuousFlag";
    public static final String ENDPOINTING_FLAG = "doEndpointing";
    public static final String DATA_MODE = "dataMode";
    public static final String CMN_BATCH = "CmnBatchFlag";
    public static final String OUTPUT_MODE = "outputMode";  //text or json (default to text)
    
    
	//Common Recognizer and Synthesizer field names
    public static final String SAMPLE_RATE_FIELD_NAME = "sampleRate";
    public static final String BIG_ENDIAN_FIELD_NAME = "bigEndian";
    public static final String BYTES_PER_VALUE_FIELD_NAME = "bytesPerValue";
    public static final String ENCODING_FIELD_NAME = "encoding";
  
    //Recognizer attachment names
    public static final String GRAMMAR = "grammar";
    public static final String AUDIO = "audio";
    
    //Synthesizer field names
    public static final String  MIME_TYPE= "mimeType";
    public static final String  VOICE_NAME= "voice";
    public static final String  TEXT= "text";
   
    
}
