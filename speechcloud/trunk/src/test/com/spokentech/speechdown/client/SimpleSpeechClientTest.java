/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.client;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

import org.apache.log4j.Logger;




import junit.framework.TestCase;

public class SimpleSpeechClientTest extends TestCase {

    private static Logger _logger = Logger.getLogger(SimpleSpeechClientTest.class);
    
    		SimpleSpeechClient ssc = new SimpleSpeechClient();
    		
    		
    		String grammar = "#JSGF V1.0;\n grammar example;\n public <main> = [ <pre> ] ( <weather> {WEATHER} | <sports>  {SPORTS} | <stocks> {STOCKS} ) ;\n <pre> = ( I would like [ to hear ] ) | ( hear ) | ( [ please ] get [ me ] ) | ( look up );\n <weather> = [ the ] weather;\n <sports> = sports [ news ];\n <stocks> = ( [ a ] stock ( quote | quotes ) ) | stocks;";


    		
    		String grammar2 = "#JSGF V1.0;\n" +
    				"grammar smashup; public <command> = (<link>); \n" +
    				"<link> = ( Images {http://www.google.com/}|\n " +
    				"Maps {http://maps.google.com/maps?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=wl}|\n " +
    				"News {http://news.google.com/nwshp?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=wn}|\n " +
    				"Shopping {http://www.google.com/prdhp?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=wf}| \n" +
    				"more {http://www.google.com/intl/en/options/}| \n" +
    				"Books {http://books.google.com/bkshp?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=wp}|\n " +
    				"Finance {http://www.google.com/finance?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=we}|\n " +
    				"Translate {http://translate.google.com/?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=wT}|\n " +
    				"Scholar {http://scholar.google.com/schhp?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=ws}|\n " +
    				"Blogs {http://blogsearch.google.com/?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=wb}|\n " +
    				"Updates {http://www.google.com/realtime?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=wY}|\n " +
    				"YouTube {http://www.youtube.com/?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=w1}|\n " +
    				"Calendar {http://www.google.com/calendar/render?hl=en&tab=wc}|\n " +
    				"Photos {http://picasaweb.google.com/home?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=wq}|\n " +
    				"Documents {http://docs.google.com/?hl=en&tab=wo}|\n Reader {http://www.google.com/reader/?hl=en&tab=wy}|\n " +
    				"Sites {http://sites.google.com/?hl=en&tab=w3}|\n " +
    				"Groups {http://groups.google.com/grphp?client=firefox-a&rls=org.mozilla:en-US:official&hl=en&tab=wg}|\n " +
    				"even more {http://www.google.com/intl/en/options/}|\n Advanced Search {http://www.google.com/advanced_search?hl=en}|\n" +
    				" Subscribe to our monthly newsletter {http://www.mozilla.com/en-US/newsletter/?WT.mc_id=S_09_10_3&WT.mc_ev=click}|\n " +
    				"About Mozilla {http://www.mozilla.org/about/?utm_source=gsnippet&utm_content=aboutlink&utm_campaign=s170210}|\n " +
    				"Firefox Support {http://support.mozilla.com/en-US/kb/?S100719_support&WT.mc_ev=click}| " +
    				"speech web site {http://www.speechapi.com}); ";
    		
    		String grammar3 = "#JSGF V1.0;\n grammar smashup;\n public <command> = (<link>);\n <link> = ( Images {X}| Videos {Y}| Maps {X}| News {h}); " ; 
    		
	     protected void setUp() {
		    
	    	 ssc.setup();
	     }

	 
	    
	    public void testPlay() {
			ssc.play("hello hello hello", "jmk-arctic");
	    
	    }
	    
	    public void testRecognize() {
			ssc.recognize(grammar );
			
	    }
	    
	    public void testRecognizeWithTriggers() {
			ssc.recognizeWithTriggers(grammar2);
			
			ssc.triggerStart();
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
			ssc.triggerEnd();
			
			
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println("Results: "+ssc.getResults());
			
	    }
	    
	    
	    


     public void showFormats( Line.Info li )
        {
        if ( li instanceof DataLine.Info )
            {
            AudioFormat[] afs = ( ( DataLine.Info ) li ).getFormats();
            for ( AudioFormat af : afs )
                {
            	System.out.println( "        " + af.toString() );
                }
            }
        }


     public  void testFormats2( ) {
    	 Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
    	 for (Mixer.Info info: mixerInfos){
    		 Mixer m = AudioSystem.getMixer(info);
    		 //Line.Info[] lineInfos = m.getSourceLineInfo();
    		 //for (Line.Info lineInfo:lineInfos){
    		//	 System.out.println (info.getName()+"---"+lineInfo);
    		//	 Line line = m.getLine(lineInfo);
    		//	 System.out.println("\t-----"+line);
    		 //}
    		 Line.Info[] lineInfos = m.getTargetLineInfo();
    		 for (Line.Info lineInfo:lineInfos){
    			 System.out.println (m+"---"+lineInfo);
    			 Line line;
				try {
					line = m.getLine(lineInfo);
	    			 System.out.println("\t-----"+line);
				} catch (LineUnavailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


    		 }
    	 }
     }
     
     
    public  void testFormats( )
        {
        // loop through all mixers, and all source and target lines within each mixer.
        Mixer.Info[] mis = AudioSystem.getMixerInfo();
       System.out.println(")))))))))))))))))) "+mis.length);
        for ( Mixer.Info mi : mis )
            {
            Mixer mixer = AudioSystem.getMixer( mi );

            // e.g. com.sun.media.sound.DirectAudioDevice
            System.out.println( "mixer: " + mixer.getClass().getName().toString() );

            Line.Info[] lis = mixer.getSourceLineInfo();
            for ( Line.Info li : lis )
                {
            	System.out.println( "    source line: " + li.toString() );
                showFormats( li );
                }

            lis = mixer.getTargetLineInfo();
            for ( Line.Info li : lis )
                {
                System.out.println( "    target line: " + li.toString() );
                showFormats( li );
                }

            Control[] cs = mixer.getControls();
            for ( Control c : cs )
                {
            	System.out.println( "    control: " + c.toString() );
                }
            }
        }

  
}



