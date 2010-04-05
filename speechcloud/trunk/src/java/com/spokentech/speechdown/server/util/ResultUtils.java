package com.spokentech.speechdown.server.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.common.WordData;
import com.spokentech.speechdown.common.rule.RuleMatch;
import com.spokentech.speechdown.common.rule.SimpleNLRuleHandler;
import com.spokentech.speechdown.server.recog.SphinxRecEngine;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.WordSearchState;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.result.ConfidenceResult;
import edu.cmu.sphinx.result.ConfidenceScorer;
import edu.cmu.sphinx.result.Path;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.LogMath;


public class ResultUtils {
	
    private static DecimalFormat format = new DecimalFormat("#.#####");
    private static Logger _logger = Logger.getLogger(SphinxRecEngine.class);

    protected final static String OUTOFGRAMMAR = "<unk>";
    
    private static LogMath logm = null;
    

	/**
     * @param logm the logm to set
     */
    public static void setLogm(LogMath logm) {
    	ResultUtils.logm = logm;
    }

	/**
     * Returns the string of words (with timestamp) for this token.
     *
     * @param wantFiller     true if we want filler words included, false otherwise
     * @param wordTokenFirst true if the word tokens come before other types of tokens
	 * @param cs 
     * @return the string of words
     */
    public static Utterance getAllResults(Result r,
    		                            boolean wantFiller,
                                        boolean wordTokenFirst,
                                        RuleGrammar ruleGrammar) {
        Token token = r.getBestToken();
        Utterance u;
        if (token == null) {
            return null;
        } else {
            if (wordTokenFirst) {
                u = getTimedWordPath(token, wantFiller);
            } else {
                u = getTimedWordTokenLastPath(token, wantFiller);
            }
             
            if (u.getText().equals(OUTOFGRAMMAR)) {
                u.setOog(true);
            }
            if (u.getText() != null  && ruleGrammar != null && !u.isOog()) {
                try {
                    RuleParse ruleParse = ruleGrammar.parse(u.getText(), null);
                    u.setRuleMatches(SimpleNLRuleHandler.getRuleMatches(ruleParse));
                } catch (GrammarException e) {
                    _logger.info("GrammarException encountered! "+ e.getLocalizedMessage());
                }
            }

			Collections.reverse(u.getWords());
 	
        	return u;
        }
    }
    

    
    
    
	   /**
     * Returns the string of words (with timestamp) for this token.
     *
     * @param wantFiller     true if we want filler words included, false otherwise
     * @param wordTokenFirst true if the word tokens come before other types of tokens
     * @param cs 
     * @return the string of words
     */
    public static Utterance getAllResults(Result r,
    		boolean wantFiller,
    		boolean wordTokenFirst, ConfidenceScorer cs) {
    	Token token = r.getBestToken();
    	Utterance u;
    	if (token == null) {
    		return null;
    	} else {

    		if (wordTokenFirst) {
    			u = getTimedWordPath(token, wantFiller);
    		} else {
    			u = getTimedWordTokenLastPath(token, wantFiller);
    		}

    		//need to reverse the words (sphinx gives them in reverse order)
			Collections.reverse(u.getWords());
    		
    		// add the confidence data to the utterance
    		if (cs!=null) {
    			ConfidenceResult cr = cs.score(r);
    			Path best = cr.getBestHypothesis();				
    			//LogMath lm = best.getLogMath();
    			
    			double confidence = best.getLogMath().logToLinear((float) best.getConfidence());
    			u.setConfidence(confidence);
    			
    			WordResult[]  words = best.getWords();
    			for (WordResult wr : words) {
    				printWordConfidence(wr);
    			}

    			List l = u.getWords();
    			int c = 0;
    			_logger.debug(words.length+"/"+u.getWords().size());
    			
    			//loop thru the confidence words
    			for (WordResult wr : words) {
    				if(!wr.isFiller()) {
    					WordData x = u.getWords().get(c);

    					x.setConfidence(wr.getLogMath().logToLinear((float) wr.getConfidence())) ;
    	
    					_logger.debug(c+": "+x.toString());
    					_logger.debug(c+" , "+ wr.getPronunciation().getWord().getSpelling()+" / "+u.getWords().get(c).getWord());
    					c++;
    				}
    			}
    		}

    		return u;
    	}
    }
    
    
    /**
     * Returns the string of words (with timestamp) for this token. This method assumes that the word tokens come before
     * other types of token.
     *
     * @param wantFiller true if we want filler words, false otherwise
     * @return the string of words
     */
    private static Utterance getTimedWordPath(Token token, boolean wantFiller) {
        //StringBuilder sb = new StringBuilder();
    	Utterance u = new Utterance();
        List<WordData> words = new ArrayList<WordData>();
       	u.setWords(words);
    	u.setText(token.getWordPathNoFiller());
    	
        // get to the first emitting token
        while (token != null && !token.isEmitting()) {
            token = token.getPredecessor();
        }

        if (token != null) {
            Data lastWordFirstFeature = token.getData();
            Data lastFeature = lastWordFirstFeature;
            token = token.getPredecessor();

            while (token != null) {
                if (token.isWord()) {
                    Word word = token.getWord();
                    if (wantFiller || !word.isFiller()) {
                        WordData w =addWord(token, word, wantFiller,
                                (FloatData) lastFeature,
                                (FloatData) lastWordFirstFeature);
                        words.add(w);
                    }
                    lastWordFirstFeature = lastFeature;
                }
                Data feature = token.getData();
                if (feature != null) {
                    lastFeature = feature;
                }
                token = token.getPredecessor();
            }
        }
        return u;
    }


