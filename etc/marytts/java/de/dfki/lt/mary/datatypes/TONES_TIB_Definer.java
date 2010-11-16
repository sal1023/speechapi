/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.datatypes;

import java.util.Locale;

import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;

/**
 * @author Marc Schr&ouml;der
 *
 *
 */
public class TONES_TIB_Definer extends MaryDataType {
    static {
        define("TONES_TIB", new Locale("tib"), true, true, MARYXML, MaryXML.MARYXML, null,
        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
        "<maryxml version=\"0.4\"\n" +
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" +
        "xml:lang=\"tib\">\n" +
        "<s>\n" +
		"<phrase>\n" +
        "<t sampa=\"tra\">\n" +
        "<syllable sampa=\"tra\" tone=\"high\" root=\"k\" slot1=\"b\" slot3=\"r\" vowel=\"a\"/>\n" +
        "bkra\n" +
        "</t>\n" +
        "<t sampa=\"Si\">\n" +
        "<syllable sampa=\"Si\" tone=\"high\" root=\"sh\" slot5=\"s\" vowel=\"i\"/>\n" +
        "shis\n" +
        "</t>\n" +
        "<t sampa=\"te\">\n" +
        "<syllable sampa=\"te\" tone=\"low\" root=\"d\" slot1=\"b\" vowel=\"e\"/>\n" +
        "bde\n" +
        "</t>\n" +
        "<t sampa=\"lEk\">\n" +
        "<syllable sampa=\"lEk\" tone=\"low\" root=\"l\" slot4=\"g\" slot5=\"s\" vowel=\"e\"/>\n" +
        "legs\n" +
        "</t>\n" +
		"</phrase>" +
        "</s>\n" +
        "</maryxml>\n");
    }
}
