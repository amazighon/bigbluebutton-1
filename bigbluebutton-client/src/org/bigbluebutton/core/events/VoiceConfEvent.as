/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
*
* Copyright (c) 2010 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 2.1 of the License, or (at your option) any later
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
package org.bigbluebutton.core.events
{
	import flash.events.Event;

	public class VoiceConfEvent extends Event
	{
		public static const MUTE_ALL:String = "VOICECONF_MUTE_ALL";
		public static const UNMUTE_ALL:String = "VOICECONF_UNMUTE_ALL";
		public static const LOCK_MUTE_USER:String = "LOCK_MUTE_USER";
		public static const MUTE_ALMOST_ALL:String = "VOICECONF_MUTE_ALMOST_ALL";
		
		public static const MUTE_USER:String = "VOICECONF_MUTE_USER";
		public static const UNMUTE_USER:String = "VOICECONF_UNMUTE_USER";
		
		public static const EJECT_USER:String = "VOICECONF_EJECT_USER";

		public static const DIAL:String = "VOICECONF_DIAL";
		public static const DIALING:String = "VOICECONF_DIALING";
		public static const HANGINGUP:String = "VOICECONF_HANGINGUP";
		public static const CANCEL_DIAL:String = "VOICECONF_CANCEL_DIAL";
		public static const SEND_DTMF:String = "VOICECONF_SEND_DTMF";
		
		public var userid:String;
		public var mute:Boolean;
		public var lock:Boolean;
		
		public var dialOptions:Array;
		public var dialParams:Array;
		public var uuid:String;
		public var dialState:String;
		public var dialHangupCause:String;
		public var dtmfDigit:String;
		
		public function VoiceConfEvent(type:String)
		{
			super(type, true, false);
		}
	}
}