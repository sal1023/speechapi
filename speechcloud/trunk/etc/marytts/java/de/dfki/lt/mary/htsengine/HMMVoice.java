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


package de.dfki.lt.mary.htsengine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;

import org.apache.log4j.Logger;

import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.modules.synthesis.WaveformSynthesizer;
import de.dfki.lt.mary.modules.synthesis.Voice.Gender;

public class HMMVoice extends Voice {
 
    private HMMData htsData = new HMMData();
    private Logger logger = Logger.getLogger("HMMVoice");
    
   /** 
    * constructor */ 
   public HMMVoice(String[] nameArray, Locale locale, 
           AudioFormat dbAudioFormat, WaveformSynthesizer synthesizer, 
           Gender gender, int topStart, int topEnd, int baseStart, int baseEnd,
           String Ftd, String Ftf, String Ftm, String Fts, String Fta, 
           String Fmd, String Fmf, String Fmm, String Fms, String Fma,
           boolean useGV, String Fgvf, String Fgvm, String Fgvs, String Fgva,
           String FeaList, String Flab, String Fif, int nFilters, int norderFilters) throws Exception {
       super(nameArray, locale, dbAudioFormat, synthesizer, gender, topStart, topEnd, baseStart, baseEnd);

       this.htsData.setTreeDurFile(Ftd);  
       this.htsData.setTreeLf0File(Ftf);           
       this.htsData.setTreeMcpFile(Ftm);
       this.htsData.setTreeStrFile(Fts);
       this.htsData.setTreeMagFile(Fta);

       this.htsData.setPdfDurFile(Fmd);
       this.htsData.setPdfLf0File(Fmf);        
       this.htsData.setPdfMcpFile(Fmm);
       this.htsData.setPdfStrFile(Fms);
       this.htsData.setPdfMagFile(Fma);

       this.htsData.setUseGV(useGV);
       this.htsData.setPdfLf0GVFile(Fgvf);        
       this.htsData.setPdfMcpGVFile(Fgvm);
       this.htsData.setPdfStrGVFile(Fgvs);
       this.htsData.setPdfMagGVFile(Fgva);
       
       /* Feature list file */
       this.htsData.setFeaListFile(FeaList);

       /* Example context feature file in HTSCONTEXT_EN format */
       this.htsData.setLabFile(Flab);

       /* Configuration for mixed excitation */
       this.htsData.setMixFiltersFile(Fif); 
       this.htsData.setNumFilters(nFilters);
       this.htsData.setOrderFilters(norderFilters);

       /* Load ModelSet ts */
       logger.info("Loading Model Set:");
       this.htsData.loadModelSet(); 
       
       /* Load TreeSet ms*/
       logger.info("Loading Tree Set:");
       this.htsData.loadTreeSet();  
       
       /* Load GV ModelSet gv*/
       logger.info("Loading GV Model Set:");
       this.htsData.loadGVModelSet();
       
       /* Load (un-commented) context feature list from featureListFile */
       logger.info("Loading Context feature list:");      
       this.htsData.readFeatureList();

       if( getFeatureList().size() == 0)
          logger.debug("Warning feature list file empty or feature list not loaded. ");
            

   }
   
   public HMMData getHMMData(){ return this.htsData; }
   
   public Vector<String> getFeatureList(){ return this.htsData.getFeatureList(); }
    
   /* set parameters for generation: f0Std, f0Mean and length, default values 1.0, 0.0 and 0.0 */
   /* take the values from audio effects component through a MaryData object */
   public void setF0Std(double dval) { htsData.setF0Std(dval); }
   public void setF0Mean(double dval) { htsData.setF0Mean(dval); }
   public void setLength(double dval) { htsData.setLength(dval); }
   public void setDurationScale(double dval) { htsData.setDurationScale(dval); }

} /* class HMMVoice */
