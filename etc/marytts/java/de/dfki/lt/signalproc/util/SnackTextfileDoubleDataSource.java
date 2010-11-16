/**
 * Copyright 2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.signalproc.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class SnackTextfileDoubleDataSource extends TextReaderDoubleDataSource
{
    /**
     * Read Double data from a Text file. Read one double per line. 
     * @param reader
     */
    public SnackTextfileDoubleDataSource(File file) throws FileNotFoundException
    {
        this(new FileReader(file));
    }
    
    /**
     * Initialise this double data source with the reader from which a text
     * representation of doubles (one per line) can be read. 
     */
    public SnackTextfileDoubleDataSource(Reader reader) {
        super(reader);
    }
    
    
    
    /**
     * Try to get length doubles from this DoubleDataSource, and copy them into target, starting from targetPos.
     * This is the core method getting the data. Subclasses may want to override this method.
     * If an exception occurs reading from the underlying reader, or converting data to double,
     * the method will print a stack trace to standard error, but otherwise will 
     * silently stop and behave as if all data was read.
     * @param target the double array to write into
     * @param targetPos position in target where to start writing
     * @param length the amount of data requested
     * @return the amount of data actually delivered. If the returned value is less than length,
     * only that many data items have been copied into target; further calls will return 0 and not copy anything.
     */
    public int getData(double[] target, int targetPos, int length)
    {
        int i=0;
        try {            
            String line;
            String strVal;
            StringTokenizer s;
            double value;
            
            List lines = new ArrayList();
            while ((line= reader.readLine())!=null){
                lines.add(line);
            }
            int numData = lines.size();
            for (i=0;i<length;i++){
                if (i>=numData) return i;
                strVal = (String)lines.get(i);
                s = new StringTokenizer(strVal);
                
                if (s.hasMoreTokens())
                    value = Double.parseDouble(s.nextToken());
                else
                    value = Double.parseDouble(strVal);
                
                target[targetPos+i] = value;            
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return i;
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            return i;
        }
        return length;  
    }
}
