
/**   
*           The HMM-Based Speech Synthesis System (HTS)             
*                       HTS Working Group                           
*                                                                   
*                  Department of Computer Science                   
*                  Nagoya Institute of Technology                   
*                               and                                 
*   Interdisciplinary Graduate School of Science and Engineering    
*                  Tokyo Institute of Technology                    
*                                                                   
*                Portions Copyright (c) 2001-2006                       
*                       All Rights Reserved.
*                         
*              Portions Copyright 2000-2007 DFKI GmbH.
*                      All Rights Reserved.                  
*                                                                   
*  Permission is hereby granted, free of charge, to use and         
*  distribute this software and its documentation without           
*  restriction, including without limitation the rights to use,     
*  copy, modify, merge, publish, distribute, sublicense, and/or     
*  sell copies of this work, and to permit persons to whom this     
*  work is furnished to do so, subject to the following conditions: 
*                                                                   
*    1. The source code must retain the above copyright notice,     
*       this list of conditions and the following disclaimer.       
*                                                                   
*    2. Any modifications to the source code must be clearly        
*       marked as such.                                             
*                                                                   
*    3. Redistributions in binary form must reproduce the above     
*       copyright notice, this list of conditions and the           
*       following disclaimer in the documentation and/or other      
*       materials provided with the distribution.  Otherwise, one   
*       must contact the HTS working group.                         
*                                                                   
*  NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF TECHNOLOGY,   
*  HTS WORKING GROUP, AND THE CONTRIBUTORS TO THIS WORK DISCLAIM    
*  ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL       
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   
*  SHALL NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF         
*  TECHNOLOGY, HTS WORKING GROUP, NOR THE CONTRIBUTORS BE LIABLE    
*  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY        
*  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,  
*  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTUOUS   
*  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR          
*  PERFORMANCE OF THIS SOFTWARE.                                    
*                                                                   
*/

package de.dfki.lt.mary.modules;

import de.dfki.lt.mary.htsengine.HMMData;
import de.dfki.lt.mary.htsengine.HTSModel;
import de.dfki.lt.mary.htsengine.HTSModelSet;
import de.dfki.lt.mary.htsengine.HTSParameterGeneration;
import de.dfki.lt.mary.htsengine.HTSTree;
import de.dfki.lt.mary.htsengine.HTSTreeSet;
import de.dfki.lt.mary.htsengine.HTSUttModel;
import de.dfki.lt.mary.htsengine.HTSVocoder;
import de.dfki.lt.mary.htsengine.HMMVoice;

import de.dfki.lt.mary.modules.synthesis.Voice;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat;

import org.apache.log4j.Logger;
import org.jsresources.AppendableSequenceAudioInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.InternalModule;
import de.dfki.lt.mary.tests.AllTests;
import de.dfki.lt.mary.util.dom.NameNodeFilter;
import de.dfki.lt.signalproc.util.AudioPlayer;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.NoiseDoubleDataSource;


/**
 * HTSEngine: a compact HMM-based speech synthesis engine.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marc Schr&ouml;der, Marcela Charfuelan 
 */
public class HTSEngine extends InternalModule
{
    private Logger logger = Logger.getLogger("HTSEngine");
    
    public HTSEngine()
    {
        super("HTSEngine",
              MaryDataType.get("HTSCONTEXT"),
              MaryDataType.get("AUDIO")
              );
    }

