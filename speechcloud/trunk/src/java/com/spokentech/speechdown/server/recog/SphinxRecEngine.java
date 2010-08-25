/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.recog;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioInputStream;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.Utterance;

import com.spokentech.speechdown.common.Utterance.OutputFormat;
import com.spokentech.speechdown.common.sphinx.AudioStreamDataSource;
import com.spokentech.speechdown.common.sphinx.XugglerAudioStreamDataSource;
import com.spokentech.speechdown.common.sphinx.IdentityStage;
import com.spokentech.speechdown.common.sphinx.InsertSpeechSignalStage;
import com.spokentech.speechdown.common.sphinx.SpeechDataMonitor;
import com.spokentech.speechdown.common.sphinx.WavWriter;
import com.spokentech.speechdown.server.domain.SpeechRequestDTO;
import com.spokentech.speechdown.server.domain.RecogRequest;
import com.spokentech.speechdown.server.util.ResultUtils;
import com.spokentech.speechdown.server.util.ServiceLogger;
import com.spokentech.speechdown.server.util.pool.AbstractPoolableObject;
import com.spokentech.speechdown.server.util.pool.PoolableObject;

import edu.cmu.sphinx.decoder.ResultListener;

import edu.cmu.sphinx.decoder.scorer.AcousticScorer;

import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.feature.BatchCMN;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.FeatureTransform;

import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Dither;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.recognizer.Recognizer.State;
import edu.cmu.sphinx.result.ConfidenceScorer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;


public class SphinxRecEngine implements RecEngine {

    private static Logger _logger = Logger.getLogger(SphinxRecEngine.class);
    
    private static DecimalFormat format = new DecimalFormat("#.#####");
 
	public static final short WAITING_FOR_SPEECH = 0;
	public static final short SPEECH_IN_PROGRESS = 1;
	public static final short COMPLETE = 2;
	protected volatile short _state = COMPLETE;

	private FrontEnd fe = null;

    private boolean hotword = false;       
	private ConfigurationManager _cm;
	private int _id;
	
    private FileWriter outTextFile;

	protected /*static*/ Timer _timer = new Timer();
	protected TimerTask _noInputTimeoutTask;	
	protected StreamDataSource _dataSource = null;
	
	//front end elements
	DataBlocker dataBlocker ;
	SpeechClassifier speechClassifier;
	SpeechMarker speechMarker;
	NonSpeechDataFilter nonSpeechDataFilter;
	SpeechDataMonitor speechDataMonitor ;
	SpeechEventListener listener;
	InsertSpeechSignalStage insertSpeechSignalStage;
	IdentityStage identityStage ;
	Preemphasizer preemphasizer;
	Dither dither;
	RaisedCosineWindower raisedCosineWindower ;
	DiscreteFourierTransform discreteFourierTransform ;
	MelFrequencyFilterBank melFrequencyFilterBank ;
	DiscreteCosineTransform discreteCosineTransform ;
	BatchCMN batchCmn ;
	LiveCMN liveCmn;
	FeatureTransform lda;
	DeltasFeatureExtractor deltasFeatureExtractor;
	private WavWriter recorder; 

	private Recognizer recognizer;
    private JSGFGrammar jsgf;
	private GrammarManager grammarManager;
	private Loader loader;
    private AcousticScorer  scorer;
	private ConfidenceScorer condfidenceScorer;
	private LogMath logMath;
	private String recordingFilePath;
	private boolean recordingEnabled;



	private boolean transcribeMode = false;

    Gson gson = new Gson();

    
    public SphinxRecEngine() {
	    super();
    }
    
    public SphinxRecEngine(ConfigurationManager cm, GrammarManager grammarManager,String prefixId, int id, String recordingFilePath, boolean recordingEnabled) throws IOException, PropertyException, InstantiationException {

    	_logger.debug("Creating a recognizer "+prefixId +"recognizer"+id);
    	recognizer = (Recognizer) cm.lookup(prefixId+"recognizer"+id);
       
    	recognizer.allocate();

	    jsgf = (JSGFGrammar) cm.lookup("grammar");
	    this.grammarManager = grammarManager;

    	loader = (Sphinx3Loader) cm.lookup("loader");
        _cm = cm;
        _id = id;
		scorer = (AcousticScorer)_cm.lookup(prefixId+"scorer"+id);

        MyStateListener stateListener =  new MyStateListener();
        MyResultListener resultListener = new MyResultListener();
		recognizer.addResultListener(resultListener);

		recognizer.addStateListener(stateListener);

    	this.recordingEnabled = recordingEnabled;
    	
    	Date dateNow = new Date ();
        SimpleDateFormat dateformatMMDDYYYY = new SimpleDateFormat("MMddyyyy");
 
        StringBuilder nowMMDDYYYY = new StringBuilder( dateformatMMDDYYYY.format( dateNow ) );
    	//this.recordingFilePath = recordingFilePath+"/"+nowMMDDYYYY+"-";
    	this.recordingFilePath = recordingFilePath;
    	
    	// todo:  should be a single filerwriter (but then it must be threadsafe), else each recognizer maybe should have its own file...
    	// Not really sure of what happens when there is a many Filewriters with the same filename
    	if (recordingEnabled) {
		    try {
		    	outTextFile = new FileWriter(recordingFilePath+"/"+nowMMDDYYYY+"-"+id+".txt",true);
		    } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
    	}

    	//create frontend elements (to be assembled into frontends at run time)
    	createFrontEndElements();
    	
    	// obtain scorer from configuration manager
        condfidenceScorer = (ConfidenceScorer) cm.lookup("confidenceScorer");
        ResultUtils.setLogm((LogMath) cm.lookup("logMath"));
    }
    
