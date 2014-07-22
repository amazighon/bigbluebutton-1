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
package org.bigbluebutton.webconference.voice.freeswitch.actions;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class CancelDialCommand {

    private static final Logger log = 
        Red5LoggerFactory.getLogger(CancelDialCommand.class, "bigbluebutton");
        
    private final String COMMAND = "uuid_kill";
    private final String room;
    private String uuid;

    public CancelDialCommand(String room) {
        this.room = room;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String getRoom() {
        return this.room;
    }
    
    public String getUuid() {
        return this.uuid;
    }
    
    public String getCommand() {
        return this.COMMAND;
    }
    
    public String getCommandArgs() {
	    return this.uuid;
    }
}
