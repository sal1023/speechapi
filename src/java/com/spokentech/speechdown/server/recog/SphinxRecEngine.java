package com.spokentech.speechdown.server.recog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.server.util.pool.AbstractPoolableObject;

import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.recognizer.RecognizerState;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;


public class SphinxRecEngine extends AbstractPoolableObject implements RecEngine {

    private static Logger _logger = Logger.getLogger(SphinxRecEngine.class);

	private FrontEnd _fe;
    private Recognizer _recognizer;
    private JSGFGrammar _jsgfGrammar;
    private boolean hotword = false;       
	private GrammarManager _grammarManager;
	private ConfigurationManager _cm;
	private int _id;
	private boolean grammarManagerConfigured;
	
    public SphinxRecEngine(ConfigurationManager cm, GrammarManager grammarManager,String prefixId, int id) throws IOException, PropertyException, InstantiationException {

    	_logger.info("Creating a recognizer "+prefixId +"recognizer"+id);
    	_recognizer = (Recognizer) cm.lookup(prefixId+"recognizer"+id);
       
    	//SAL (comment)
    	_recognizer.allocate();
    	

    	if (prefixId.endsWith("lm")) {
    		grammarManagerConfigured = false;
    	} else {
        	grammarManagerConfigured = true;	
	        _jsgfGrammar = (JSGFGrammar) cm.lookup("grammar");
	        _cm = cm;
	        _id = id;
	        _grammarManager = grammarManager;
    	}

        MyStateListener stateListener =  new MyStateListener();
        MyResultListener resultListener = new MyResultListener();
		_recognizer.addResultListener(resultListener);

		_recognizer.addStateListener(stateListener);

		_fe = (FrontEnd)_cm.lookup(prefixId+"frontend"+_id);        
    }
	