    //constructor that does not use configuration manager (more convenient for creating pools)
    public SphinxRecEngine(Recognizer recognizer, JSGFGrammar jsgf, GrammarManager grammarManager,
    						Loader loader, AcousticScorer scorer,ConfidenceScorer cScorer,LogMath lm,
    						String recordingFilePath, boolean recordingEnabled) throws IOException, PropertyException, InstantiationException {
   	
    	//prototypes
    	this.recognizer = recognizer;      
    	this.recognizer.allocate();
	    this.jsgf = jsgf;
	    this.grammarManager = grammarManager;
        condfidenceScorer = cScorer;
	    
	    //singletons
    	this.loader = loader;
		this.scorer = scorer;


        MyStateListener stateListener =  new MyStateListener();
        MyResultListener resultListener = new MyResultListener();
		recognizer.addResultListener(resultListener);

		recognizer.addStateListener(stateListener);

    	this.recordingEnabled = recordingEnabled;
    	
    	Date dateNow = new Date ();
        SimpleDateFormat dateformatMMDDYYYY = new SimpleDateFormat("MMddyyyy");
 
        StringBuilder nowMMDDYYYY = new StringBuilder( dateformatMMDDYYYY.format( dateNow ) );
    	this.recordingFilePath = recordingFilePath;
    	
    	// todo:  should be a single filerwriter (but then it must be threadsafe), else each recognizer maybe should have its own file...
    	// Not really sure of what happens when there is a many Filewriters with the same filename
    	if (recordingEnabled) {
		    try {
		    	outTextFile = new FileWriter(recordingFilePath+"/"+nowMMDDYYYY+".txt",true);
		    } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
    	}

    	//create frontend elements (to be assembled into frontends at run time)
    	createFrontEndElements();

        ResultUtils.setLogm(lm);
    }
    
    
    
    
    //constructor that does not use configuration manager (more convenient for creating pools) for setter injection
    public void startup()  {

    	recognizer.allocate();

        MyStateListener stateListener =  new MyStateListener();
        MyResultListener resultListener = new MyResultListener();
		recognizer.addResultListener(resultListener);
		recognizer.addStateListener(stateListener);

    	Date dateNow = new Date ();
        SimpleDateFormat dateformatMMDDYYYY = new SimpleDateFormat("MMddyyyy");
 
        StringBuilder nowMMDDYYYY = new StringBuilder( dateformatMMDDYYYY.format( dateNow ) );
    	//this.recordingFilePath = recordingFilePath;
    	
    	// todo:  should be a single filerwriter (but then it must be threadsafe), else each recognizer maybe should have its own file...
    	// Not really sure of what happens when there is a many Filewriters with the same filename
    	if (recordingEnabled) {
		    try {
		    	outTextFile = new FileWriter(recordingFilePath+"/"+nowMMDDYYYY+".txt",true);
		    } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
    	}

    	//create frontend elements (to be assembled into frontends at run time)
    	createFrontEndElements();

        ResultUtils.setLogm(logMath);
    }

