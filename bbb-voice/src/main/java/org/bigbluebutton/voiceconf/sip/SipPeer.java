/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
* 
* Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 3.0 of the License, or (at your option) any later
* version.
* 
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
*
*/
package org.bigbluebutton.voiceconf.sip;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.zoolu.sip.provider.*;
import org.zoolu.net.SocketAddress;
import org.slf4j.Logger;
import org.bigbluebutton.voiceconf.messaging.IMessagingService;
import org.bigbluebutton.voiceconf.red5.CallStreamFactory;
import org.bigbluebutton.voiceconf.red5.ClientConnectionManager;
import org.red5.app.sip.codecs.Codec;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.bigbluebutton.voiceconf.red5.media.transcoder.VideoTranscoder;

/**
 * Class that is a peer to the sip server. This class will maintain
 * all calls to it's peer server.
 * @author Richard Alam
 *
 */
public class SipPeer implements SipRegisterAgentListener {
    private static Logger log = Red5LoggerFactory.getLogger(SipPeer.class, "sip");

    private ClientConnectionManager clientConnManager;
    private CallStreamFactory callStreamFactory;
    
    private CallManager callManager = new CallManager();
    private IMessagingService messagingService;
    private SipProvider sipProvider;
    private String clientRtpIp;
    private SipRegisterAgent registerAgent;
    private final String id;
    private final ConferenceProvider confProvider;
    
    private boolean registered = false;
    private SipPeerProfile registeredProfile;
    private VideoTranscoder videoTranscoder = null;
    Map<String, VideoTranscoder> videoTranscoderForUserId = new HashMap<String, VideoTranscoder>();
    
    public SipPeer(String id, String sipClientRtpIp, String host, int sipPort, 
			int startAudioPort, int stopAudioPort, int startVideoPort, int stopVideoPort, IMessagingService messagingService) {
        this.id = id;
        this.clientRtpIp = sipClientRtpIp;
        this.messagingService = messagingService;
        confProvider = new ConferenceProvider(host, sipPort, startAudioPort, stopAudioPort, startVideoPort, stopVideoPort);
        initSipProvider(host, sipPort);
    }
    
    private void initSipProvider(String host, int sipPort) {
        sipProvider = new SipProvider(host, sipPort);    
        sipProvider.setOutboundProxy(new SocketAddress(host)); 
        sipProvider.addSipProviderListener(new OptionMethodListener());    	
    }
    
    public void register(String username, String password) {
    	log.debug( "SIPUser register" );
        createRegisterUserProfile(username, password);
        if (sipProvider != null) {
        	registerAgent = new SipRegisterAgent(sipProvider, registeredProfile.fromUrl, 
        			registeredProfile.contactUrl, registeredProfile.username, 
        			registeredProfile.realm, registeredProfile.passwd);
        	registerAgent.addListener(this);
        	registerAgent.register(registeredProfile.expires, registeredProfile.expires/2, registeredProfile.keepaliveTime);
        }                              
    }
    
    private void createRegisterUserProfile(String username, String password) {    	    	
    	registeredProfile = new SipPeerProfile();
    	registeredProfile.audioPort = confProvider.getStartAudioPort();
            	
        String fromURL = "\"" + username + "\" <sip:" + username + "@" + confProvider.getHost() + ">";
        registeredProfile.username = username;
        registeredProfile.passwd = password;
        registeredProfile.realm = confProvider.getHost();
        registeredProfile.fromUrl = fromURL;
        registeredProfile.contactUrl = "sip:" + username + "@" + sipProvider.getViaAddress();
        if (sipProvider.getPort() != SipStack.default_port) {
        	registeredProfile.contactUrl += ":" + sipProvider.getPort();
        }		
        registeredProfile.keepaliveTime=8000;
        registeredProfile.acceptTime=0;
        registeredProfile.hangupTime=20;   
        
        log.debug( "SIPUser register : {}", fromURL );
        log.debug( "SIPUser register : {}", registeredProfile.contactUrl );
    }

    public void call(String clientId, String callerName, String userId,String destination,String meetingId) {
    	if (!registered) {
    		/* 
    		 * If we failed to register with FreeSWITCH, reject all calls right away.
    		 * This way the user will know that there is a problem as quickly as possible.
    		 * If we pass the call, it take more that 30seconds for the call to timeout
    		 * (in case FS is offline) and the user will be kept wondering why the call
    		 * isn't going through.
    		 */
    		log.warn("We are not registered to FreeSWITCH. However, we will allow {} to call {}.", callerName, destination);
//    		return;
    	}

        CallAgent ca = createCallAgent(clientId, userId);
        ca.setMeetingId(meetingId);//set meetingId to use with fs->bbb video stream when call is accepted
        ca.call(callerName,userId, destination);
    	callManager.add(ca);
    }

