package actors.messages

case class ErrorSend(error:String, statusCode: Int) extends Throwable with ActorMessage {
  override def getMessage: String = error
}
