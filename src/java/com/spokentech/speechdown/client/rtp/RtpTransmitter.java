package com.spokentech.speechdown.client.rtp;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.media.rtp.InvalidSessionAddressException;

import org.apache.log4j.Logger;
import org.speechforge.cairo.rtp.AudioFormats;
import org.speechforge.cairo.rtp.RTPPlayer;

import com.spokentech.speechdown.client.PromptPlayListener;

/**
 * Sends audio files down an RTP channel.
 *
 * @author Spencer Lord {@literal <}<a href="mailto:slord@users.sourceforge.net">slord@users.sourceforge.net</a>{@literal >}
 */
public class RtpTransmitter {

    private static Logger _logger = Logger.getLogger(RtpTransmitter.class);

    // TODO: move to config file
    private static final File FEEDER_PROMPT_FILE = new File("../prompts/feeder.wav");

    static final short IDLE = 0;
    static final short SPEAKING = 1;
    static final short PAUSED = 2;

    volatile short _state = IDLE;

    BlockingQueue<PromptPlay> _promptQueue = new LinkedBlockingQueue<PromptPlay>();
    private SendThread _sendThread;
    RTPPlayer _promptPlayer;
    private int _localPort;
    private InetAddress _remoteAddress;
    private int _remotePort;
    private AudioFormats _af;
    
    /**
     * TODOC
     * @param localPort 
     * @param remoteAddress 
     * @param remotePort 
     */
    public RtpTransmitter(int localPort, InetAddress remoteAddress, int remotePort, AudioFormats af) {
        _localPort = localPort;
        _remoteAddress = remoteAddress;
        _remotePort = remotePort;
        _af = af;
    }

    private boolean init() throws InvalidSessionAddressException, IOException {
        if (_promptPlayer == null) {
            _promptPlayer = new RTPPlayer(_localPort, _remoteAddress, _remotePort, _af);
            (_sendThread = new SendThread()).start();
            return true;
        }
        return false;
    }
    
    public synchronized void shutdown() throws InterruptedException {
        _sendThread.shutdown();
        _promptPlayer.shutdown();
    }

    public synchronized int queueAudio(File promptFile, PromptPlayListener listener)
      throws InvalidSessionAddressException, IOException {

        int state = _state;
        try {
            if (init()) {
                if (FEEDER_PROMPT_FILE.exists()) {
                    if (_logger.isDebugEnabled()) {
                        _logger.debug("Queueing feeder prompt: " + FEEDER_PROMPT_FILE.getAbsolutePath());
                    }
                    _promptQueue.put(new PromptPlay(FEEDER_PROMPT_FILE, null));
                } else if (_logger.isDebugEnabled()) {
                    _logger.debug("Feeder prompt not found: " + FEEDER_PROMPT_FILE.getAbsolutePath());
                }
            }
            _promptQueue.put(new PromptPlay(promptFile, listener));
            _state = SPEAKING;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return state;
    }
    
    public synchronized void stopPlayback() {
        _sendThread.interrupt();
        //TODO: wait for send thread to complete?  (prevent double interrupt while draining queue)
    }

    private class SendThread extends Thread {
        
        volatile boolean _run = true;
        
//        @Override
//        public synchronized void interrupt() {
//            super.interrupt();
//        }
//        
//        @Override
//        public synchronized boolean isInterrupted() {
//            return super.isInterrupted();
//        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            /*if (Thread.currentThread() != this) {
                throw new RuntimeException();
            }*/
            while (_run) {
                PromptPlay promptPlay = null;
                boolean drainQueue = false;
                Exception cause = null;

                try {

                    // first clear interrupted status of current thread
                    Thread.interrupted();

                    // get next prompt to play
                    _logger.debug("taking next prompt from prompt queue...");
                    promptPlay = _promptQueue.take();
                    _logger.debug("playing next prompt...");
                    _promptPlayer.playPrompt(promptPlay._promptFile);

                    // drain all prompts in queue if current prompt playback is interrupted (e.g. by STOP request)
                    drainQueue = Thread.interrupted();

                } catch (InterruptedException e) {
                    _logger.debug(e, e);
                    // TODO: cancel current prompt playback
                    drainQueue = true;

                } catch (Exception e) {
                    _logger.debug(e, e);
                    cause = e;
                }

                if (drainQueue) {
                    _logger.debug("draining prompt queue...");
                    while (!_promptQueue.isEmpty()) {
                        try {
                            _promptQueue.take();
                            //TODO: may need to remove only specific prompts
                            // (e.g. save and put back in queue if not in cancel list)
                        } catch (InterruptedException e1) {
                            // should not happen since this is the only thread consuming from queue
                            _logger.warn(e1, e1);
                        }
                    }
                } else if (promptPlay != null) {
                    if (promptPlay._listener != null) {
                        _logger.debug("notifying prompt play listener...");
                        if (cause == null) {
                            try {
                                // give rtp stream a chance to catch up...
                                Thread.sleep(250);
                            } catch (InterruptedException e) {
                                _logger.debug("InterruptedException encountered!", e);
                            }
                            promptPlay._listener.playCompleted();
                        } else {
                            promptPlay._listener.playFailed(cause);
                        }
                        _logger.debug("prompt play listener notified.");
                    }
                } else {
                    _logger.warn("promptPlay is null!", cause);
                }

                _state = _promptQueue.isEmpty() ? IDLE : SPEAKING;
            }
        }
        
        public void shutdown() {
            _run = false;
        }
    }

    private static class PromptPlay {

        private File _promptFile;
        private PromptPlayListener _listener;

        PromptPlay(File promptFile, PromptPlayListener listener) {
            _promptFile = promptFile;
            _listener = listener;
        }
    }

    /**
     * TODOC
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        
        File promptDir = new File("C:\\work\\cvs\\onomatopia\\cairo\\prompts\\test");

        int localPort = 42050;
        InetAddress remoteAddress = InetAddress.getLocalHost();
        int remotePort = 42048;

        RtpTransmitter player = new RtpTransmitter(localPort, remoteAddress, remotePort, new AudioFormats());
        
        File prompt = new File(promptDir, "good_morning_rita.wav");
        player.queueAudio(prompt, null);
        player.queueAudio(prompt, null);
        player.queueAudio(prompt, null);
        player.queueAudio(prompt, null);
        player.queueAudio(prompt, null);
    }

}
