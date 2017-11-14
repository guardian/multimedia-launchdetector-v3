package actors.messages

import com.gu.contentatom.thrift.ChangeRecord

case class MasterNotFound(atomId: String,contentChangeCreated: Option[ChangeRecord], contentChangeLastUpdated:Option[ChangeRecord], attempt: Int) extends ActorMessage
