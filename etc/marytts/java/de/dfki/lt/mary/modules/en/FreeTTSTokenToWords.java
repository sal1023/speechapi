/**
 * Portions Copyright 2002 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
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

package de.dfki.lt.mary.modules.en;

import java.util.Iterator;
import java.util.List;

import com.sun.speech.freetts.Utterance;
import com.sun.speech.freetts.UtteranceProcessor;
import com.sun.speech.freetts.cart.CARTImpl;
import com.sun.speech.freetts.en.us.PrefixFSM;
import com.sun.speech.freetts.en.us.PronounceableFSM;
import com.sun.speech.freetts.en.us.SuffixFSM;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.modules.InternalModule;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;


/**
 * Use an individual FreeTTS module for English synthesis.
 *
 * @author Marc Schr&ouml;der
 */

public class FreeTTSTokenToWords extends InternalModule
{
    private UtteranceProcessor processor;

    public FreeTTSTokenToWords()
    {
        super("TokenToWords",
              MaryDataType.get("FREETTS_TOKENS_EN"),
              MaryDataType.get("FREETTS_WORDS_EN")
              );
    }

    public void startup() throws Exception
    {
        super.startup();

        // Initialise FreeTTS
        FreeTTSVoices.load();
    	CARTImpl numbersCart = new CARTImpl
	       (com.sun.speech.freetts.en.us.CMUVoice.class.getResource("nums_cart.txt"));
    	PronounceableFSM prefixFSM = new PrefixFSM
	       (com.sun.speech.freetts.en.us.CMUVoice.class.getResource("prefix_fsm.txt"));
	   PronounceableFSM suffixFSM = new SuffixFSM
	        (com.sun.speech.freetts.en.us.CMUVoice.class.getResource("suffix_fsm.txt"));
        processor = new TokenToWords(numbersCart, prefixFSM, suffixFSM);
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        List utterances = d.getUtterances();
        Iterator it = utterances.iterator();
        while (it.hasNext()) {
            Utterance utterance = (Utterance) it.next();
            processor.processUtterance(utterance);
        }
        MaryData output = new MaryData(outputType());
        output.setUtterances(utterances);
        return output;
    }




}
