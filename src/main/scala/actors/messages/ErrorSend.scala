package actors.messages

case class ErrorSend(error:String) extends Throwable {
  override def getMessage: String = error
}