	public void connectToGlobalStream(String clientId, String userId, String callerIdName, String destination) {
        CallAgent ca = createCallAgent(clientId,userId);

        ca.connectToGlobalStream(clientId, userId, callerIdName, destination);
     	callManager.add(ca);
	}

    private CallAgent createCallAgent(String clientId, String userId) {
    	SipPeerProfile callerProfile = SipPeerProfile.copy(registeredProfile);
        CallAgent ca = new CallAgent(this.clientRtpIp, sipProvider, callerProfile, confProvider, clientId, userId, messagingService);
    	ca.setClientConnectionManager(clientConnManager);
    	ca.setCallStreamFactory(callStreamFactory);

    	return ca;
    }

	public void close() {
		log.debug("SIPUser close1");
        try {
			unregister();
		} catch(Exception e) {
			log.error("close: Exception:>\n" + e);
		}

       log.debug("Stopping SipProvider");
       sipProvider.halt();
	}

    public void hangup(String userId) {
        log.debug( "SIPUser hangup" );

        CallAgent ca = callManager.remove(userId);

        if (ca != null) {
            if (ca.isListeningToGlobal()) {
                String destination = ca.getDestination();

                log.info("User has disconnected from global audio, user [{}] voiceConf {}", userId, destination);
                messagingService.userDisconnectedFromGlobalAudio(destination, userId);
            }
            ca.hangup();
        }
    }

    public void unregister() {
    	log.debug( "SIPUser unregister" );

    	Collection<CallAgent> calls = callManager.getAll();
    	for (Iterator<CallAgent> iter = calls.iterator(); iter.hasNext();) {
    		CallAgent ca = (CallAgent) iter.next();
    		ca.hangup();
    	}

        if (registerAgent != null) {
            registerAgent.unregister();
            registerAgent = null;
        }
    }

    public void startBbbToFreeswitchAudioStream(String clientId, String userId, IBroadcastStream broadcastStream, IScope scope) {
        CallAgent ca = callManager.get(userId);
        String videoStream = callManager.getVideoStream(userId);
        String meetingId = callManager.getMeetingId(userId);
        log.debug("Starting Audio Stream for the user ["+userId+"]");
        if (ca != null) {
            ca.startBbbToFreeswitchAudioStream(broadcastStream, scope);
            if ((videoStream != null) && (meetingId != null)){
                log.debug(" There's a VideoStream for this audio call, starting it ");
                ca.startBbbToFreeswitchVideoStream(videoStream,meetingId);
            }else log.debug("There's no videostream for this flash audio call yet.");
        }
    }
    
    public void stopBbbToFreeswitchAudioStream(String userId, IBroadcastStream broadcastStream, IScope scope) {
        CallAgent ca = callManager.get(userId);

        if (ca != null) {
           ca.stopBbbToFreeswitchAudioStream(broadcastStream, scope);
       
        } else {
        	log.info("Can't stop talk stream as stream may have already been stopped.");
        }
    }

    public void startBbbToFreeswitchVideoStream(String userId, String videoStreamName, String meetingId) {
        CallAgent ca = callManager.getByUserId(userId);
        if (ca != null){
            if(ca.isGlobalStream()){
                log.debug("This is a global CallAgent, there's no video stream to send from bbb to freeswitch");
            }else {
                ca.startBbbToFreeswitchVideoStream(videoStreamName, meetingId);
            }
        }
        else {
            log.debug("Could not START BbbToFreeswitchVideoStream: there is no CallAgent with"
                       + " userId " + userId + " (maybe this is an webRTC call?). Saving the current stream and scope to be used when the CallAgent is created by this user");            
            callManager.addVideoStream(userId,videoStreamName);
            callManager.addMeetingId(userId,meetingId);
        }
    }

    public void stopBbbToFreeswitchVideoStream(String userId) {
        CallAgent ca = callManager.getByUserId(userId);
        if (ca != null) {
           ca.stopBbbToFreeswitchVideoStream();
           callManager.removeVideoStream(userId);
           callManager.removeMeetingId(userId);
        }
        else
            log.debug("Could not STOP BbbToFreeswitchVideoStream: there is no CallAgent with"
                       + "userId " + userId);
        
    }