	//Recognize using language mode
    public Utterance recognize(InputStream as, String mimeType, AFormat af, OutputFormat outMode, boolean doEndpointing, boolean cmnBatch, SpeechRequestDTO hr) {

    	long  start = System.nanoTime();
    	long startTime= System.currentTimeMillis();
		_logger.info("Using recognizer # "+_id+ ", time: "+startTime);
		String resultCode = "Success";
		String resultMessage = null;
		
		//setup the recorder
		//TODO: Test if this is needed!
		if (recordingEnabled) {
			recorder.setBigEndian(true);
			recorder.setBitsPerSample(16);
			recorder.setSampleRate(8000);
			recorder.setSigned(true);
			
			recorder.setCaptureUtts(false);
			recorder.setDeveloperId(hr.getDeveloperId());
			recorder.startRecording();
		}
		
	    Result r = doRecognize(as, mimeType, af, outMode, doEndpointing, cmnBatch);
	    _logger.info("Result: " + (r != null ? r.getBestFinalResultNoFiller() : null));
	    
		long stop = System.nanoTime();
		long wall = (stop - start)/1000000;
		long streamLen = _dataSource.getLengthInMs();
		double ratio = (double)wall/(double)streamLen;
		_logger.info(ratio+ "  Wall time "+ wall+ " stream length "+ streamLen);
		
		if (recordingEnabled) {	
			recorder.stopRecording();
			
	        RecogRequest rr = new RecogRequest();
	        
			String raw = null;
			String pro = null;
			
			//getting the best result and pronunciation twice (at least in the json output mode case)
			if (r != null) {
				raw = r.getBestFinalResultNoFiller();
				pro = r.getBestPronunciationResult();
			}
	        
		    Date d = new Date();
		    rr.setDate(d);

		    if (af  != null) {
			    rr.setEncoding(af.getEncoding());
			    rr.setSampleRate((int)af.getSampleRate());
			    rr.setBigEndian(af.isBigEndian());
			    rr.setBytesPerValue(af.getSampleSizeInBits()/8);
		    }
		    rr.setCmnBatch(cmnBatch);
		    rr.setContentType(mimeType);

		    rr.setEndPointing(doEndpointing);
		    rr.setGrammar(null);

		    rr.setLm(true);
		    rr.setContinuous(false);
		    
		    rr.setTags(null);
		    rr.setRawResults(raw);
		    rr.setPronunciation(pro);
		    
		    rr.setWallTime(wall);
		    rr.setStreamLen(streamLen);
		    
		    rr.setAudioUri(recorder.getWavUri());
		    rr.setAudioFileName(recorder.getWavFileName());

		    //rr.setHttpRequest(hr);
			//ServiceLogger.logRecogRequest(rr,hr);
		    
		    hr.setRecog(rr);
		    try {
	            ServiceLogger.logHttpRequest(hr);
            } catch (Exception e) {
            	_logger.warn("exception thrown while recording http request");
	            e.printStackTrace();
            }

		}
		
	    long tt = System.nanoTime();
		long x = (tt - stop)/1000000;
		_logger.debug("  Logging time "+ x);
		
		
		//Do i need this?  
        _logger.debug("LM stopping audio "+_state);
        synchronized (SphinxRecEngine.this) {
        	 stopAudioTransfer();
        	_state = COMPLETE;
        }
	
        Utterance utterance = null;
		if (r != null) {
			//TODO: change to return Utterance object that can be serialized to json or plant text (see transcribe method)
			//current approach is a bit of a hack, by constructing with a json flag.
        	utterance = ResultUtils.getAllResults(r, false, false,condfidenceScorer);
        	utterance.setRCode("Success");
	    } else {
	    	utterance = new Utterance();
			utterance.setRCode("NullResult");
			utterance.setRMessage("Recognizer returned null result");
	    }

        return utterance;
    }
	//recognize using a grammar
	public Utterance recognize(InputStream as, String mimeType, String grammar, AFormat af, OutputFormat outMode,  boolean doEndpointing, boolean cmnBatch, SpeechRequestDTO hr) {

		long  start = System.nanoTime();
    	long startTime= System.currentTimeMillis();
		_logger.info("Using recognizer # "+_id+ ", time: "+startTime);
     
		//setup the recorder
		if (recordingEnabled) {
			//TODO: Test if setting the format in the recorder is needed!
			recorder.setBigEndian(true);
			recorder.setBitsPerSample(16);
			recorder.setSampleRate(8000);
			recorder.setSigned(true);

			recorder.setDeveloperId(hr.getDeveloperId());
			recorder.setCaptureUtts(false);
			recorder.startRecording();
		}
        
	
		String resultCode = "Success";
		String resultMessage = null;
		GrammarLocation grammarLocation = null;
		Result r = null; 
	    Utterance utterance = null;
	    try {
	    	
	    	//save and load the grammar
	        grammarLocation = grammarManager.saveGrammar(grammar);
	        loadJSGF(jsgf, grammarLocation);
	        _logger.debug("After save and load grammar" + System.currentTimeMillis());
	        
	        //do the recognition
		    r = doRecognize(as, mimeType, af, outMode, doEndpointing,  cmnBatch);
			
		    //process the results (text or json)
	

			if (r != null) {
				//TODO: change to return Utterance object that can be serialized to json or plant text (see transcribe method)
				//current approach is a bit of a hack, by constructing with a json flag.
	        	utterance = ResultUtils.getAllResults(r, false, false,jsgf.getRuleGrammar());
		    } else {
		    	resultCode = "NullResult";
		    	resultMessage ="Null result returned from Reognition Engine";
		    	utterance = new Utterance();
		    }
			
        } catch (GrammarException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	        resultCode="GrammarException";
	        resultMessage = e.getMessage();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	        resultCode="IOException";
	        resultMessage = e.getMessage();
        }	
			
		// Do Recording 
		long stop = System.nanoTime();
		long wall = (stop - start)/1000000;
		long streamLen = _dataSource.getLengthInMs();
		double ratio = (double)wall/(double)streamLen;
		_logger.info(ratio+ "  Wall time "+ wall+ " stream length "+ streamLen);
		
		if (recordingEnabled) {
			_logger.debug("logging enabled, stopping recording and writing to database");
			recorder.stopRecording();

	        RecogRequest rr = new RecogRequest();
	        
	        
	        //TODO: Save processing time and get raw and pro from results (it is already there)
			String raw = null;
			String pro = null;
			if (r != null) {
				raw = r.getBestFinalResultNoFiller();
				pro = r.getBestPronunciationResult();
			    rr.setRawResults(raw);
			    rr.setPronunciation(pro);
			}
	        
		    Date d = new Date();
		    rr.setDate(d);
		    
		    if (af  != null) {
			    rr.setEncoding(af.getEncoding());
			    rr.setSampleRate((int)af.getSampleRate());
			    rr.setBigEndian(af.isBigEndian());
			    rr.setBytesPerValue(af.getSampleSizeInBits()/8);
		    }
		    
		    rr.setCmnBatch(cmnBatch);
		    rr.setContentType(mimeType);

		    rr.setEndPointing(doEndpointing);
		    rr.setGrammar(grammar);

		    rr.setLm(false);
		    rr.setContinuous(false);
		    
		    
		    //TODO: the tags are now in the utterance/json object, get it from there
		    //if (results != null)
		    //   rr.setTags(results.getText());

		    
		    rr.setWallTime(wall);
		    rr.setStreamLen(streamLen);
		    
		    rr.setAudioUri(recorder.getWavUri());
		    rr.setAudioFileName(recorder.getWavFileName());

		    //rr.setHttpRequest(hr);
			//ServiceLogger.logRecogRequest(rr,hr);
		    
		    hr.setRecog(rr);
		    try {
	            ServiceLogger.logHttpRequest(hr);
            } catch (Exception e) {
            	_logger.warn("exception thrown while recording http request");
	            e.printStackTrace();
            }
		    _logger.debug("done recording...");

		}
	     long tt = System.nanoTime();
		long x = (tt - stop)/1000000;
		_logger.debug("  Logging time "+ x);
		
		
		utterance.setRCode(resultCode);
		utterance.setRMessage(resultMessage);
        return utterance;

    }

