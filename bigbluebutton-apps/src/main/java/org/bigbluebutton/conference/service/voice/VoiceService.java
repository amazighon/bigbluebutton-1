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
package org.bigbluebutton.conference.service.voice;

import org.slf4j.Logger;
import org.red5.server.api.Red5;
import org.bigbluebutton.conference.BigBlueButtonSession;
import org.bigbluebutton.conference.Constants;
import org.red5.logging.Red5LoggerFactory;
import org.bigbluebutton.webconference.voice.ConferenceService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bigbluebutton.webconference.voice.Participant;

public class VoiceService {
	
	private static Logger log = Red5LoggerFactory.getLogger( VoiceService.class, "bigbluebutton" );
	
	private ConferenceService conferenceService;

	@SuppressWarnings("unchecked")
	public Map<String, List> getMeetMeUsers() {
		String voiceBridge = getBbbSession().getVoiceBridge();
		
    	log.debug("GetMeetmeUsers request for room[" + voiceBridge + "]");
    	ArrayList<Participant> p = conferenceService.getParticipants(voiceBridge);

		Map participants = new HashMap();
		if (p == null) {
			participants.put("count", 0);
		} else {		
			participants.put("count", p.size());
			if (p.size() > 0) {				
				participants.put("participants", arrayListToMap(p));
			}			
		}
		log.info("MeetMe::service - Sending " + p.size() + " current users...");
		return participants;
	}
	
	private Map<Integer, Map> arrayListToMap(ArrayList<Participant> alp) {
		log.debug("Converting arraylist to Map " + alp.size());
		Map<Integer, Map> result = new HashMap();
		
		for (Participant p : alp) {
			Map<String, Object> pmap = new HashMap();
			pmap.put("participant", p.getId());
			pmap.put("name", p.getName());
			pmap.put("muted", p.isMuted());
			pmap.put("talking", p.isTalking());
			pmap.put("locked", p.isMuteLocked());
			log.debug("[" + p.getId() + "," + p.getName() + "," + p.isMuted() + "," + p.isTalking() + "]");
			result.put(p.getId(), pmap);
		}
		
		return result;
	}
	
	public void muteAllUsers(boolean mute) {
		String conference = getBbbSession().getVoiceBridge();    	
    	log.debug("Mute all users in room[" + conference + "]");
    	conferenceService.mute(conference, mute);	   	
	}	
	
	public boolean isRoomMuted(){
		String conference = getBbbSession().getVoiceBridge(); 
    	return conferenceService.isRoomMuted(conference);	
	}
	
	public void muteUnmuteUser(Integer userid,Boolean mute) {
		String conference = getBbbSession().getVoiceBridge();    	
    		log.debug("MuteUnmute request for user [" + userid + "] in room[" + conference + "]");
    		conferenceService.mute(userid, conference, mute);

		/*String room = conference;
		HashMap<String, String> options = new HashMap<String, String>(10);
		options.put("origination_callee_id_name", "Professor");
		options.put("effective_caller_id_name", "Student");
		options.put("origination_caller_id_name", "Student");

		HashMap<String, String> params = new HashMap<String, String>(10);
		params.put("module_profile", "sofia/external/");
		params.put("destination", "72013@10.0.3.211");
		params.put("caller_number", "72100");
		params.put("caller_name", "Professor");

		System.out.println("[VoiceService] conferenceService.dial()");
		conferenceService.dial(room, options, params);*/
	}

	public void dial(Map<String, String> opt, Map<String, String> par) {
		String conference = getBbbSession().getVoiceBridge();
		log.debug("Dial from [" + conference + "] to destination [" + par.get("destination") + "]");

		HashMap<String, String> options = new HashMap<String, String>(opt);
		HashMap<String, String> params = new HashMap<String, String>(par);
                                        
		conferenceService.dial(conference, options, params);
	}
	
	public void cancelDial(String cancelDialIdName, String cancelDialDestination) {
		String conference = getBbbSession().getVoiceBridge();
		log.debug("Cancel dial from [" + conference + "].");

		conferenceService.cancelDial(conference, cancelDialIdName, cancelDialDestination);
	}
	
	public void clearDial(String cancelDialIdName, String cancelDialDestination) {
		String conference = getBbbSession().getVoiceBridge();
		log.debug("Clear dial from [" + conference + "].");

		conferenceService.clearDial(conference, cancelDialIdName, cancelDialDestination);
	}

	public void lockMuteUser(Integer userid, Boolean lock) {
		String conference = getBbbSession().getVoiceBridge();    	
    	log.debug("Lock request for user [" + userid + "] in room[" + conference + "]");
    	conferenceService.lock(userid, conference, lock);
	}
	
	public void kickUSer(Integer userid) {
		String conference = getBbbSession().getVoiceBridge();		
    	log.debug("KickUser " + userid + " from " + conference);		
		conferenceService.eject(userid, conference);
	}
	
	public void setConferenceService(ConferenceService s) {
		log.debug("Setting voice server");
		conferenceService = s;
		log.debug("Setting voice server DONE");
	}
	
	private BigBlueButtonSession getBbbSession() {
		return (BigBlueButtonSession) Red5.getConnectionLocal().getAttribute(Constants.SESSION);
	}
}
