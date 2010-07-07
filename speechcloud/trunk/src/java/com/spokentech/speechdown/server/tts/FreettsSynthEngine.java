/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.server.tts;



import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat.Type;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.server.util.pool.AbstractPoolableObject;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;



/**
 * Generates speech prompt files using the FreeTTS text-to-speech engine.
 *
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class FreettsSynthEngine extends AbstractPoolableObject implements SynthEngine {

	
    static Logger _logger = Logger.getLogger(FreettsSynthEngine.class);
	
    private Voice _voice;

    public FreettsSynthEngine(String voiceName) {

        VoiceManager voiceManager = VoiceManager.getInstance();
        _voice = voiceManager.getVoice(voiceName);

        if (_voice == null) {
            throw new RuntimeException("TTS voice name <" + voiceName + "> not found!");
        }

        _voice.allocate();
    }

    /* (non-Javadoc)
     * @see com.spokentech.speechdown.server.tts.Synthesizer#generateAudio(java.lang.String, java.io.File)
     */
    public synchronized File generateAudio(String text, File dir, AudioFormat format, AudioFileFormat.Type fileType) throws IllegalArgumentException {
        if (dir == null || !dir.isDirectory()) {
            throw new IllegalArgumentException("Directory file specified does not exist or is not a directory: " + dir);
        }

        if (text == null) {
            text = "";
        }

        String promptName = Long.toString(System.currentTimeMillis());
        File promptFile = new File(dir, promptName);

        AudioPlayer ap = new SingleFileAudioPlayer(promptFile.getAbsolutePath(), fileType);

        ap.setAudioFormat(format);
        _voice.setAudioPlayer(ap);
        _voice.speak(text);
        try {
	        ap.close();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
        _voice.setAudioPlayer(null);

        promptFile = new File(dir, promptName + "."+fileType.getExtension());
        if (!promptFile.exists()) {
            throw new RuntimeException("Expected generated prompt file does not exist! "+ promptName + "."+fileType.getExtension());
        }
        return promptFile;
    }

	@Override
    public File generateAudio(String text, OutputStream stream, AudioFormat format, Type fileType)
            throws IllegalArgumentException {
	    _logger.warn("No implemented yet!");
	    return null;
    }

}