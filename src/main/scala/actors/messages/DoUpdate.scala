package actors.messages

import com.gu.contentatom.thrift.Atom

case class DoUpdate(atom:Atom) extends ActorMessage