	private Result doRecognize(InputStream as, String mimeType, AFormat af, OutputFormat outMode, boolean doEndpointing, boolean cmnBatch) {
	    //TODO: Timers for recog timeout
		
		transcribeMode = false;

	
	     // configure the audio input for the recognizer
	 	 // if the format is null, that indicates that no format was passed in
		 // use xuggler front end and let xuggler get the format from the header
		 if (af == null) {
			 _dataSource = new XugglerAudioStreamDataSource();
			 fe = createAudioFrontend(true,true,(DataProcessor) _dataSource);
			 
		 // if the format is not (it wa specified) use a front end based on mimetype.
	     // used for realtime mode
	     // TODO: maybve could remove the wav case below which uses audiostreamdatasource  and use xuggler (So more encodings work) 
		 // with real time mode (but that is if the format is in the header of the attachment created by the realtime clients)
		 }else {
			 String parts[] = mimeType.split("/");
			 if (parts[1].equals("x-s4audio")) {
				 _dataSource = new S4DataStreamDataSource();
				 fe = createAudioFrontend(doEndpointing,cmnBatch,(DataProcessor)_dataSource);
			 } else if (parts[1].equals("x-s4feature")) {
				 _dataSource = new S4DataStreamDataSource();
				 fe = createFeatureFrontend((DataProcessor)_dataSource);
				 if (doEndpointing) {
					 _logger.warn("Endpointing not supported for feature streams");
				 }
			 } else if (parts[1].equals("x-wav")) {
				 _dataSource = new AudioStreamDataSource();
				 fe = createAudioFrontend(doEndpointing,cmnBatch,(DataProcessor)_dataSource);
			 } else {
				 _dataSource = new AudioStreamDataSource();
				 //TODO: Check if xuggler is installed on this machine
				 //_dataSource = new XugglerAudioStreamDataSource();
				 fe = createAudioFrontend(doEndpointing,cmnBatch,(DataProcessor)_dataSource);
			 }
		     _logger.debug("-----> "+mimeType+ " "+parts[1]);
		 }
		 if (doEndpointing) {
			//TODO: start the timer
			//_noInputTimeoutTask = new TimerTask(30000);
		 }

	     //set the first stage of the front end
		 fe.setDataSource((DataProcessor) _dataSource);
		 
		 // set the front end in the scorer in realtime
		 scorer.setFrontEnd(fe);
	     		 
		 //AFormat af = new AFormat(encoding.toString(), sampleRate, bytesPerValue*8, 1, bigEndian, true, bytesPerValue, sampleRate);
		 //_dataSource.setInputStream(as, "ws-audiostream", af);
		 _dataSource.setInputStream(as, "ws-audiostream",af);
		    
		_logger.debug("After setting the input stream " + System.currentTimeMillis());
	    
	    // decode the audio file.
	    //_logger.debug("Decoding " + audioFileURL);
		Result r = recognizer.recognize();
	
		//for now record results to a file (eventually need to start using the database recording)
		//if (recordingEnabled) {
		//	logResults(r, recorder.getWavFileName());
		//}

        String resultText;
        if (r != null) {
	        if (outMode == OutputFormat.text) {
	        	resultText = r.getBestFinalResultNoFiller();  
	  	
	        } else if (outMode == OutputFormat.json) {
	        	Utterance utterance = ResultUtils.getAllResults(r, false, false,condfidenceScorer);
	            resultText = gson.toJson(utterance);
	        	
	        } else {
	        	resultText = r.getBestFinalResultNoFiller();  
	        	_logger.warn("Inrecognized output format: "+outMode+ "  ,using plain text mode as a default.");
	        }
			_logger.debug(resultText);
        } else {
        	_logger.debug("Null results...");
        }
        
        
		fe=null;
	    return r;
    }
    