    /**
     * Returns the string of words for this token, each with the starting sample number as the timestamp. This method
     * assumes that the word tokens come after the unit and hmm tokens.
     *
     * @return the string of words, each with the starting sample number
     */
    private static Utterance getTimedWordTokenLastPath(Token token, boolean wantFiller) {
       	Utterance u = new Utterance();
        List<WordData> words = new ArrayList<WordData>();
       	u.setWords(words);
    	u.setText(token.getWordPathNoFiller());
    	
        Word word = null;
        Data lastFeature = null;
        Data lastWordFirstFeature = null;
        Token wtoken = token;
        while (token != null) {
            if (token.isWord()) {
                if (word != null) {
                	//_logger.debug("*** "+token.getWord().getSpelling() + " "+
                	//			  logm.logToLinear( (float)(token.getAcousticScore() )) + " "+
                	//			  logm.logToLinear( (float)(token.getLanguageScore() ))+ " "+
                	//			  logm.logToLinear( (float)(token.getScore() )) );

                	
                    if (wantFiller || !word.isFiller()) {
                        WordData w = addWord(wtoken, word,wantFiller,
                                (FloatData) lastFeature,
                                (FloatData) lastWordFirstFeature);
                        words.add(w);
                    }
                    word = token.getWord();
                    lastWordFirstFeature = lastFeature;
                }
                wtoken =token;
                word = token.getWord();
            }
            Data feature = token.getData();
            if (feature != null) {
                lastFeature = feature;
                if (lastWordFirstFeature == null) {
                    lastWordFirstFeature = lastFeature;
                }
            }
            token = token.getPredecessor();
        }

        return u;
    }
    
    
    /**
     * Adds the given word into the given string builder with the start and end times from the given features.
     *
     * @param sb           the StringBuilder into which the word is added
     * @param word         the word to add
     * @param startFeature the starting feature
     * @param endFeature   tne ending feature
     */
    private static WordData addWord(Token token, Word word, boolean wantFiller,
                         FloatData startFeature, FloatData endFeature) {
    	
    	
        float startTime = startFeature == null ? -1 : ((float) startFeature.getFirstSampleNumber() /
                startFeature.getSampleRate());
        float endTime = endFeature == null ? -1 : ((float) endFeature.getFirstSampleNumber() /
                endFeature.getSampleRate());

        
        
        WordData w = new WordData();
        w.setStartTime(startTime);
        w.setStopTime(endTime);
        w.setWord(word.getSpelling());
        w.setPronunciation(getPronunciation(token,wantFiller));
       _logger.debug(w.toString());
        return w;
    }
    
    
    
    
    
    
    
    
    /**
     * Returns the string of words leading up to this token. (***FROM TOKEN***)
     *
     * @param wantFiller         if true, filler words are added
     * @param wantPronunciations if true append [ phoneme phoneme ... ] after each word
     * @return the word path
     */
    public static String getPronunciation(Token token, boolean wantFiller) {
            StringBuilder sb = new StringBuilder();
            if (token.isWord()) {
                WordSearchState wordState =
                        (WordSearchState) token.getSearchState();
                Pronunciation pron = wordState.getPronunciation();
                Word word = wordState.getPronunciation().getWord();

//                System.out.println(token.getFrameNumber() + " " + word + " " + token.logLanguageScore + " " + token.logAcousticScore);

                if (wantFiller || !word.isFiller()) {

                        sb.insert(0, ']');
                        Unit[] u = pron.getUnits();
                        for (int i = u.length - 1; i >= 0; i--) {
                            if (i < u.length - 1) sb.insert(0, ',');
                            sb.insert(0, u[i].getName());
                        }
                        sb.insert(0, '[');
                    //sb.insert(0, word.getSpelling());
                    //sb.insert(0, ' ');
                }
            }
            return sb.toString();
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

}