    /**
     * This module is actually tested as part of the HMMSynthesizer test,
     * for which reason this method does nothing.
     */
    public synchronized void powerOnSelfTest() throws Error
    {
    }
    
    
    /**
     * when calling this function HMMVoice must be initialised already.
     * that is TreeSet and ModelSet must be loaded already.
     * @param d
     * @return
     * @throws Exception
     */
    public MaryData process(MaryData d)
    throws Exception
    {
        /** The utterance model, um, is a Vector (or linked list) of Model objects. 
         * It will contain the list of models for current label file. */
        HTSUttModel um = new HTSUttModel();
        HTSParameterGeneration pdf2par = new HTSParameterGeneration();
        HTSVocoder par2speech = new HTSVocoder();
        AudioInputStream ais;
              
        Voice v = d.getDefaultVoice(); /* This is the way of getting a Voice through a MaryData type */
        assert v instanceof HMMVoice;
        HMMVoice hmmv = (HMMVoice)v;
              
        String context = d.getPlainText();
        
        /* Process label file of Mary context features and creates UttModel um */
        processUtt(context, um, hmmv.getHMMData());

        /* Process UttModel */
        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
        pdf2par.htsMaximumLikelihoodParameterGeneration(um, hmmv.getHMMData());
    
        /* Process generated parameters */
        /* Synthesize speech waveform, generate speech out of sequence of parameters */
        ais = par2speech.htsMLSAVocoder(pdf2par, hmmv.getHMMData());
       
        MaryData output = new MaryData(outputType());
        if (d.getAudioFileFormat() != null) {
            output.setAudioFileFormat(d.getAudioFileFormat());
            if (d.getAudio() != null) {
               // This (empty) AppendableSequenceAudioInputStream object allows a 
               // thread reading the audio data on the other "end" to get to our data as we are producing it.
                assert d.getAudio() instanceof AppendableSequenceAudioInputStream;
                output.setAudio(d.getAudio());
            }
        }     
       output.appendAudio(ais);
       
       /* include correct durations in MaryData output */
       output.setPlainText(um.getRealisedAcoustParams());
              
       return output;
        
    }
   
    /* For stand alone testing. */
    public AudioInputStream processStr(String context, HMMData htsData)
    throws Exception
    {
        HTSUttModel um = new HTSUttModel();
        HTSParameterGeneration pdf2par = new HTSParameterGeneration();
        HTSVocoder par2speech = new HTSVocoder();
        AudioInputStream ais;
        
        /* htsData contains:
         * data in the configuration file, .pdf file names and other parameters. 
         * After InitHMMData it contains TreeSet ts and ModelSet ms 
         * ModelSet: Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
         *           these are all the HMMs trained for a particular voice 
         * TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
         *          these are all the trees trained for a particular voice. */
 
        
        logger.info("CONTEXT:" + context);
        
        /* Process label file of Mary context features and creates UttModel um */
        processUtt(context, um, htsData);

        /* Process UttModel */
        /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
        pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);
    
        /* Process generated parameters */
        /* Synthesize speech waveform, generate speech out of sequence of parameters */
        ais = par2speech.htsMLSAVocoder(pdf2par, htsData);
        
