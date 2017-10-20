package vidispine

import java.util.Base64

import actors.messages.ErrorSend
import akka.event.DiagnosticLoggingAdapter
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import com.softwaremill.sttp._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait VSCommunicator {
  protected val plutoUser:String
  protected val plutoPass:String

  private def authString:String = Base64.getEncoder.encodeToString(s"$plutoUser:$plutoPass".getBytes)

  protected implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]]
  protected implicit val logger:DiagnosticLoggingAdapter

  private def sendPut(uri:Uri, xmlString:String, headers:Map[String,String]):Future[Response[Source[ByteString, Any]]] = {
    val bs = ByteString(xmlString,"UTF-8")

    logger.info("Got xml body")
    val source:Source[ByteString, Any] = Source.single(bs)
    val hdr = Map(
      "Accept"->"application/xml",
      "Authorization"->s"Basic $authString",
      "Content-Type"->"application/xml"
    ) ++ headers

    logger.info(hdr.toString())
    logger.info(s"Got headers, initiating send to $uri")
    sttp
      .put(uri)
      .streamBody(source)
      .headers(hdr)
      .response(asStream[Source[ByteString, Any]])
      .send()
  }

  private def consumeSource(source:Source[ByteString,Any])(implicit logger:DiagnosticLoggingAdapter, materializer: akka.stream.Materializer, ec: ExecutionContext):Future[String] = {
    logger.info("Consuming returned body")
    val sink = Sink.reduce((acc:ByteString, unit:ByteString)=>acc.concat(unit))
    val runnable = source.toMat(sink)(Keep.right)
    runnable.run().map(_.utf8String)
  }

  def request(uri:Uri,xmlString:String,headers:Map[String,String])
             (implicit logger:DiagnosticLoggingAdapter, materializer: akka.stream.Materializer,ec: ExecutionContext):Future[Try[Future[String]]] = sendPut(uri, xmlString, headers).map({ response=>
    response.body match {
      case Right(source)=>
        logger.info("Send succeeded")
        Success(consumeSource(source))
      case Left(errorString)=>
        VSError.fromXml(errorString) match {
          case Left(unparseableError)=>
            val errMsg = s"Send failed: ${response.code} - $errorString"
            logger.warning(errMsg)
            Failure(ErrorSend(errMsg))
          case Right(vsError)=>
            logger.warning(vsError.toString)
            Failure(ErrorSend(vsError.toString))
        }
    }})
}
