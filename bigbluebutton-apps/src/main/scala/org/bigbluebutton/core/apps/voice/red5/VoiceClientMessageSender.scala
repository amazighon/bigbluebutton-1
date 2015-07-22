package org.bigbluebutton.core.apps.voice.red5

import com.google.gson.Gson
import org.bigbluebutton.conference.meeting.messaging.red5.ConnectionInvokerService
import org.bigbluebutton.conference.meeting.messaging.red5.BroadcastClientMessage
import org.bigbluebutton.core.api._

class VoiceClientMessageSender(service: ConnectionInvokerService) extends OutMessageListener2 {

  def handleMessage(msg: IOutMessage) {
    msg match {
      case msg: SipVideoUpdated               => handleSipVideoUpdated(msg)
      case _ => // do nothing
    }
  }

  private def handleSipVideoUpdated(msg: SipVideoUpdated) {
    val args = new java.util.HashMap[String, Object]()
    args.put("voiceConf", msg.voiceBridge)
    args.put("isSipVideoPresent", msg.isSipVideoPresent:java.lang.Boolean)
    args.put("sipVideoStreamName", msg.sipVideoStreamName)
    args.put("talkerUserId", msg.talkerUserId)
    args.put("width", msg.width)
    args.put("height", msg.height)

    val message = new java.util.HashMap[String, Object]()
    val gson = new Gson()
    message.put("msg", gson.toJson(args))

    var m = new BroadcastClientMessage(msg.meetingID, "sipVideoUpdate", message)
    service.sendMessage(m)
  }
}
