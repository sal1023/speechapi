package com.speechforge.masher.applet;

import static org.speechforge.cairo.jmf.JMFUtil.MICROPHONE;
import netscape.javascript.JSObject;

import org.speechforge.cairo.rtp.NativeMediaClient;
import org.speechforge.cairo.client.recog.RecognitionResult;
import org.speechforge.cairo.rtp.RTPConsumer;
import org.speechforge.cairo.client.recog.RuleMatch;
import org.speechforge.cairo.sip.SdpMessage;
import org.speechforge.cairo.sip.SimpleSipAgent;
import org.speechforge.cairo.sip.SipSession;
import org.speechforge.cairo.client.NoMediaControlChannelException;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechClientImpl;
import org.speechforge.cairo.client.SpeechEventListener;
import org.speechforge.cairo.client.SpeechRequest;
import java.applet.Applet;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sip.SipException;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONStringer;
import org.mrcp4j.MrcpEventName;
import org.mrcp4j.MrcpResourceType;
import org.mrcp4j.client.MrcpChannel;
import org.mrcp4j.client.MrcpFactory;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.client.MrcpProvider;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.header.IllegalValueException;


import com.speechforge.masher.applet.test.AppletProperties;

/**
 * Demo MRCPv2 client application that plays a TTS prompt while performing speech recognition on
 * microphone input.  Prompt playback is cancelled as soon as start of speech is detected.
 *
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */

public class MrcpClientApplet extends Applet implements SpeechEventListener {

    private static final long serialVersionUID = 1L;

    private static Logger _logger = Logger.getLogger(MrcpClientApplet.class);
    
    public enum SessionStateType {none, establishing, established, fault}
    public enum RecognitionStateType {none, normal, hotword, fault}
    
    
    private SessionStateType sessionState = SessionStateType.none;
    private RecognitionStateType recognitionState = RecognitionStateType.none;

    private SpeechClient _client;
    
    NativeMediaClient mediaClient = null;
    
    private MrcpEvent _mrcpEvent;
    
    private volatile boolean _bargeIn;
    private  SimpleSipAgent sipAgent;
    private   boolean sentBye=false;
    private  String _localHostName = null;
    private  String _publicLocalHostName = null;
    private InetAddress _localAddress = null;

    InetAddress _cairoSipInetAddress = null;
    private  String _cairoSipHostName;
    
    final static int  defaultLocalRtpPort = 42046;
    private  int _localRtpPort = defaultLocalRtpPort;
    
    final static int  defaultLocalSipPort = 5090;
    private  int _localSipPort = defaultLocalSipPort;
    
    final static int  defaultCairoSipPort = 5050;
    private  int _cairoSipPort = defaultCairoSipPort;
    
    final static String defaultSipAddress = "sip:speechSynthClient@speechforge.org";
    private  String _mySipAddress = null;
    
    final static String defaultCairoSipAddress = "sip:cairo@speechforge.org";
    private  String _cairoSipAddress = defaultCairoSipAddress;

    final static String defaultSdpSessionName ="voice.web";
    private  String _sdpSessionName = "voice.web";  
    
    final static String defaultSipTransport ="UDP";
    private  String _sipTransport = "UDP";  
    
    final static long  defaultNoInputTimeout = 30000;
    private  long _noInputTimeout = defaultNoInputTimeout;
    
    
    public JSObject _window = null;
     
    private  Map<String, String> urls;

   Image green;
   Image yellow;
   Image red;
   Image grey;
   
   // This object will allow you to control loading
   MediaTracker mt; 
    
   // The applet base URL
   URL base; 
 
   boolean done = false;
   
   String focus = null;
   
   String latestResult = null;
   