       return ais;
        
    }
  
 
    
    /** Reads the Label file, the file which contains the Mary context features,
     *  creates an scanner object and calls _ProcessUtt
     * @param LabFile
     */
    public void processUttFromFile(String LabFile, HTSUttModel um, HMMData htsData){ 
        Scanner s = null;
        try {    
            /* parse text in label file */
            s = new Scanner(new BufferedReader(new FileReader(LabFile)));
            _processUtt(s,um,htsData,htsData.getTreeSet(),htsData.getModelSet());
              
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
            
        } finally {
            if (s != null)
                s.close();
        }           
    }
    
    /** Creates a scanner object with the Mary context features contained in Labtext
     * and calls _ProcessUtt
     * @param LabText
     */
    public void processUtt(String LabText, HTSUttModel um, HMMData htsData) {
        Scanner s = null;
        try {
          s = new Scanner(LabText);
         _processUtt(s, um, htsData, htsData.getTreeSet(),htsData.getModelSet());
        } finally {
            if (s != null)
              s.close();
        }   
    }
    

    
    /** Parse Mary context features. 
     * For each triphone model in the file, it creates a Model object in a linked list of 
     * Model objects -> UttModel um 
     * It also estimates state duration from state duration model (Gaussian).
     * For each model in the vector, the mean and variance of the DUR, LF0, MCP, STR and MAG 
     * are searched in the ModelSet and copied in each triphone model.   */
    private void _processUtt(Scanner s, HTSUttModel um, HMMData htsData, HTSTreeSet ts, HTSModelSet ms){     
        int i, mstate,frame;
        HTSModel m;                   /* current model, corresponds to a line in label file */
        String nextLine;
        double diffdurOld = 0.0;
        double diffdurNew = 0.0;
        HTSTree auxTree;
        float fperiodmillisec = ((float)htsData.getFperiod() / (float)htsData.getRate()) * 1000;
        Integer dur;
        boolean firstPh = true; 
        boolean lastPh = false;
        
        /* parse text */
        i=0;
        while (s.hasNext()) {
            nextLine = s.next();
            //System.out.println("STR: " + nextLine);
            um.addUttModel(new HTSModel(ms));            

            m = um.getUttModel(i);
            /* this function also sets the phoneme name, the phoneme between - and + */
            m.setName(nextLine);  
            
            if(!(s.hasNext()) )
              lastPh = true;

            /* Estimate state duration from state duration model (Gaussian) 
             * 1. find the index idx of the durpdf corresponding (or that best match in the tree) 
             *    to the triphone+context features in nextLine. 
             * NOTE 1: the indexes in the tree.inf file start in 1 ex. dur_s2_1, but here are stored 
             * in durpdf[i][j] array which starts in i=0, so when finding this dur pdf, the idx should 
             * be idx-1 !!!
             * 2. Calculate duration using the pdf idx found in the tree, function: FindDurPDF */
            auxTree = ts.getTreeHead(HMMData.DUR);
            m.setDurPdf( ts.searchTree(nextLine, auxTree.getRoot(), false));

            //System.out.println("dur->pdf=" + m.getDurPdf());

            if (htsData.getLength() == 0.0 ) {
                diffdurNew = ms.findDurPdf(m, firstPh, lastPh, htsData.getRho(), diffdurOld, htsData.getDurationScale());
                m.setTotalDurMillisec((int)(fperiodmillisec * m.getTotalDur()));                
                diffdurOld = diffdurNew;
                um.setTotalFrame(um.getTotalFrame() + m.getTotalDur());
                dur = m.getTotalDurMillisec();
                um.concatRealisedAcoustParams(m.getPhoneName() + " " + dur.toString() + "\n");           
            } /* else : when total length of generated speech is specified */
            /* Not implemented yet...*/
            //m.printDuration(ms.getNumState());

            /* Find pdf for LF0 */               
            for(auxTree=ts.getTreeHead(HMMData.LF0), mstate=0; auxTree != ts.getTreeTail(HMMData.LF0); auxTree=auxTree.getNext(), mstate++ ) {           
                m.setLf0Pdf(mstate, ts.searchTree(nextLine,auxTree.getRoot(),false));
                //System.out.println("lf0pdf[" + mstate + "]=" + m.getLf0Pdf(mstate));
                ms.findLf0Pdf(mstate, m, htsData.getUV());
            }

            /* Find pdf for MCP */
            for(auxTree=ts.getTreeHead(HMMData.MCP), mstate=0; auxTree != ts.getTreeTail(HMMData.MCP); auxTree=auxTree.getNext(), mstate++ ) {           
                m.setMcepPdf(mstate, ts.searchTree(nextLine,auxTree.getRoot(),false));
                //System.out.println("mceppdf[" + mstate + "]=" + m.getMcepPdf(mstate));
                ms.findMcpPdf(mstate, m);
            }              

            /* Find pdf for strengths */
            /* If there is no STRs then auxTree=null=ts.getTreeTail so it will not try to find pdf for strengths */
            for(auxTree=ts.getTreeHead(HMMData.STR), mstate=0; auxTree != ts.getTreeTail(HMMData.STR); auxTree=auxTree.getNext(), mstate++ ) {           
                m.setStrPdf(mstate, ts.searchTree(nextLine,auxTree.getRoot(),false));
                //System.out.println("strpdf[" + mstate + "]=" + m.getStrPdf(mstate));
                ms.findStrPdf(mstate, m);                    
            }

            /* Find pdf for Fourier magnitudes */
            /* If there is no MAGs then auxTree=null=ts.getTreeTail so it will not try to find pdf for Fourier magnitudes */
            for(auxTree=ts.getTreeHead(HMMData.MAG), mstate=0; auxTree != ts.getTreeTail(HMMData.MAG); auxTree=auxTree.getNext(), mstate++ ) {           
                m.setMagPdf(mstate, ts.searchTree(nextLine,auxTree.getRoot(),false));
                //System.out.println("magpdf[" + mstate + "]=" + m.getMagPdf(mstate));
                ms.findMagPdf(mstate, m);
            }

            //System.out.println();
            /* increment number of models in utterance model */
            um.setNumModel(um.getNumModel()+1);
            /* update number of states */
            um.setNumState(um.getNumState() + ms.getNumState());
            i++;
            
            if(firstPh)
              firstPh = false;
        }

        for(i=0; i<um.getNumUttModel(); i++){
            m = um.getUttModel(i);                  
            for(mstate=0; mstate<ms.getNumState(); mstate++)
                for(frame=0; frame<m.getDur(mstate); frame++) 
                    if(m.getVoiced(mstate))
                        um.setLf0Frame(um.getLf0Frame() +1);
            //System.out.println("Vector m[" + i + "]=" + m.getName()); 
        }

        logger.info("Number of models in sentence numModel=" + um.getNumModel() + "  Total number of states numState=" + um.getNumState());
        logger.info("Total number of frames=" + um.getTotalFrame() + "  Number of voiced frames=" + um.getLf0Frame());    

        
    } /* method _ProcessUtt */

    
    
    
    /** 
     * Stand alone testing using an HTSCONTEXT file as input. 
     * @param args
     * @throws IOException
     * * to run the jmp profiler add to run VM arguments: -Xrunjmp:noobjects,nomonitors  */
    public static void main(String[] args) throws IOException, InterruptedException, Exception {
      
      /* configure log info */
      org.apache.log4j.BasicConfigurator.configure();

      /* To run the stand alone version of HTSEngine, it is necessary to pass a configuration
       * file. It can be one of the hmm configuration files in MARY_BASE/conf/*hmm*.config 
       * The input for creating a sound file is a label file in HTSCONTEXT format, there
       * is an example indicated in the configuration file as well, if one wants to 
       * change this file example, use the MARY system to generate a HTSCONTEX file for whatever
       * text and saved in a file.
       * The output sound file is located in MARY_BASE/tmp/tmp.wav */
      HTSEngine hmm_tts = new HTSEngine();
      
      /* htsData contains:
       * Data in the configuration file, .pdf, tree-xxx.inf file names and other parameters. 
       * After initHMMData it contains TreeSet ts and ModelSet ms 
       * ModelSet: Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
       *           these are all the HMMs trained for a particular voice 
       * TreeSet: Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
       *          these are all the trees trained for a particular voice. */
      HMMData htsData = new HMMData();
      
      /* For initialise provide the name of the hmm voice and the name of its configuration file,
       * also indicate the name of your MARY_BASE directory.*/
      String voiceName = "hmm-slt";
      String configName = "english-hmm-slt.config";
      String MaryBase = "/project/mary/marcela/MARY TTS/";
      htsData.initHMMData(voiceName, MaryBase, configName);
      
      /** The utterance model, um, is a Vector (or linked list) of Model objects. 
       * It will contain the list of models for current label file. */
      HTSUttModel um = new HTSUttModel();
      HTSParameterGeneration pdf2par = new HTSParameterGeneration();
      HTSVocoder par2speech = new HTSVocoder();
      AudioInputStream ais, ais_aux;
                
      /** Example of HTSCONTEXT context features file */
      String feaFile = htsData.getLabFile();
      
      /* Process label file of Mary context features and creates UttModel um, a linked             
       * list of all the models in the utterance. For each model, it searches in each tree, dur,   
       * cmp, etc, the pdf index that corresponds to a triphone context feature and with           
       * that index retrieves from the ModelSet the mean and variance for each state of the HMM.   */
      hmm_tts.processUttFromFile(feaFile, um, htsData);


      /* Generate sequence of speech parameter vectors, generate parameters out of sequence of pdf's */     
      pdf2par.htsMaximumLikelihoodParameterGeneration(um, htsData);

      /* Synthesize speech waveform, generate speech out of sequence of parameters */
      ais = par2speech.htsMLSAVocoder(pdf2par, htsData);
          
      String fileOutName = MaryBase + "tmp/tmp.wav";  
          
      File fileOut = new File(fileOutName);
      System.out.println("saving to file: " + fileOutName);
            
          
      if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE,ais)) {
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, fileOut);
      }

      System.out.println("Calling audioplayer:");
      AudioPlayer player = new AudioPlayer(fileOut);
      player.start();  
      player.join();
      System.out.println("audioplayer finished...");
                 
    }  /* main method */

}  /* class HTSEngine*/

