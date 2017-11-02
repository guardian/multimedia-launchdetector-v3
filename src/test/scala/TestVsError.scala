import org.scalatest.{FunSuite, MustMatchers, PrivateMethodTester}
import akka.event.{DiagnosticLoggingAdapter, Logging}

class TestVsError extends FunSuite with MustMatchers {
  private implicit val myLogger:DiagnosticLoggingAdapter = new DiagnosticLoggingAdapter {
    override protected def notifyError(message: String): Unit = {}

    override protected def notifyError(cause: Throwable, message: String): Unit = {}

    override protected def notifyDebug(message: String): Unit = {}

    override def isInfoEnabled: Boolean = false

    override protected def notifyWarning(message: String): Unit = {}

    override def isErrorEnabled: Boolean = false

    override protected def notifyInfo(message: String): Unit = {}

    override def isDebugEnabled: Boolean = false

    override def isWarningEnabled: Boolean = false
  }

  test("VSError should parse a returned xml string"){
    val testXmlString = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><ExceptionDocument xmlns="http://xml.vidispine.com/schema/vidispine"><invalidInput><context>metadata-field-group</context><explanation>No group name is specified</explanation></invalidInput></ExceptionDocument>"""
    val result = VSError.fromXml(testXmlString)

    result must be('right)

//    result.map({
//      case VSErrorInvalidInput(error)=>
//        error.context must be("metadata-field-group")
//        error.explanation must be("No group name is specified")
//        error.exceptType must be("invalidInput")
//    })

  }
}
