package com.speechforge.masher.applet;

import netscape.javascript.JSObject;

import org.speechforge.cairo.rtp.NativeMediaClient;
import org.speechforge.cairo.rtp.RTPConsumer;

import org.speechforge.cairo.sip.SdpMessage;
import org.speechforge.cairo.sip.SimpleSipAgent;
import org.speechforge.cairo.sip.SipSession;
import java.applet.Applet;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Vector;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sip.SipException;
import org.apache.log4j.Logger;
import org.mrcp4j.MrcpResourceType;

import com.speechforge.masher.applet.test.AppletProperties;

/**
 * Simple Streaming applet.  streams audio from the local microphone to the speech server adn from the server to the local speakers.
 *
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */

public class StreamingApplet extends Applet  {

    private static final long serialVersionUID = 1L;

    private static Logger _logger = Logger.getLogger(StreamingApplet.class);

    private  SimpleSipAgent sipAgent;
    private   boolean sentBye=false;
    private  String _localHostName = null;
    private InetAddress _localAddress = null;

    InetAddress _cairoSipInetAddress = null;
    private  String _cairoSipHostName;
    private  int _peerHostPort;
    private  int _localRtpPort = 42046;
    private  int _localSipPort = 5090;
    private  int _cairoSipPort = 5060;
    private  String _mySipAddress = null;
    private  String _cairoSipAddress = null;
    private  String _sdpSessionName = null;  

    public JSObject _window = null;
    //StringBuffer buffer;
    
    public void init() { 
        AppletProperties.print();
        _window = (JSObject) JSObject.getWindow(this);
    }

    public void start() {
        getConnectionParams();
        //after getting the parameters from the page, it calls
        //init() and  startSession();
    }

    public void stop() {
        _logger.info("stopping... ");
        stopSession();
    }

    public void destroy() {
        _logger.info("preparing for unloading...");
    }