    public void init() { 
        AppletProperties.print();
        _window = (JSObject) JSObject.getWindow(this);
        
        //TODO:  A  hack for the demo. Urls are hardcoded here.  Need to get the urls to applet from the server at page load time
        urls = new Hashtable<String, String>();
        urls.put("scams","http://www.craigslist.org/about/scams");
        urls.put("safety","http://www.craigslist.org/about/safety");
        urls.put("blog","http://blog.craigslist.org/");
        urls.put("facts","http://www.craigslist.org/about/factsheet.html");
        urls.put("best","http://www.craigslist.org/about/best/all/");
        urls.put("jobs","http://www.craigslist.org/about/job.boards.html");
        urls.put("weather","http://www.crh.noaa.gov/forecasts/CAZ006.php");
        urls.put("quake","http://quake.wr.usgs.gov/recenteqs/");
        urls.put("tides","http://tidesonline.nos.noaa.gov/plotcomp.shtml?station_info=9414290+San+Francisco,+CA");
        urls.put("directory","http://www.bapd.org");
        urls.put("movies","http://24hoursoncraigslist.com/subs/nowplaying.html");
        urls.put("tshirts","http://www.craigslistfoundation.org/index.php?page=Craigslist_Foundation_Store");
        urls.put("foundation","http://craigslistfoundation.org/");
        urls.put("bootcamp","http://www.craigslistfoundation.org/bayarea");
        urls.put("net","http://savetheinternet.com/=faq");
        
        // initialize the MediaTracker
        mt = new MediaTracker(this);

        // The try-catch is necassary when the URL isn't valid
        // Ofcourse this one is valid, since it is generated by
        // Java itself.

       try {
           // getDocumentbase gets the applet path.
             base = getDocumentBase();
       }
       catch (Exception e) {}

        // Here we load the image.
        // Only Gif and JPG are allowed. Transparant gif also.
        red = getImage(base,"speechforgered.jpg");
        yellow = getImage(base,"speechforgeyellow.jpg");
        green = getImage(base,"speechforgegreen.jpg");
        grey = getImage(base,"speechforgegrey.jpg");

        // tell the MediaTracker to kep an eye on this image, and give it ID 1;
        mt.addImage(red,1);
        mt.addImage(yellow,2);
        mt.addImage(green,3);
        mt.addImage(grey,4);

        // now tell the mediaTracker to stop the applet execution
        // (in this example don't paint) until the images are fully loaded.
        // must be in a try catch block.

       try {
             mt.waitForAll();
        }
        catch (InterruptedException  e) {}

        // when the applet gets here then the images is loaded. 
        
    }

    public void start() {
        getConnectionParams();
        startSession();      //_cairoSipPort, _cairoSipHostName, _cairoSipAddress, _localRtpPort, _sdpSessionName
        (new Thread(new RecognitionControl())).start();
    }

    public void stop() {
        _logger.info("stopping... ");
        done = true;
        stopSession();
    }

    public void destroy() {
        _logger.info("preparing for unloading...");
    }   

