package fitzroy.components

import fitzroy.clocks.{TS, TimeStamp}

case class Message(cmd: Command, senderId: ProcessId, ts: TimeStamp = TS()) {
  override def toString: String = "Message from Node" + senderId + ": " + cmd + " at " + ts
}
