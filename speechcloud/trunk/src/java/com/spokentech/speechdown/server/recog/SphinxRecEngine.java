package com.spokentech.speechdown.server.recog;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.RecognitionResult;
import com.spokentech.speechdown.server.util.pool.AbstractPoolableObject;

import edu.cmu.sphinx.decoder.ResultListener;
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
	private StreamDataSource _dataSource;
    private Recognizer _recognizer;
    private JSGFGrammar _jsgfGrammar;
    private boolean hotword = false;       
	private GrammarManager _grammarManager;
	private ConfigurationManager _cm;
	private int _id;

	
    public SphinxRecEngine(ConfigurationManager cm, GrammarManager grammarManager,String prefixId, int id) throws IOException, PropertyException, InstantiationException {

    	_logger.info("Creating a recognizer "+prefixId +"recognizer"+id);
    	_recognizer = (Recognizer) cm.lookup(prefixId+"recognizer"+id);
        _recognizer.allocate();
        _jsgfGrammar = (JSGFGrammar) cm.lookup("grammar");
        _cm = cm;
        _id = id;
        _grammarManager = grammarManager;

        MyStateListener stateListener =  new MyStateListener();
        MyResultListener resultListener = new MyResultListener();
		_recognizer.addResultListener(resultListener);

		_recognizer.addStateListener(stateListener);

		_dataSource = (StreamDataSource) _cm.lookup(prefixId+"streamDataSource"+_id);        
    }
	
	public RecognitionResult recognize(InputStream as, String grammar, int sampleRate, boolean bigEndian, int bytesPerValue, Encoding encoding) {
		_logger.info("Using recognizer # "+_id);
		//_recognizer.allocate();
        
        _logger.info("After allocate" + System.currentTimeMillis());
		GrammarLocation grammarLocation = null;
	    try {
	        grammarLocation = _grammarManager.saveGrammar(grammar);
	    } catch (IOException e) {
	        _logger.debug(e, e);
	    }
        
        _logger.info("After save grammar" + System.currentTimeMillis());
	
	    
	    try {
	        loadJSGF(_jsgfGrammar, grammarLocation);
        } catch (GrammarException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        
        _logger.info("After load grammar" + System.currentTimeMillis());
	
	    //TODO: Timers for recog timeout
	
	    // configure the audio input for the recognizer
  	
		_dataSource.setInputStream(as, "ws-audiostream", sampleRate, bigEndian, bytesPerValue,encoding);
	    
		_logger.info("After setting the input stream" + System.currentTimeMillis());
	    
	    // decode the audio file.
	    //System.out.println("Decoding " + audioFileURL);
	    RecognitionResult results = waitForResult(false);
	    
	
	    System.out.println("Result: " + (results != null ? results.getText() : null));
	    //_recognizer.deallocate();
	    return results;
    }
    

	/* (non-Javadoc)
     * @see com.spokentech.speechdown.server.recog.RecEngine#recognize(javax.sound.sampled.AudioInputStream, java.lang.String)
     */
	public RecognitionResult recognize(AudioInputStream as, String grammar) {
		_logger.info("Using recognizer # "+_id);
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
       _dataSource.setInputStream(as, "ws-audiostream");
  
	    // decode the audio file.
	    //System.out.println("Decoding " + audioFileURL);
	    RecognitionResult results = waitForResult(false);
	    
	
	    System.out.println("Result: " + (results != null ? results.getText() : null));
	    //_recognizer.deallocate();
	    return results;
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
 
 private RecognitionResult waitForResult(boolean hotword) {
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
     //stopProcessing();
     //if (result != null) {
     //    Result result2clear = _recognizer.recognize();
     //    if (result2clear != null) {
     //        _logger.info("waitForResult(): result2clear not null!");
     //    }
    // } else {
    //     _logger.info("waitForResult(): got null result from recognizer!");
     //    return null;
     //}
     return new RecognitionResult(result, _jsgfGrammar.getRuleGrammar());

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
	        _logger.info("Recognizer Status changed to "+arg0.toString() +" "+System.currentTimeMillis());
	        
        }

		public void newProperties(PropertySheet arg0) throws PropertyException {
	        _logger.info("StateListener New properties called");
	        
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
