package com.spokentech.speechdown.client.util;

import org.apache.log4j.Logger;

public  class WerUtils {
    
    private static Logger _logger = Logger.getLogger(WerUtils.class);

    
    
    public static double calcWer(String actual, String expected) {
        
        int wordsInExpectedResult = expected.split(" ").length;
        if (expected == null) {
            _logger.info("Null Expected result!  Check test script.");
            throw new RuntimeException();
        }
        int distance = wordsInExpectedResult;
        if (actual == null) {
            _logger.info("Null result.  Counting errors as # of words in expected result.  100% error.");
        } else {
           distance = LevenshteinDistance(actual, expected);
        }
        double WER = (double)distance/(double)wordsInExpectedResult;

        _logger.debug("WER %"+100.0*WER+" ("+actual+"/"+expected+")");
        return WER;
    }
    
    private static int LevenshteinDistance(String actual, String expected) {

        //char s[1..m], char t[1..n])
        String s[] = actual.split("\\s+");
        String t[] = expected.split("\\s+");
        
        //for (int i=0;i<s.length;i++) {
        //   _logger.info(i+" "+s[i]);
        //}
        
        //int count =0;
        //for (String x: t ) {
        //	_logger.info(count++ +" "+x);
        //}
        	

        // d is a table with m+1 rows and n+1 columns
        int[][] d = new int[s.length+1][t.length+1];

        for (int i=0;i<=s.length;i++) {
            d[i][0] = i;
        }
        for (int j=0;j<=t.length;j++) {
            d[0][j] = j;
        }

        int cost = 0;
        for (int i=1;i<=s.length;i++) {
            for (int j=1;j<=t.length;j++) {
                if (s[i-1].equals(t[j-1])) {
                    cost = 0;
                } else {
                    cost = 1;
                }
                int del = d[i-1][j] +1;
                int ins = d[i][j-1]+1;
                int sub = d[i-1][j-1] + cost;
                if ((del < ins) && (del < sub)) {
                    d[i][j] = del;
                } else if ((ins<del) && (ins < sub)) { //could eliminate first check...
                    d[i][j] = ins;
                } else {
                    d[i][j] = sub;
                }
            }
        }
        return d[s.length][t.length];
    }

}