	public RecognitionResult recognize(InputStream as, String mimeType, String grammar, int sampleRate, boolean bigEndian, int bytesPerValue, Encoding encoding) {
		_logger.info("Using recognizer # "+_id);
	    //SAL
		//_recognizer.allocate();
        _logger.debug("After allocate" + System.currentTimeMillis());
        
        if (!grammarManagerConfigured)
        	_logger.warn("Can not use a language model sphinx engine to do a grammar recognition request");
        //TODO throw an exception
        
		GrammarLocation grammarLocation = null;
	    try {
	        grammarLocation = _grammarManager.saveGrammar(grammar);
	    } catch (IOException e) {
	        _logger.debug(e, e);
	    }
        
        _logger.debug("After save grammar" + System.currentTimeMillis());
    
	    try {
	        loadJSGF(_jsgfGrammar, grammarLocation);
        } catch (GrammarException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        _logger.debug("After load grammar" + System.currentTimeMillis());
 
	    Result r = doRecognize(as, mimeType, sampleRate, bigEndian, bytesPerValue, encoding);
	    
	    RecognitionResult results = new RecognitionResult(r, _jsgfGrammar.getRuleGrammar());
	    _logger.info("Result: " + (results != null ? results.getText() : null));
	    //SAL
	    //_recognizer.deallocate();
	    return results;
    }

	private Result doRecognize(InputStream as, String mimeType, int sampleRate, boolean bigEndian,
            int bytesPerValue, Encoding encoding) {
	    //TODO: Timers for recog timeout
	
	    // configure the audio input for the recognizer
  		 StreamDataSource dataSource = null;
		 String parts[] = mimeType.split("/");
		 if (parts[1].equals("x-s4audio")) {
			 dataSource = new S4DataStreamDataSource();
		 } else if (parts[1].equals("x-s4feature")) {
			 dataSource = new S4DataStreamDataSource();
		 } else if (parts[1].equals("x-wav")) {
			 dataSource = new AudioStreamDataSource();
		 } else {
			 _logger.warn("Unrecognized mime type: "+mimeType + " Trying to process as audio/x-wav");
			 dataSource = new AudioStreamDataSource();
		 }
	     _logger.debug("-----> "+mimeType+ " "+parts[1]);
		 _fe.setDataSource((DataProcessor) dataSource);
		 dataSource.setInputStream(as, "ws-audiostream", sampleRate, bigEndian, bytesPerValue,encoding);
	    
		_logger.debug("After setting the input stream" + System.currentTimeMillis());
	    
		long  start = System.nanoTime();
	    // decode the audio file.
	    //_logger.debug("Decoding " + audioFileURL);
		Result r = _recognizer.recognize();
		long stop = System.nanoTime();
		long wall = (stop - start)/1000000;
		long streamLen = dataSource.getLengthInMs();
		double ratio = (double)wall/(double)streamLen;
		_logger.info(ratio+ "  Wall time "+ wall+ " stream length "+ streamLen);
	    return r;
    }
    
	
	private String doTranscribe(InputStream as, String mimeType, int sampleRate, boolean bigEndian,
            int bytesPerValue, Encoding encoding, PrintWriter out) {
	    //TODO: Timers for recog timeout
	
	    // configure the audio input for the recognizer
  		 StreamDataSource dataSource = null;
		 String parts[] = mimeType.split("/");
		 if (parts[1].equals("x-s4audio")) {
			 dataSource = new S4DataStreamDataSource();
		 } else if (parts[1].equals("x-s4feature")) {
			 dataSource = new S4DataStreamDataSource();
		 } else if (parts[1].equals("x-wav")) {
			 dataSource = new AudioStreamDataSource();
		 } else {
			 _logger.warn("Unrecognized mime type: "+mimeType + " Trying to process as audio/x-wav");
			 dataSource = new AudioStreamDataSource();
		 }
	     _logger.debug("-----> "+mimeType+ " "+parts[1]);
		 _fe.setDataSource((DataProcessor) dataSource);
		 dataSource.setInputStream(as, "ws-audiostream", sampleRate, bigEndian, bytesPerValue,encoding);
	    
		_logger.debug("After setting the input stream" + System.currentTimeMillis());
	    
	    // decode the audio file.
	    //_logger.debug("Decoding " + audioFileURL);
	    //List<Result> resultList = new ArrayList<Result>();
		String totalResult ="";

		Result result;
        while ((result = _recognizer.recognize())!= null) {

            String resultText = result.getBestResultNoFiller();
            _logger.info(resultText);
            out.println(resultText);
            totalResult = totalResult+resultText;
            //resultList.add(result);
    
        }
	    return totalResult;
    }
	

	/* (non-Javadoc)
     * @see com.spokentech.speechdown.server.recog.RecEngine#recognize(javax.sound.sampled.AudioInputStream, java.lang.String)
     */
	//recognize using a grammar
	public RecognitionResult recognize(AudioInputStream as, String grammar) {
		_logger.info("Using recognizer # "+_id);
	    //SAL
		//_recognizer.allocate();
		GrammarLocation grammarLocation = null;
	    try {
	        grammarLocation = _grammarManager.saveGrammar(grammar);
	    } catch (IOException e) {
	        _logger.debug(e, e);
	    }
	    
	    try {
	        loadJSGF(_jsgfGrammar, grammarLocation);
        } catch (GrammarException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
	
	    //TODO: Timers for recog timeout
	
	    // configure the audio input for the recognizer
		StreamDataSource dataSource = new AudioStreamDataSource();
	    _fe.setDataSource((DataProcessor) dataSource);
        dataSource.setInputStream(as, "ws-audiostream");
  
	    // decode the audio file.
	    //_logger.debug("Decoding " + audioFileURL);
		Result r = _recognizer.recognize();
	    
	    RecognitionResult results = new RecognitionResult(r, _jsgfGrammar.getRuleGrammar());
	    _logger.info("Result: " + (results != null ? results.getText() : null));
	    //SAL
	    //_recognizer.deallocate();
	    return results;
	}
	
	//Recognize using language mode
    public RecognitionResult recognize(InputStream as, String mimeType, int sampleRate, boolean bigEndian, int bytesPerValue, Encoding encoding) {
		_logger.info("Using recognizer # "+_id);
	    //SAL
		//_recognizer.allocate();
        _logger.debug("After allocate" + System.currentTimeMillis());
        
        if (!grammarManagerConfigured)
        	_logger.warn("Can not use a language model sphinx engine to do a grammar recognition request");
        //TODO throw an exception

	    Result r = doRecognize(as, mimeType, sampleRate, bigEndian, bytesPerValue, encoding);

	    _logger.info("Result: " + (r != null ? r.getBestFinalResultNoFiller() : null));
	    //SAL
	    //_recognizer.deallocate();
	    
	    if (r != null) {
	       RecognitionResult results = new RecognitionResult(r.getBestResultNoFiller());
	       return results;
	    } else {
	    	return null;
	    }
    }
    
    
	@Override
    public String transcribe(InputStream as, String mimeType, int sampleRate, boolean bigEndian,
            int bytesPerValue, Encoding encoding, PrintWriter out) {
		
		_logger.info("Using recognizer # "+_id);
	    //SAL
		//_recognizer.allocate();
        _logger.debug("After allocate" + System.currentTimeMillis());
        
        if (!grammarManagerConfigured)
        	_logger.warn("Can not use a language model sphinx engine to do a grammar recognition request");
        //TODO throw an exception

        
	    String r = doTranscribe(as, mimeType, sampleRate, bigEndian, bytesPerValue, encoding, out);

	    //SAL
	    //_recognizer.deallocate();
	    return r;

    }


    
	
	   /**
  * TODOC
  * @param grammarLocation
  * @throws IOException
  * @throws GrammarException
  */
 public synchronized void loadJSGF(JSGFGrammar jsgfGrammar, GrammarLocation grammarLocation) throws IOException, GrammarException {
     jsgfGrammar.setBaseURL(grammarLocation.getBaseURL());
     try {
         jsgfGrammar.loadJSGF(grammarLocation.getGrammarName());
         _logger.debug("loadJSGF(): completed successfully.");
     } catch (com.sun.speech.engine.recognition.TokenMgrError e) {
         _logger.debug("loadJSGF(): encountered exception: " + e.getClass().getName(), e); // com.sun.speech.engine.recognition.TokenMgrError!!!
         String message = e.getMessage();
         /*if (message.indexOf("speech") < 0) {
             throw e;
         }*/
         // else assume caused by GrammarException
         // TODO: edu.cmu.sphinx.jsapi.JSGFGrammar.loadJSGF() should be updated not to swallow GrammarException
         throw new GrammarException(message);
     }
 }
 
 private Result waitForResult(boolean hotword) {
     Result result = null;
     
     _logger.debug("The hotword flag is: "+hotword);
     //if hotword mode, run recognize until a match occurs
     if (hotword) {
         RecognitionResult rr = new RecognitionResult();
         boolean inGrammarResult = false;
         while (!inGrammarResult) {
              result = _recognizer.recognize();

              if (result == null) {
                  _logger.debug("result is null");
              } else {
                  _logger.debug("result is:"+result.toString());
              }
              rr.setNewResult(result, _jsgfGrammar.getRuleGrammar());
              _logger.debug("Rec result: "+rr.toString());
              _logger.debug("text:"+rr.getText()+" matches:"+rr.getRuleMatches()+" oog flag:"+rr.isOutOfGrammar());
              if( (!rr.getRuleMatches().isEmpty()) && (!rr.isOutOfGrammar())) {
                  inGrammarResult = true;
              }
         }
      
     //if not hotword, just run recognize once
     } else {
          result = _recognizer.recognize();
     }
     return result;

 }
 


    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#activate()
     */
    @Override
    public synchronized void activate() {
        _logger.info("SphinxRecEngine activating...");
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#passivate()
     */
    @Override
    public synchronized void passivate() {
        _logger.info("SphinxRecEngine passivating...");
        //stopProcessing();
        //_recogListener = null;
    }


    /**
     * TODOC
     * @param text
     * @param ruleName
     * @return
     * @throws GrammarException
     */
    public synchronized RuleParse parse(String text, String ruleName) throws GrammarException {
        
        RuleGrammar ruleGrammar = _jsgfGrammar.getRuleGrammar();
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

		public void statusChanged(RecognizerState arg0) {
	        _logger.debug("Recognizer Status changed to "+arg0.toString() +" "+System.currentTimeMillis());
	        
        }

		public void newProperties(PropertySheet arg0) throws PropertyException {
	        _logger.debug("StateListener New properties called");
	        
        }
    	
    }
    private class MyResultListener implements ResultListener {

		public void newResult(Result arg0) {
			_logger.info("best final result: "+arg0.getBestFinalResultNoFiller());
			_logger.info("best pronuciation: "+arg0.getBestPronunciationResult());
			_logger.info("Frame "+arg0.getStartFrame()+ " to "+arg0.getEndFrame()+"("+arg0.getFrameNumber()+")");
	        
        }

		public void newProperties(PropertySheet arg0) throws PropertyException {
	        _logger.info("ResultListener New properties called");
	        
        }
    	
    }


}