    public void startBbbToFreeswitchWebRTCVideoStream(String userId, String videoStreamName, String meetingId) {

        String ip = Red5.getConnectionLocal().getHost();
        String ports[] = callManager.getWebRTCPorts(userId);
        String remoteVideoPort="";
        String localVideoPort="";

        if (ports == null) {
            log.debug("There isn't any webRTCCall going on for this user. WebRTC Video will be transmited when the user to make one. Saving video parameters");
            callManager.addVideoStream(userId,videoStreamName);
            callManager.addMeetingId(userId,meetingId);
            return;
        }

        remoteVideoPort=ports[0];
        localVideoPort = ports[1];

        if (videoStreamName.equals("") || meetingId.equals("")){
            log.debug("There's no videoStream for this webRTCCall. Waiting for the user to enable your webcam");
            return;
        }

        //start webRTCVideoStream
        log.debug("{} is requesting to send video through webRTC. " + "[uid=" + userId + "]");
        videoTranscoder = new VideoTranscoder(VideoTranscoder.Type.TRANSCODE_RTMP_TO_RTP,videoStreamName,meetingId,ip,localVideoPort,remoteVideoPort);
        videoTranscoderForUserId.put(userId,videoTranscoder);
        videoTranscoder.start();
    }

    public void stopBbbToFreeswitchWebRTCVideoStream(String userId) {
        log.debug("Stopping webRTC video stream for the user: "+userId);
        callManager.removeVideoStream(userId);
        callManager.removeMeetingId(userId);

        //destroy processMonitor-ffmpeg for this user
        videoTranscoder = videoTranscoderForUserId.get(userId);
        if (videoTranscoder != null) {
            videoTranscoder.stop();
            videoTranscoder=null;
            videoTranscoderForUserId.remove(userId);
        }
    }

    public void startFreeswitchToBbbGlobalVideoStream(String userId) {
        CallAgent ca = callManager.getByUserId(userId);
        if (ca != null){
            if(ca.isGlobalStream()){ //this MUST be a globalStream, because the global is the only one that sends video
                log.debug("Starting GlobalCall's freeswitch->bbb video stream");
                ca.startFreeswitchToBbbVideoStream();
            }
            log.debug("startFreeswitchToBbbGlobalVideoStream(): There's no global call agent for the user: "+userId+" callerName: "+ ca.getCallerName());
        }
    }

    public void stopFreeswitchToBbbGlobalVideoStream(String userId) {
        CallAgent ca = callManager.getByUserId(userId);
        if (ca != null) {
            if(ca.isGlobalStream()) {
                ca.stopFreeswitchToBbbGlobalVideoStream();
            }
        }
        else
            log.debug("Could not STOP FreeswitchToBbbGlobalVideoStream: there is no Global CallAgent with"
                       + "userId " + userId);
    }

    public void saveWebRTCParameters(String userId, String username, String meetingId, String remoteVideoPort, String localVideoPort) throws PeerNotFoundException {
        String[] ports = {remoteVideoPort,localVideoPort};
        callManager.addWebRTCPorts(userId, ports);
        callManager.addVideoStream(userId,username);
        callManager.addMeetingId(userId,meetingId);
    }

    public void removeWebRTCParameters(String userId) throws PeerNotFoundException {
            callManager.removeWebRTCPorts(userId);
            callManager.removeVideoStream(userId);
            callManager.removeMeetingId(userId);
    }

    public String getStreamType(String userId, String streamName) {
        CallAgent ca = callManager.get(userId);
        if (ca != null) {
           return ca.getStreamType(streamName);
        }
        else
        {
            log.debug("[SipPeer] Invalid clientId");
            return null;
        }
    }

    public boolean isAudioStream(String userId, IBroadcastStream broadcastStream) {
        CallAgent ca = callManager.get(userId);
        if (ca != null) {
           return ca.isAudioStream(broadcastStream);
        }
        else
            return false;
    }

    public boolean isVideoStream(String userId, IBroadcastStream broadcastStream) {
        CallAgent ca = callManager.get(userId);
        if (ca != null) {
           return ca.isVideoStream(broadcastStream);
        }
        else
            return false;
    }

	@Override
	public void onRegistrationFailure(String result) {
		log.error("Failed to register with Sip Server.");
		registered = false;
	}

	@Override
	public void onRegistrationSuccess(String result) {
		log.info("Successfully registered with Sip Server.");
		registered = true;
	}

	@Override
	public void onUnregistedSuccess() {
		log.info("Successfully unregistered with Sip Server");
		registered = false;
	}
	
	public void setCallStreamFactory(CallStreamFactory csf) {
		callStreamFactory = csf;
	}
	
	public void setClientConnectionManager(ClientConnectionManager ccm) {
		clientConnManager = ccm;
	}
}