	public void stopAudioTransfer() {
		 try {
		        _dataSource.closeDataStream();
	     } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
	     }

	}
	
	
	@Override
    public String transcribe(InputStream as, String mimeType, AFormat af,  OutputFormat outMode, PrintWriter out,HttpServletResponse response, SpeechRequestDTO hr) {
		
		_logger.debug("Using recognizer # "+_id);
	    //SAL
		//_recognizer.allocate();
        _logger.debug("After allocate" + System.currentTimeMillis());
        
		if (recordingEnabled) {
			recorder.setCaptureUtts(false);
			recorder.setDeveloperId(hr.getDeveloperId());
			recorder.startRecording();
		}
        
		long  start = System.nanoTime();

	    String r = doTranscribe(as, mimeType, af, outMode, out, response);
	    
		long stop = System.nanoTime();
		long wall = (stop - start)/1000000;
		long streamLen = _dataSource.getLengthInMs();
		double ratio = (double)wall/(double)streamLen;
		
		if (recordingEnabled) {	
			recorder.stopRecording();
	        RecogRequest rr = new RecogRequest();
	        
		    Date d = new Date();
		    rr.setDate(d);
		    
		    if (af  != null) {
			    rr.setEncoding(af.getEncoding());
			    rr.setSampleRate((int)af.getSampleRate());
			    rr.setBigEndian(af.isBigEndian());
			    rr.setBytesPerValue(af.getSampleSizeInBits()/8);
		    }
		    
		    rr.setCmnBatch(true);
		    rr.setContentType(mimeType);

		    rr.setEndPointing(true);

		    rr.setLm(true);
		    rr.setContinuous(true);
		    
		    rr.setRawResults(r);
		    
		    rr.setWallTime(wall);
		    rr.setStreamLen(streamLen);
		    
		    rr.setAudioUri(recorder.getWavUri());
		    rr.setAudioFileName(recorder.getWavFileName());

		    hr.setRecog(rr);
		    try {
	            ServiceLogger.logHttpRequest(hr);
            } catch (Exception e) {
            	_logger.warn("exception thrown while recording http request");
	            e.printStackTrace();
            }
		}

	    return r;
    }
	
	
	private String doTranscribe(InputStream as, String mimeType, AFormat af, OutputFormat outMode, PrintWriter out,HttpServletResponse response) {
	    //TODO: Timers for recog timeout
	
		transcribeMode = true;
		
	    List<Utterance> utterances = new ArrayList<Utterance>();
		
		FrontEnd fe = null;
	    // configure the audio input for the recognizer

		
	 	 // if the format is null, that indicates that no format was passed in
		 // use xuggler front end and let xuggler get the format from the header
		 if (af == null) {
			 _dataSource = new XugglerAudioStreamDataSource();
			 fe = createAudioFrontend(true,true,(DataProcessor) _dataSource);
			 
		 // if the format is not (it wa specified) use a front end based on mimetype.
	     // used for realtime mode
	     // TODO: maybve could remove the wav case below which uses audiostreamdatasource  and use xuggler (So more encodings work) 
		 // with real time mode (but that is if the format is in the header of the attachment created by the realtime clients)
		 } else {
			 String parts[] = mimeType.split("/");
			 if (parts[1].equals("x-s4audio")) {
				 _dataSource = new S4DataStreamDataSource();
				 fe = createAudioFrontend(true,true,(DataProcessor) _dataSource);
			 } else if (parts[1].equals("x-s4feature")) {
				 _logger.warn("Feature mode Endpointing not for continuous recognition mode");
				 _dataSource = new S4DataStreamDataSource();
			 } else if (parts[1].equals("x-wav")) {
				 _dataSource = new AudioStreamDataSource();
				 fe = createAudioFrontend(true,true,(DataProcessor) _dataSource);
			 } else {
				 //TODO: Check if xuggler is installed on this machine
				 _dataSource = new XugglerAudioStreamDataSource();
				 fe = createAudioFrontend(true,true,(DataProcessor) _dataSource);
			 }
		     _logger.debug("-----> "+mimeType+ " "+parts[1]);
		 }
	     //set the first stage of the front end
		 fe.setDataSource((DataProcessor) _dataSource);
		 
		 // set the front end in the scorer in realtime
		 scorer.setFrontEnd(fe);
		 
		// AFormat af = new AFormat(encoding.toString(), sampleRate, bytesPerValue*8, 1, bigEndian, true, bytesPerValue, sampleRate);
		 //_dataSource.setInputStream(as, "ws-audiostream", af);
		 _dataSource.setInputStream(as, "ws-audiostream",af);

		_logger.debug("After setting the input stream" + System.currentTimeMillis());
	    
	    // decode the audio file.
	    //_logger.debug("Decoding " + audioFileURL);
	    //List<Result> resultList = new ArrayList<Result>();
		String totalResult ="";

		//List<Utterance> utterences = new ArrayList<Utterance>();
		Result result;
        while ((result = recognizer.recognize())!= null) {

            String resultText = null;      
	        
	        if (outMode == OutputFormat.text) {
	        	resultText = result.getBestFinalResultNoFiller();  
      	
	        } else if (outMode == OutputFormat.json) {
	        	Utterance utterance = ResultUtils.getAllResults(result, false, false,condfidenceScorer);
	            resultText = gson.toJson(utterance);
	        	
	        } else {
	        	resultText = result.getBestFinalResultNoFiller();  
	        	_logger.warn("Inrecognized output format: "+outMode+ "  ,using plain text mode as a default.");
	        }
            _logger.debug(resultText);
            
            //if the PrintWriter is not null, then send the text utterence by utterence
            if (out != null)
            	out.println(resultText); 	        	
	        

            totalResult = totalResult+resultText;

            //chunked encoded, so flush for realtime streaming
            if (response != null)
	            try {
	                response.flushBuffer();
                } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                }
            //resultList.add(result);
    
        }
        fe = null;
        this.stopAudioTransfer();
        transcribeMode = false;
	    return totalResult;
    }
	

	/* (non-Javadoc)
     * @see com.spokentech.speechdown.server.recog.RecEngine#recognize(javax.sound.sampled.AudioInputStream, java.lang.String)
     */
	//recognize using a grammar (the SOAP method) 
	//TODO: Re-write
	public Utterance recognize(AudioInputStream as, String grammar) {
	    return null;
	}
    	
	   /**
	  * TODOC
	  * @param grammarLocation
	  * @throws IOException
	  * @throws GrammarException
	  */
	public synchronized void loadJSGF(JSGFGrammar jsgfGrammar, GrammarLocation grammarLocation) throws IOException, GrammarException {
		jsgfGrammar.setBaseURL(grammarLocation.getBaseURL());

		jsgfGrammar.loadJSGF(grammarLocation.getGrammarName());
		_logger.debug("loadJSGF(): completed successfully.");
	}
 





    /**
     * TODOC
     * @param text
     * @param ruleName
     * @return
     * @throws GrammarException
     */
    public synchronized RuleParse parse(String text, String ruleName) throws GrammarException {
        
        RuleGrammar ruleGrammar = jsgf.getRuleGrammar();
        return ruleGrammar.parse(text, ruleName);
    }


    /**
     * @return the hotword
     */
    public boolean isHotword() {
        return hotword;
    }

    /**
     * @param hotword the hotword to set
     */
    public void setHotword(boolean hotword) {
        this.hotword = hotword;
    }

    
    private class MyStateListener implements StateListener {

		public void statusChanged(State  arg0) {
	        _logger.debug("Recognizer Status changed to "+arg0.toString() +" "+System.currentTimeMillis());
	        
        }

		public void newProperties(PropertySheet arg0) throws PropertyException {
	        _logger.debug("StateListener New properties called");
	        
        }	
    }
    
    private class MyResultListener implements ResultListener {

		public void newResult(Result arg0) {
			_logger.debug("best final result: "+arg0.getBestFinalResultNoFiller());
			_logger.debug("best pronuciation: "+arg0.getBestPronunciationResult());
			_logger.debug("Frame "+arg0.getStartFrame()+ " to "+arg0.getEndFrame()+"("+arg0.getFrameNumber()+")");
	        
        }

		public void newProperties(PropertySheet arg0) throws PropertyException {
	        _logger.debug("ResultListener New properties called");
	        
        }
    	
    }
    /**
   	 * The Class Listener.
   	 */
   	protected class Listener implements SpeechEventListener {

			@Override
	        public void speechStarted() {
	            _logger.debug("speechStarted()");

	            synchronized (SphinxRecEngine.this) {
	                if (_state == WAITING_FOR_SPEECH) {
	                    _state = SPEECH_IN_PROGRESS;
	                }
	                if (_noInputTimeoutTask != null) {
	                    _noInputTimeoutTask.cancel();
	                    _noInputTimeoutTask = null;
	                }
	            }
	        }
	        
        	public void speechEnded() {
	            _logger.debug("speechEnded()");
	            synchronized (SphinxRecEngine.this) {
	            	if (!transcribeMode)
	            	    stopAudioTransfer();
	            	_state = COMPLETE;
	            }

	        }

        	public void noInputTimeout() {
	            _logger.debug("no input timeout()");
	            synchronized (SphinxRecEngine.this) {
	            	stopAudioTransfer();
	            	_state = COMPLETE;
	            }	   

	        }

			@Override
            public void recognitionComplete(Utterance rr) {
	            // TODO Auto-generated method stub
	            
            }

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
            synchronized (SphinxRecEngine.this) {
                _noInputTimeoutTask = null;
                if (_state == WAITING_FOR_SPEECH) {
                    _state = COMPLETE;
                    stopAudioTransfer();

                }
            }
        }
        
    }
    
    
    
    private FrontEnd createFeatureFrontend(DataProcessor dataSource) {
    	   ArrayList<DataProcessor> components = new ArrayList <DataProcessor>();
    	   //components.add (new IdentityStage ());
     	   components.add(dataSource);
    	   FrontEnd fe = new FrontEnd (components);
    	   return fe;   	
    }

	
	
    private void createFrontEndElements() {

    	// create all the components that may be needed for the front end ahead of time. 
    	// to save time at recognition requests time.
    	dataBlocker = new DataBlocker(10);
    	speechClassifier = new SpeechClassifier(10,0.003,10.0,0.0);
    	speechMarker = new SpeechMarker(200,200,100,50,100,15);
    	nonSpeechDataFilter = new NonSpeechDataFilter();
    	speechDataMonitor  = new SpeechDataMonitor();
    	listener = new Listener();
    	insertSpeechSignalStage = new InsertSpeechSignalStage();
    	identityStage = new IdentityStage();
    	preemphasizer = new Preemphasizer(0.97);
    	dither = new Dither();
    	raisedCosineWindower = new RaisedCosineWindower(0.46,(float)25.625,(float)10.0);
    	discreteFourierTransform = new DiscreteFourierTransform(-1,false);
    	melFrequencyFilterBank = new MelFrequencyFilterBank((double)133.0,(double)3500.0,31);
    	discreteCosineTransform = new DiscreteCosineTransform(40,13);
    	batchCmn = new BatchCMN();
    	liveCmn = new LiveCMN(12,500,800);
    	deltasFeatureExtractor = new DeltasFeatureExtractor(3);
    	lda = new FeatureTransform(loader);
    	
		boolean isCompletePath = false;
		int bitsPerSample = 16;
		boolean isSigned = true;
		
		//setting this false captures the entire audio from datastart to dataend.  
		//since not always getting speech signals, need to false,  Revisit when doing more transcription.
		boolean captureUtts = false;
		boolean bigEndian = false;
		recorder = new WavWriter(recordingFilePath,isCompletePath,bitsPerSample,bigEndian,isSigned,captureUtts);
    }
    
    
    private FrontEnd createAudioFrontend(boolean endpointing, boolean batchCMN, DataProcessor dataSource) {
    	
 	   ArrayList<DataProcessor> components = new ArrayList <DataProcessor>();
 	   components.add(dataSource);
	   components.add (dataBlocker);
	   if (endpointing) {
		  components.add (speechClassifier);
	      components.add (speechMarker);
	      components.add (nonSpeechDataFilter);
	   //} else {
		
		//   components.add(insertSpeechSignalStage);
	   }
	   _logger.debug("Endpointing is: "+endpointing);
	   //always add the monitor and listener
	   components.add (speechDataMonitor);
	   speechDataMonitor.setSpeechEventListener(listener);
	    
	   //this is just for logging debug messages.
	   components.add(identityStage);
	   if (recordingEnabled && (recorder != null)) {
	      components.add(recorder);
	   }
	   components.add (preemphasizer);
	   components.add (dither);
	   components.add (raisedCosineWindower);
	   components.add (discreteFourierTransform);
	   components.add (melFrequencyFilterBank);

	   components.add (discreteCosineTransform);
	   if (batchCMN) {
	      components.add (batchCmn);
	   } else {
	      components.add (liveCmn);
	   }
	   
	   components.add (deltasFeatureExtractor);
	   components.add (lda);
	   	   
       //for (DataProcessor dp : components) {
       //   _logger.debug(dp);
       //}
	   
	   FrontEnd fe = new FrontEnd (components);
	   return fe;   
    }



	public void logResults(Result r,String wavName) {
		try {
			String results = null;
			if (r == null) {
				results ="null result";
			} else {
				results = r.getBestFinalResultNoFiller();
			}
	        outTextFile.write(results+","+wavName+"\n");
	        outTextFile.flush();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
           
    }
	
    /**
     * Prints out the word and its confidence score.
     *
     * @param wr the WordResult to print
     */
    private static void printWordConfidence(WordResult wr) {
        String word = wr.getPronunciation().getWord().getSpelling();
        System.out.print(word);
        
        /* pad spaces between the word and its score */
        int entirePadLength = 10;
        if (word.length() < entirePadLength) {
            for (int i = word.length(); i < entirePadLength; i++) {
                System.out.print(" ");
            }
        }

        System.out.println
                (" (confidence: " +
                        format.format
                                (wr.getLogMath().logToLinear((float) wr.getConfidence())) + ')');
    }


    /**
     * @return the recognizer
     */
    public Recognizer getRecognizer() {
    	return recognizer;
    }

	/**
     * @param recognizer the recognizer to set
     */
    public void setRecognizer(Recognizer recognizer) {
    	this.recognizer = recognizer;
    }

	/**
     * @return the jsgf
     */
    public JSGFGrammar getJsgf() {
    	return jsgf;
    }

	/**
     * @param jsgf the jsgf to set
     */
    public void setJsgf(JSGFGrammar jsgf) {
    	this.jsgf = jsgf;
    }

	/**
     * @return the grammarManager
     */
    public GrammarManager getGrammarManager() {
    	return grammarManager;
    }

	/**
     * @param grammarManager the grammarManager to set
     */
    public void setGrammarManager(GrammarManager grammarManager) {
    	this.grammarManager = grammarManager;
    }

	/**
     * @return the loader
     */
    public Loader getLoader() {
    	return loader;
    }

	/**
     * @param loader the loader to set
     */
    public void setLoader(Loader loader) {
    	this.loader = loader;
    }

	/**
     * @return the scorer
     */
    public AcousticScorer getScorer() {
    	return scorer;
    }

	/**
     * @param scorer the scorer to set
     */
    public void setScorer(AcousticScorer scorer) {
    	this.scorer = scorer;
    }

	/**
     * @return the condfidenceScorer
     */
    public ConfidenceScorer getCondfidenceScorer() {
    	return condfidenceScorer;
    }

	/**
     * @param condfidenceScorer the condfidenceScorer to set
     */
    public void setCondfidenceScorer(ConfidenceScorer condfidenceScorer) {
    	this.condfidenceScorer = condfidenceScorer;
    }

	/**
     * @return the logMath
     */
    public LogMath getLogMath() {
    	return logMath;
    }

	/**
     * @param logMath the logMath to set
     */
    public void setLogMath(LogMath logMath) {
    	this.logMath = logMath;
    }

	/**
     * @return the recordingFilePath
     */
    public String getRecordingFilePath() {
    	return recordingFilePath;
    }

	/**
     * @param recordingFilePath the recordingFilePath to set
     */
    public void setRecordingFilePath(String recordingFilePath) {
    	this.recordingFilePath = recordingFilePath;
    }

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

}
