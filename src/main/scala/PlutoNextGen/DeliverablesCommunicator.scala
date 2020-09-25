package PlutoNextGen

import java.security.MessageDigest
import java.util.Base64

import actors.messages.ErrorSend
import akka.event.DiagnosticLoggingAdapter
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import com.softwaremill.sttp.{Response, SttpBackend, Uri, asStream, sttp}
import org.apache.commons.codec.digest.{DigestUtils, HmacAlgorithms, HmacUtils}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

trait DeliverablesCommunicator {
  protected val sharedSecret:String

  protected implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]]
  private val localLogger = LoggerFactory.getLogger(getClass)

  protected def makeAuthHeaders(uri:Uri,
                              contentType: String,
                              bodyContent:String,
                              method:String,
                              httpDate:akka.http.scaladsl.model.DateTime=akka.http.scaladsl.model.DateTime.now) = {
    val checksumString = DigestUtils.sha384Hex(bodyContent)

    val stringToSign =
      s"""${uri.toJavaUri.getPath}
         |${httpDate.toRfc1123DateTimeString()}
         |$contentType
         |$checksumString
         |$method""".stripMargin

    val signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_384, sharedSecret).hmacHex(stringToSign)
    localLogger.debug(s"signature is $signature")

    Map(
      "Authorization"->s"HMAC $signature",
      "Date"->httpDate.toRfc1123DateTimeString(),
      "Content-Type"->contentType,
      "Digest"-> s"SHA-384=$checksumString"
    )
  }

  private def sendPost(uri:Uri, xmlString:String, headers:Map[String,String]):Future[Response[Source[ByteString, Any]]] = {
    val bs = ByteString(xmlString,"UTF-8")

    localLogger.info("Got xml body")
    val source:Source[ByteString, Any] = Source.single(bs)
    val hdr = headers ++ makeAuthHeaders(uri, "application/json", xmlString, "POST")

    localLogger.info(s"Initiating send to $uri with headers $hdr")
    sttp
      .post(uri)
      .streamBody(source)
      .headers(hdr)
      .response(asStream[Source[ByteString, Any]])
      .send()
  }

//  private def sendGet(uri:Uri, headers:Map[String,String]):Future[Response[Source[ByteString, Any]]] = {
//    val hdr = Map(
//      "Accept"->"application/xml",
//      "Authorization"->s"Basic $authString",
//      "Content-Type"->"application/xml"
//    ) ++ headers
//
//    localLogger.info(s"Got headers, initiating send to $uri")
//    sttp
//      .get(uri)
//      .headers(hdr)
//      .response(asStream[Source[ByteString, Any]])
//      .send()
//  }

  private def consumeSource(source:Source[ByteString,Any])(implicit localLogger:DiagnosticLoggingAdapter, materializer: akka.stream.Materializer, ec: ExecutionContext):Future[String] = {
    localLogger.info("Consuming returned body")
    val sink = Sink.reduce((acc:ByteString, unit:ByteString)=>acc.concat(unit))
    val runnable = source.toMat(sink)(Keep.right)
    runnable.run().map(_.utf8String)
  }

  def request(uri:Uri,xmlString:String,headers:Map[String,String])
             (implicit localLogger:DiagnosticLoggingAdapter, materializer: akka.stream.Materializer,ec: ExecutionContext):
  Future[String] = sendPost(uri, xmlString, headers).flatMap({ response=>
    response.body match {
      case Right(source)=>
        localLogger.info("Send succeeded")
        consumeSource(source)
      case Left(errorString)=>
        val errMsg = s"Send failed: ${response.code} - $errorString"
        localLogger.warning(errMsg)
        throw ErrorSend(errMsg, response.code)
    }})
}