    public void getConnectionParams() {
        //Get the applet parameters to be used for establishing the conncetion to the speech server
        // sip ports, rtp port, sip addresses, sdp session name
        
        
        //the local sip port
        String tempString;
        tempString = getParameter("LOCALSIPPORT");
        _logger.debug("getparam: local sip port "+ tempString);
        if (tempString != null) {
            try {
                _localSipPort = Integer.parseInt(tempString);
            } catch (NumberFormatException e) {
                _localSipPort = defaultLocalSipPort;
            }
        }
        
        //the local sip address
        _mySipAddress = getParameter("LOCALSIPADDRESS");
        _logger.debug("getparam: local sip address "+ _mySipAddress);
        if (_mySipAddress == null)
            _mySipAddress =defaultSipAddress;
        
        //get the local host name
        //_localHostName = getParameter("LOCALHOSTNAME");
        //_logger.debug("getparam: local host name "+ _localHostName);
        //if (_localHostName == null)
        //    _logger.warn("Null local host name");
        
        URL url = getDocumentBase();
        String host = url.getHost();
        int p = url.getPort();
        Socket socket = null;
        try {
            socket = new Socket(host, p);
        } catch (UnknownHostException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        InetAddress addr = socket.getLocalAddress();
        _localHostName = addr.getHostAddress();
        System.out.println("Addr: " + _localHostName);


        
        
        //get the local host name
        _publicLocalHostName = getParameter("LOCALHOSTNAMEPUBLIC");
        _logger.debug("getparam: local host name "+ _publicLocalHostName);
        if (_publicLocalHostName == null)
            _logger.warn("Null public local host name");
        
        //Cairo sip port
        tempString = getParameter("CAIROSIPPORT");
        _logger.debug("getparam: cairo sip port "+ tempString);
        if (tempString != null) {
            try {
                _cairoSipPort = Integer.parseInt(tempString);
            } catch (NumberFormatException e) {
                _cairoSipPort = defaultCairoSipPort;
            }
        }
        
        // The Voice Server needs to run on the same server from which the applet was loaded
        // so get the codbase host
        //_peerHostName = getCodeBase().getHost();
        //_peerHostPort = getCodeBase().getPort();
        

        _cairoSipHostName = getParameter("REMOTEHOSTNAME");
        _logger.debug("getparam: cairo sip hostname "+ _cairoSipHostName);
        //System.out.println("peer host parameter :"+cairoSipHostName);
        if (_cairoSipHostName == null)
            _cairoSipHostName = getCodeBase().getHost();
        
        //CAIRO SIP ADDRESS
        _cairoSipAddress = getParameter("CAIROSIPADDRESS");
        _logger.debug("getparam: cairo sip address "+ _cairoSipAddress);
        if (_cairoSipAddress == null)
            _cairoSipAddress=defaultCairoSipAddress;
        
        //LOCAL RTP PORT
        tempString = getParameter("LOCALRTPPORT");
        _logger.debug("getparam: local rtp  port "+ tempString);
        if (tempString != null) {
            try {
                _localRtpPort = Integer.parseInt(tempString);
            } catch (NumberFormatException e) {
                _localRtpPort = defaultLocalRtpPort;
            }
        }

        _sdpSessionName = getParameter("SDPSESSIONNAME");
        _logger.debug("getparam: sdp session name "+ _sdpSessionName);
        if (_sdpSessionName == null)
            _sdpSessionName =defaultSdpSessionName;
     
        _sipTransport = getParameter("SIPTRANSPORT");
        _logger.debug("getparam: sip transport "+ _sipTransport);
        if (_sipTransport == null)
            _sipTransport =defaultSipTransport;
       
        
        tempString = getParameter("NOINPUTTIMEOUT");
        _logger.debug("getparam: no input timeout "+ tempString);
        if (tempString != null) {
            try {
                _noInputTimeout = Integer.parseInt(tempString);
            } catch (NumberFormatException e) {
                _noInputTimeout = defaultNoInputTimeout;
            }
        }        

    }
    
    public void paint(Graphics g) {
        _logger.debug("Painting session/recog states: "+sessionState +"/"+recognitionState);
        //Draw a Rectangle around the applet's display area.
        g.drawRect(0, 0, 
                   getWidth() - 1,
                   getHeight() - 1);

        if ((sessionState == SessionStateType.none) || 
                (sessionState == SessionStateType.establishing)) {
            g.drawImage(red,1,1,this);
        } else if (sessionState == SessionStateType.established) {
            if (recognitionState == RecognitionStateType.hotword) {
                g.drawImage(yellow,1,1,this);
            } else if (recognitionState == RecognitionStateType.normal) {
                g.drawImage(green,1,1,this);
            } else if (recognitionState == RecognitionStateType.none) {
                g.drawImage(grey,1,1,this);
            } else if (recognitionState == RecognitionStateType.fault) {
                g.drawImage(grey,1,1,this);        
                                
            }
        } else if (sessionState == SessionStateType.fault) {
            g.drawImage(grey,1,1,this);
        }


        //Draw the current string inside the rectangle.
        //g.drawString(buffer.toString(), 5, 15);
    }
    
  /*  private void createGUI() {
        JPanel contentPane = new JPanel(new FlowLayout());
        contentPane.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(Color.BLACK),
                                    BorderFactory.createEmptyBorder(10,10,10,10)));
        setContentPane(contentPane);
        
        
        startSession = new JButton();
        stopSession = new JButton();
        recognizeOnce = new JButton();
        startRecognize = new JButton();
        stopRecognize = new JButton();
        
        playOnce = new JButton("Bark!");
        playOnce.setActionCommand(PLAY_ONCE_CMD);
        playOnce.addActionListener(this);
        add(playOnce);

        startLoop = new JButton("Start sound loop");
        startLoop.setActionCommand(START_LOOP_CMD);
        stopLoop = new JButton("Stop sound loop");
        stopLoop.setActionCommand(STOP_LOOP_CMD);
        stopLoop.setEnabled(false);
        startLoop.addActionListener(this);
        add(startLoop);
        stopLoop.addActionListener(this);
        add(stopLoop);

        reload = new JButton("Reload sounds");
        reload.setActionCommand(RELOAD_CMD);
        reload.addActionListener(this);
        add(reload);

        startLoadingSounds();
    }*/

    
    //public void startSession(int cairoSipPort, String cairoSipHostName, String cairoSipAddress, int localRtpPort, String sdpSessionName) {
    public void startSession() {

        sessionState = SessionStateType.establishing;
        try {
            _cairoSipInetAddress = InetAddress.getByName(_cairoSipHostName);
        } catch (UnknownHostException e1) {
            System.err.println("Couldn't get Internet address: Unknown host");
            e1.printStackTrace();

        }         

        System.out.println("cairo sip host name: "+_cairoSipHostName);
        System.out.println("cairo sip inet address: "+_cairoSipInetAddress);
        if ((_cairoSipInetAddress == null) || (_cairoSipHostName.length() == 0)) {
            //Just use the localhost if there is no codebase host (for testing)
            _cairoSipHostName=_localHostName;
            _cairoSipInetAddress=_localAddress;
        }
        

        if (_localRtpPort < 0 || _localRtpPort >= RTPConsumer.TCP_PORT_MAX || _localRtpPort % 2 != 0) {
            _logger.warn("Improper format for first command line argument <local-rtp-port>," +
                " should be even integer between 0 and " + RTPConsumer.TCP_PORT_MAX);
        }
        
        //TODO:  Where should teh format be specified?
        Vector format = new Vector();
        format.add("0");           //PCMU
        
     
        // Construct a SIP agent to be used to send a SIP Invitation to the ciaro server
        try {
            sipAgent = new SimpleSipAgent(_mySipAddress, "Synth Client Sip Stack", _localHostName, _publicLocalHostName, _localSipPort, _sipTransport);
        } catch (SipException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        // Construct the SDP message that will be sent in the SIP invitation
        SdpMessage message = null;
        try {
            message = constructResourceMessage(_localRtpPort,format);
        } catch (UnknownHostException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (SdpException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        // Send the sip invitation (This method on the demoSipAgent blocks until a response is received or timeout occurs) 

        SdpMessage inviteResponse = null;
        int retryCount = 0;
        boolean failed =true;
        while ((retryCount <3)&& (failed)) {
            try {
                inviteResponse = sipAgent.sendInviteWithoutProxy(_cairoSipAddress, message, _cairoSipHostName, _cairoSipPort);
                failed = false;
            } catch (SipException e1) {
 
                _logger.info("failed to setup streams, failure #" +retryCount++);
                
                try {
                    sipAgent.sendBye();
                } catch (SipException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                }
                
                if (retryCount >= 3) {
                    _logger.info("Too many failures setting up stream.  Giving up.");  
                    //TODO: may as well shut it all down at this point -- cant set up the stream
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
        }

        if (inviteResponse != null) {
            _logger.info("Received the SIP Response.");
            

            
            // Get the MRCP media channels (need the port number and the channelID that are sent
            // back from the server in the response in order to setup the MRCP channel)
            
            
            //The transmitter channel
            List<MediaDescription> xmitterChans = null;
            int xmitterPort = 0;
            String xmitterChannelId = null;
            try {
                xmitterChans = inviteResponse.getMrcpTransmitterChannels();
                xmitterPort = xmitterChans.get(0).getMedia().getMediaPort();
                xmitterChannelId = xmitterChans.get(0).getAttribute(SdpMessage.SDP_CHANNEL_ATTR_NAME);
            } catch (SdpException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            //The receiver channel
            List<MediaDescription> receiverChans = null;
            MediaDescription controlChan = null;
            int receiverPort = 0;
            String receiverChannelId = null;
            try {
                receiverChans = inviteResponse.getMrcpReceiverChannels();
                controlChan = receiverChans.get(0);
                receiverPort = controlChan.getMedia().getMediaPort();
                receiverChannelId = receiverChans.get(0).getAttribute(SdpMessage.SDP_CHANNEL_ATTR_NAME);
            } catch (SdpException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            
            //The media channel
            List<MediaDescription> rtpChans = null;
            int remoteRtpPort = -1;
            try {
                rtpChans = inviteResponse.getAudioChansForThisControlChan(controlChan);
                if (rtpChans.size() > 0) {
                    //TODO: What if there is more than 1 media channels?
                    //TODO: check if there is an override for the host attribute in the m block
                    //InetAddress remoteHost = InetAddress.getByName(rtpmd.get(1).getAttribute();
                    remoteRtpPort =  rtpChans.get(0).getMedia().getMediaPort();
                } else {
                    _logger.warn("No Media channel specified in the invite request");
                    //TODO:  handle no media channel in the response corresponding tp the mrcp channel (sip/sdp error)
                }
            } catch (SdpException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            _logger.debug("Starting NativeMediaClient...");

            
            if (mediaClient == null) {
                retryCount = 0;
                failed =true;
                while ((retryCount <3)&& (failed)) {
                    try {
                        mediaClient = new NativeMediaClient(_localHostName, _localRtpPort, _cairoSipInetAddress, remoteRtpPort);
                        failed = false;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        if (mediaClient != null) {
                            mediaClient.stop();
                        }
                        _logger.info("failed to setup streams, failure #" +retryCount++);
                        if (retryCount >= 3) {
                            _logger.info("Too many failures setting up stream.  Giving up.");  
                            //TODO: may as well shut it all down at this point -- cant set up the stream
                        }
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                _logger.info("There is already a media client.  Not creating a new one.");
            }
            mediaClient.startTransmit();

            //Construct the MRCP Channels
            String protocol = MrcpProvider.PROTOCOL_TCP_MRCPv2;
            MrcpFactory factory = MrcpFactory.newInstance();
            MrcpProvider provider = factory.createProvider();

            MrcpChannel ttsChannel = null;
            MrcpChannel recogChannel = null;
            
            try {
                ttsChannel = provider.createChannel(xmitterChannelId, _cairoSipInetAddress, xmitterPort, protocol);
                _logger.info("Created the tts channel: "+ttsChannel.toString());
            } catch (IllegalArgumentException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IllegalValueException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }


            try {
                recogChannel = provider.createChannel(receiverChannelId, _cairoSipInetAddress, receiverPort, protocol);
                _logger.debug("created the recog channel:"+recogChannel.toString());
            } catch (IllegalArgumentException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IllegalValueException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            SipSession session = sipAgent.getSipSession();
            session.setTtsChannel(ttsChannel);
            session.setRecogChannel(recogChannel);
            
            _client = new SpeechClientImpl(ttsChannel,recogChannel);
            _client.setListener(this);
            
            changeStatus(SessionStateType.established, recognitionState);
            enableRecognition();
            
        } else {
            //Invitation Timeout
            _logger.info("Sip Invitation timed out.  Is server running?");
        }

        
    }
    
    public void stopSession() {

        //shutdown the cairo (mrcp) client
        if (_client != null) {
            try {
                _client.shutdown();
            } catch (MrcpInvocationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            _client = null;
        }


        //terminate the sip session with a bye request
        if (sipAgent != null){
            try {
                sipAgent.sendBye();
                sipAgent.dispose();
            } catch (SipException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            sentBye = true;
        }

        //clean up the media client (stop streaming)
        if (mediaClient != null) {
            mediaClient.stop();
        }

        changeStatus(SessionStateType.none, RecognitionStateType.none);
    }

    
    private void changeStatus(SessionStateType newSessionState, RecognitionStateType newRecognitionState) {
        
        //check if a change , so that only changes trigger a repaint
        if ((newSessionState != sessionState) ||  (recognitionState != newRecognitionState)) {
           sessionState = newSessionState;
           recognitionState = newRecognitionState;
           _logger.debug("Changing State: "+sessionState+"  "+recognitionState);
           //trigger new paint() call
           repaint();
        }
        
    }

    private  SdpMessage constructResourceMessage(int localRtpPort, Vector format) throws UnknownHostException, SdpException {
        SdpMessage sdpMessage = SdpMessage.createNewSdpSessionMessage(_mySipAddress, _publicLocalHostName, "The session Name");
        MediaDescription rtpChannel = SdpMessage.createRtpChannelRequest(localRtpPort, format);
        MediaDescription synthControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHSYNTH);
        MediaDescription recogControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHRECOG);
        Vector v = new Vector();
        v.add(synthControlChannel);
        v.add(recogControlChannel);
        v.add(rtpChannel);
        sdpMessage.getSessionDescription().setMediaDescriptions(v);
        return sdpMessage;
    }

    
    /**
     * TODOC
     * @param String representation of this object (generated by the toString() method)
     * @return JSON String representation of the recognition result
     */
    public  String toJSONString(RecognitionResult result) {
        
        System.out.println("Converting to JSON. "+ result.toString());
        
        JSONStringer jj = new JSONStringer();
        String s = null;
        
        if (result == null)
            return null;
        
        try {
            jj.object();
            //jj.key("input");
            //jj.value(result.getText());
            //jj.key("slots");
            //jj.array();
            for (int i = 0; i < result.getRuleMatches().size(); i++) {
                System.out.println("rule matche # "+i+ " is "+result.getRuleMatches().get(i).getRule() +" :: "+result.getRuleMatches().get(i).getTag() );
                //jj.object();
                jj.key(result.getRuleMatches().get(i).getRule());
                jj.value(result.getRuleMatches().get(i).getTag());
                
                //hack in the url
                if (result.getRuleMatches().get(i).getRule().equals("link")) {
                    jj.key("url");
                    jj.value(urls.get(result.getRuleMatches().get(i).getTag()));
                }
                //jj.endObject();
            }
            //jj.endArray();
            jj.endObject();
       
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        
        
        /*try {
            s = jj
                    .object()
                        .key("input")
                        .value(result.getText())
                        .key("slots")

                        .array()
                            .object()
                                .key("slot1")
                                .value("value1")
                            .endObject()
                            .object()
                                .key("slot2-array1")
                                .array()
                                   .value("array1value1")
                                   .value("array1value2")
                                .endArray()
                            .endObject()
                            .object()
                                .key("Slot3-array2")
                                .array()
                                   .value("array2value1")
                                   .value("array2value2")
                                .endArray()
                            .endObject()
                        .endArray()
                    .endObject()
            .toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }  */
        
        return jj.toString();
    }
    
    public void characterEventReceived(String arg0, EventType arg1) {
        // TODO Auto-generated method stub
        
    }
    
    //------------------------------------------------------------------------------------------------------------------------
    //Blocking methods
    //------------------------------------------------------------------------------------------------------------------------

    private void ProcessRecognitionResult(RecognitionResult result) {

        if (result != null) {
      
        if( (!result.getRuleMatches().isEmpty()) && (!result.isOutOfGrammar())) {
            _logger.info("PRocessing Rec result: "+result.toString());
            //_logger.info("text:"+result.getText()+" matches:"+result.getRuleMatches()+" oog flag:"+result.isOutOfGrammar());
            boolean recogModeCommand = false;
            
            //check if the rule is a recognition mode change rule.  if so no need to pass it on 
            //to the javascript/browser.  But will change the state so the next recog command will
            //sue the proper grammar and command type (hotword or normal)
            List<RuleMatch> rules = result.getRuleMatches();
            for(RuleMatch rule : rules) {
                _logger.info(rule.getTag()+"/"+rule.getRule());
                if  (rule.getRule().equals("recognitionMode")) {
                    recogModeCommand = true;
                
                    if  (rule.getTag().equals("hotword")) {
                        changeStatus(sessionState,RecognitionStateType.hotword); 
                    } else if (rule.getTag().equals("normal")) {
                        changeStatus(sessionState,RecognitionStateType.normal);
                    }
                    _logger.info("Recog mode change command to "+rule.getTag()+" so no call to javascript...");
                }

            }
            // if there was not recognition change command, go ahead and pass info on to the web page/javascript
            if (!recogModeCommand) {
               String[] params = new String[2];
               params[0] = result.getText();
               params[1] = toJSONString(result);
            
                _window.call("recognitionEvent", params); 
            } else {
                
            }
        } else {
            _logger.info("NO results to process and pass to javascript...");
        }
        }else {
            _logger.info("NULL Result returned from speech server.");
        }
    }
    

    public void playAndRecognizeBlocking(Boolean urlPrompt, String prompt, String grammarUrl) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException {
        RecognitionResult rr = _client.playAndRecognizeBlocking(urlPrompt, prompt, grammarUrl, false);
        ProcessRecognitionResult(rr);
    }
    
    public void playBlocking(Boolean urlPrompt, String prompt)  throws IOException, MrcpInvocationException, InterruptedException, NoMediaControlChannelException {
        _client.playBlocking(urlPrompt, prompt);
    }

    public RecognitionResult recognizeBlocking(String grammarUrl, Boolean hotword, long noInputTimeout)throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException {
        //String fullUrl = "http://"+_cairoSipHostName+":"+_peerHostPort+grammarUrl;
        //String fullUrl = "http://"+_cairoSipHostName+":"+_peerHostPort+"/grammar/href-example.gram";
        
        String[] params = new String[1];
        if (latestResult != null) {
           params[0] = "Latest result was "+ latestResult+ ", ready...";
        } else {
            params[0] = "Ready...";
        }
        _window.call("setStatus", params); 
        
        _logger.info("GrammarURL is: "+grammarUrl +"hotword flag is: "+hotword);
        RecognitionResult rr = _client.recognizeBlocking(grammarUrl,hotword,false, noInputTimeout);
        return (rr);

    }
    //------------------------------------------------------------------------------------------------------------------------
    

    //methods to the speech server
    
    public void recognize(String grammarUrl, Boolean hotword) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException {
        //String fullUrl = "http://"+_cairoSipHostName+":"+_peerHostPort+grammarUrl;
        //String fullUrl = "http://"+_cairoSipHostName+":"+_peerHostPort+"/grammar/href-example.gram";
        _logger.info("GrammarURL is: "+grammarUrl +"hotword flag is: "+hotword);
        SpeechRequest sr = _client.recognize(grammarUrl, hotword, false, 0);
  
    }



    //callbacks from the recognition server (to be forwarded to the web page thru javascript with JSON params
    
    public void recognitionEventReceived(MrcpEvent event, RecognitionResult result) {
        _logger.info("Received recognition event: "+event.toString());
        //_logger.info("Recog result: "+result.toString());

        String[] params = new String[1];
        MrcpEventName eventName = event.getEventName();
        params[0] = event.getEventName().name();
        if (MrcpEventName.RECOGNITION_COMPLETE.equals(eventName)) {
            if (result != null) {
                latestResult = result.getText();
            }else {
                latestResult =  " : No Results";
            }
            params[0] = params[0]  + " : "+latestResult;
        }
        _window.call("setStatus", params); 
        
   
    }

    // methods used by control on web page to control the speech features on the page
    
     public void  establishSession() {
             getConnectionParams();
             startSession(); 
     }

     public void terminateSession() {
             _logger.info("Session not yet fully established, terminating anyway...");
             stopSession();
     }

     public void  enableRecognition() {
         if (sessionState == SessionStateType.none) {
             _logger.warn("Can not start recognition, establish session first.");
         } else if (sessionState == SessionStateType.established) {
             _logger.info("enabling normal recognition");
             if (recognitionState == RecognitionStateType.normal) {
                 _logger.warn("Recognition already in normal mode.");
             } else if ((recognitionState == RecognitionStateType.hotword) ||
                        (recognitionState == RecognitionStateType.none)){
                 
                 changeStatus(sessionState, RecognitionStateType.normal);
                 
                 /*try {
                    _client.stopActiveRecognitionRequests();
                 } catch (MrcpInvocationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                 } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                 } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                 }*/
               
       

             } else {
                 _logger.warn("Trying to go to normal mode from currnet Recognition mode = "+recognitionState);
             }
         } else if (sessionState == SessionStateType.establishing) {
             _logger.warn("Can not start recognition, session in process of being established -- but not established yet.");
         } else {
             _logger.warn("Unhandled session status "+sessionState);
         }

     }

     //not really totally disabled, but in hotword mode
     //TODO:  maybe a totally disabled mode is needed (now the terminate session is the way to do that...)
     public void  disableRecognition() {
         if (sessionState == SessionStateType.none) {
             _logger.warn("Can not start recognition, establish session first.");
         } else if (sessionState == SessionStateType.established) {
             _logger.info("enabling normal recognition");
             if ((recognitionState == RecognitionStateType.normal) || 
                 (recognitionState == RecognitionStateType.none)){
                 
                 changeStatus(sessionState, RecognitionStateType.hotword);
                 
              /*   try {
                    _client.stopActiveRecognitionRequests();
                } catch (MrcpInvocationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } */
     

             } else if (recognitionState == RecognitionStateType.hotword) {
                 _logger.warn("Recognition already in hotword mode.");
    
             } else {
                 _logger.warn("Trying to go to normal mode from currnet Recognition mode = "+recognitionState);
             }
         } else if (sessionState == SessionStateType.establishing) {
             _logger.warn("Can not start recognition, session in process of being established -- but not established yet.");
         } else {
             _logger.warn("Unhandled session status "+sessionState);
         }
     }
     
     private class RecognitionControl extends Thread {
         public void run() {
 
                 String[] params = new String[1];    //just returning two parameters to the javascript
                 params[0] = "dummy"; 
                 //init the recognition session
                 RecognitionResult rr;
                 while (!done) {
                                         
                     if ((sessionState == SessionStateType.established) || (sessionState == SessionStateType.fault)){
                         if (recognitionState == RecognitionStateType.normal ) {
                             _logger.info("Starting Normal Recognition...");
                             
                             
                             try {
                                 Object obj = _window.call("getNormal", params);
                                 String grammarUrl = (String) obj;
                                 rr = recognizeBlocking(grammarUrl,false,_noInputTimeout);
                                 ProcessRecognitionResult(rr);
                                 changeStatus(SessionStateType.established,recognitionState);;
                             } catch (MrcpInvocationException e) {
                                 // TODO Auto-generated catch block
                                 
                                 //TODO:  Most errors ar due to no resource available.  Need to ditinguish between these types of errors
                                 //       this approach of just waiting and trying again -- is not correct for all faults
                                 _logger.warn("MRCP eror is "+ e.getResponse().getStatusCode());
                                 changeStatus(SessionStateType.fault,recognitionState); 
                                 try {
                                     sleep(3000);
                                 } catch (InterruptedException e1) {
                                     // TODO Auto-generated catch block
                                     e.printStackTrace();
                                 }

                             } catch (IllegalValueException e) {
                                 // TODO Auto-generated catch block
                                 e.printStackTrace();
                             } catch (IOException e) {
                                 // TODO Auto-generated catch block
                                 e.printStackTrace();
                             } catch (InterruptedException e) {
                                 // TODO Auto-generated catch block
                                 e.printStackTrace();
                             } catch (NoMediaControlChannelException e) {
	                            // TODO Auto-generated catch block
	                            e.printStackTrace();
                            }
                         } else if (recognitionState == RecognitionStateType.hotword ) {
                             _logger.info("Starting Hotword Recognition...");
                            try {
                                Object obj = _window.call("getHotword", params);
                                String grammarUrl = (String) obj;
                                rr = recognizeBlocking(grammarUrl,true,_noInputTimeout);
                                ProcessRecognitionResult(rr);
                                changeStatus(SessionStateType.established,recognitionState);;
                            } catch (MrcpInvocationException e) {
                                // TODO Auto-generated catch block
                                _logger.warn("MRCP eror is "+ e.getResponse().getStatusCode());
                                changeStatus(SessionStateType.fault,recognitionState); 
                                try {
                                    sleep(3000);
                                } catch (InterruptedException e1) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            } catch (IllegalValueException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (NoMediaControlChannelException e) {
	                            // TODO Auto-generated catch block
	                            e.printStackTrace();
                            }
 
                         } else {

                             _logger.info("No recognition becuase recognition state is: "+recognitionState);
                             try {
                                 sleep(3000);
                             } catch (InterruptedException e) {
                                 // TODO Auto-generated catch block
                                 e.printStackTrace();
                             }
                         }

                         
                     } else {
                         _logger.info("No recognition becuase session state is: "+sessionState); 
                         try {
                             sleep(3000);
                         } catch (InterruptedException e) {
                             // TODO Auto-generated catch block
                             e.printStackTrace();
                         }
                     }
   
                 }
         }
     }

    public void speechSynthEventReceived(MrcpEvent arg0) {
        // TODO Auto-generated method stub
        
    }


}
