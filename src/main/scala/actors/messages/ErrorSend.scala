package actors.messages

case class ErrorSend(error:String) extends Throwable with ActorMessage {
  override def getMessage: String = error
}
