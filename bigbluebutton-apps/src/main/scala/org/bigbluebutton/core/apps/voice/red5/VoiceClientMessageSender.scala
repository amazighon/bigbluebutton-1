package org.bigbluebutton.core.apps.voice.red5

import com.google.gson.Gson
import org.bigbluebutton.conference.meeting.messaging.red5.ConnectionInvokerService
import org.bigbluebutton.conference.meeting.messaging.red5.BroadcastClientMessage
import org.bigbluebutton.core.api._

class VoiceClientMessageSender(service: ConnectionInvokerService) extends OutMessageListener2 {

  def handleMessage(msg: IOutMessage) {
    msg match {
      case msg: SipVideoUpdated               => handleSipVideoUpdated(msg)
      case msg: GlobalVideoStreamInfoMessage  => handleGlobalVideoStreamInfoMessage(msg)
      case _ => // do nothing
    }
  }

  private def handleSipVideoUpdated(msg: SipVideoUpdated) {
    val args = new java.util.HashMap[String, Object]()
    args.put(Constants.VOICE_CONF, msg.voiceBridge)
    args.put("isSipVideoPresent", msg.sipVideoPresent:java.lang.Boolean)
    args.put("activeTalker", msg.activeTalker)

    val message = new java.util.HashMap[String, Object]()
    val gson = new Gson()
    message.put("msg", gson.toJson(args))

    var m = new BroadcastClientMessage(msg.meetingID, "SipVideoUpdate", message)
    service.sendMessage(m)
  }

  private def handleGlobalVideoStreamInfoMessage(msg: GlobalVideoStreamInfoMessage) {
    val args = new java.util.HashMap[String, Object]()
    args.put(Constants.GLOBAL_VIDEO_STREAM_NAME, msg.globalVideoStreamName)

    val message = new java.util.HashMap[String, Object]()
    val gson = new Gson()
    message.put("msg", gson.toJson(args))

    var m = new BroadcastClientMessage(msg.meetingID, MessageNames.GLOBAL_VIDEO_STREAM_INFO, message)
    service.sendMessage(m)
  }
}
