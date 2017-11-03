package vidispine
import scala.util.Try
import scala.xml._
import akka.event._

class VSError(errorMessage:String) extends Throwable {
  override def getMessage: String = errorMessage

  override def toString: String = getMessage
}

object VSError {
  def fromXml(xmlText: String)(implicit logger: DiagnosticLoggingAdapter):Either[String,VSError] = {
    try {
      logger.info(s"raw xml: $xmlText")
      fromXml(XML.loadString(xmlText))
    } catch {
      case except:Exception=>Left(xmlText)
    }
  }

  def fromXml(xmlElem: Node):Either[String,VSError] = {
    val exceptType = xmlElem.child.head.label
    if(exceptType=="invalidInput") {
      Right(VSErrorInvalidInput(exceptType, (xmlElem \\ "context").text, (xmlElem \\ "explanation").text))
    } else if(exceptType=="notFound") {
      Right(VSErrorNotFound(exceptType, (xmlElem \\ "type").text, (xmlElem \\ "id").text))
    } else {
      Left(xmlElem.toString())
    }
  }
}

case class VSErrorNotFound(exceptType: String, entityType: String, entityId: String) extends VSError(exceptType){
  override def getMessage: String = s"$exceptType: $entityType $entityId"
}

case class VSErrorInvalidInput(exceptType: String, context: String, explanation:String) extends VSError(exceptType){
  override def getMessage: String = s"$exceptType: $explanation (context is $context)"

}