    public void getConnectionParams() {
        //Get the applet parameters to be used for establishing the conncetion to the speech server
        // sip ports, rtp port, sip addresses, sdp session name
        
        int localSipPort = 5090;
        String tempString;
        tempString = getParameter("LOCALSIPPORT");
        if (tempString != null) {
            try {
                localSipPort = Integer.parseInt(tempString);
            } catch (NumberFormatException e) {
                localSipPort = 5090;
            }
        }
        
        String mySipAddress = getParameter("LOCALSIPADDRESS");
        if (mySipAddress == null)
            mySipAddress ="sip:speechSynthClient@speechforge.org";
        
        initLocalSipAgent(localSipPort, mySipAddress);
        
        
        //CAIRO SIP PORT
        int cairoSipPort = 5060;
        tempString = getParameter("CAIROSIPPORT");
        if (tempString != null) {
            try {
                cairoSipPort = Integer.parseInt(tempString);
            } catch (NumberFormatException e) {
                cairoSipPort = 5060;
            }
        }
        
        //CAIRO SIP HOST
        // The Voice Server needs to run on the same server from which the applet was loaded
        // so get the codbase host
        //_peerHostName = getCodeBase().getHost();
        _peerHostPort = getCodeBase().getPort();
        
        //passing in the remote host name from the javascript
        //maybe this will fix issues with localhost
        String cairoSipHostName = getParameter("REMOTEHOSTNAME");
        System.out.println("ph :"+cairoSipHostName);
        if (cairoSipHostName == null)
            cairoSipHostName = getCodeBase().getHost();
        
        //CAIRO SIP ADDRESS
        String cairoSipAddress = getParameter("CAIROSIPADDRESS");
        if (cairoSipAddress == null)
            cairoSipAddress="sip:cairo@speechforge.org";
        
        //LOCAL RTP PORT
        int localRtpPort = 42046;
        tempString = getParameter("LOCALRTPPORT");
        if (tempString != null) {
            try {
                localRtpPort = Integer.parseInt(tempString);
            } catch (NumberFormatException e) {
                localRtpPort = 42046;
            }
        }

        String sdpSessionName = getParameter("SDPSESSIONNAME");
        if (sdpSessionName == null)
            sdpSessionName ="voice.web";

        startSession(cairoSipPort, cairoSipHostName, cairoSipAddress, localRtpPort, sdpSessionName);
    }
    
    
    public void startSession(int cairoSipPort, String cairoSipHostName, String cairoSipAddress, int localRtpPort, String sdpSessionName) {

        _cairoSipPort = cairoSipPort;
        _cairoSipHostName = cairoSipHostName;
        try {
            _cairoSipInetAddress = InetAddress.getByName(cairoSipHostName);
        } catch (UnknownHostException e1) {
            System.err.println("Couldn't get Internet address: Unknown host");
            e1.printStackTrace();

        }         

        System.out.println("cairo sip host name: "+cairoSipHostName);
        System.out.println("cairo sip inet address: "+_cairoSipInetAddress);
        if ((_cairoSipInetAddress == null) || (cairoSipHostName.length() == 0)) {
            //Just use the localhost if there is no codebase host (for testing)
            _cairoSipHostName=_localHostName;
            _cairoSipInetAddress=_localAddress;
        }
        
        _cairoSipAddress = cairoSipAddress;
        
        _localRtpPort = localRtpPort;
        if (_localRtpPort < 0 || _localRtpPort >= RTPConsumer.TCP_PORT_MAX || _localRtpPort % 2 != 0) {
            _logger.warn("Improper format for first command line argument <local-rtp-port>," +
                " should be even integer between 0 and " + RTPConsumer.TCP_PORT_MAX);
        }
        
        //TODO:  Where should teh format be specified?
        Vector format = new Vector();
        format.add("0");           //PCMU
        
        _sdpSessionName = sdpSessionName;
        
        
        // Construct a SIP agent to be used to send a SIP Invitation to the ciaro server
        try {
            sipAgent = new SimpleSipAgent(_mySipAddress, "Synth Client Sip Stack", _localSipPort, "UDP");
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
        try {
            inviteResponse = sipAgent.sendInviteWithoutProxy(_cairoSipAddress, message, _cairoSipHostName, _cairoSipPort);
        } catch (SipException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if (inviteResponse != null) {
            _logger.info("Received the SIP Response.");

            
            Vector remoteFormats = null;
            int remoteRtpPort = 0;
            try {
                for (MediaDescription md : inviteResponse.getRtpChannels()) {
                    remoteRtpPort = md.getMedia().getMediaPort();
                    remoteFormats = md.getMedia().getMediaFormats(true);
                    //System.out.println("Individual Media connection address: "+ md.getConnection().getAddress());
                }
            } catch (SdpException e) {
                _logger.debug(e, e);
                e.printStackTrace();
            }

            _logger.debug("Starting NativeMediaClient...");
            NativeMediaClient mediaClient = null;
            try {
                mediaClient = new NativeMediaClient(_localRtpPort, _cairoSipInetAddress, remoteRtpPort);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            mediaClient.startTransmit();

            
        } else {
            //Invitation Timeout
            _logger.info("Sip Invitation timed out.  Is server running?");
        }

    }
    
    public void stopSession() {
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
    }
    
    
    private  SdpMessage constructResourceMessage(int localRtpPort, Vector format) throws UnknownHostException, SdpException {
        SdpMessage sdpMessage = SdpMessage.createNewSdpSessionMessage(_mySipAddress, _localHostName, "The session Name");
        MediaDescription rtpChannel = SdpMessage.createRtpChannelRequest(localRtpPort, format);
        //MediaDescription synthControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHSYNTH);
        //MediaDescription recogControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHRECOG);
        Vector v = new Vector();
        //v.add(synthControlChannel);
        //v.add(recogControlChannel);
        v.add(rtpChannel);
        sdpMessage.getSessionDescription().setMediaDescriptions(v);
        return sdpMessage;
    }



    public void initLocalSipAgent(int localSipPort, String mySipAddress) {
        _localSipPort = localSipPort;
        _mySipAddress = mySipAddress;
        
        
        //get the local host name

        try {
            _localAddress = InetAddress.getLocalHost();
            _localHostName = InetAddress.getLocalHost().getHostAddress();      
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }    
    }

    public void startSession(SipSession session) {
        // TODO Auto-generated method stub
        
    }

}
