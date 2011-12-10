/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package com.spokentech.speechdown.server;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

/** A simple example that shows how to transcribe a continuous audio file that has multiple utterances in it. */
public class TranscriberS4ConfigTest  extends TestCase  {

	public void startup() {}
	protected void setUp() {
		//gson = new Gson();
	}


	public void testGtd()throws IOException, UnsupportedAudioFileException {
		URL audioURL;


		audioURL = new URL("file:///c:/work/speechcloud/etc/test/transcriber/mandarin.wav");
        audioURL = new URL("file:///c:/work/speechcloud/etc/test/transcriber/gulf.wav");


		//URL configURL = Transcriber.class.getResource("config.xml");
		URL configURL = new URL("file:///c:/work/speechcloud/etc/test/transcriber/config.xml");


		ConfigurationManager cm = new ConfigurationManager(configURL);
		Recognizer recognizer = (Recognizer) cm.lookup("recognizer");

		/* allocate the resource necessary for the recognizer */
		recognizer.allocate();

		// configure the audio input for the recognizer
		AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
		dataSource.setAudioFile(audioURL, null);

		// Loop until last utterance in the audio file has been decoded, in which case the recognizer will return null.
		Result result;
		System.out.println("Entering loop");
		while ((result = recognizer.recognize())!= null) {
			System.out.println("In loop");
			String resultText = result.getBestResultNoFiller();
			System.out.println(resultText);
		}
	}
}
