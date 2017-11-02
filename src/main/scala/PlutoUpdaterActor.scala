import java.util.Base64

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import akka.actor.Actor
import akka.actor.Props
import akka.event.{DiagnosticLoggingAdapter, Logging}
import com.gu.contentatom.thrift.{Atom, AtomData}
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp._
import com.typesafe.config.Config
import java.time.{Instant, LocalDateTime, ZoneOffset, ZonedDateTime}

import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class DoUpdate(atom:Atom)
case class SuccessfulSend()
case class NoResponseBody()
case class ErrorSend(error:String) extends Throwable {
  override def getMessage: String = error
}

class PlutoUpdaterActor(config:Config) extends Actor{
  implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]] = AkkaHttpBackend.usingActorSystem(context.system)
  import context.dispatcher
  implicit val materializer = ActorMaterializer()

  private val plutoHost=config.getString("pluto_host")
  private val plutoPort=config.getInt("pluto_port")
  private val plutoUser=config.getString("pluto_user")
  private val plutoPass=config.getString("pluto_pass")
  private val proto="http"

  private implicit val logger:DiagnosticLoggingAdapter = Logging.getLogger(this)

  override def receive: Receive = {
    case DoUpdate(atom)=>
      logger.info(s"Received update request from ${sender()}")
      val origSender = sender() //need to make a copy of this so when it's called from another thread in map() below it is passed correctly.
      doUpdate(atom).map({
      case Success(futureString)=>
        futureString.onComplete({
          case Success(serverResponse)=>
            logger.info(s"Successfully updated vidispine: $serverResponse")
            origSender ! SuccessfulSend
          case Failure(error)=>
            logger.warning(s"Updated vidispine but not able to read any response back")
            origSender ! NoResponseBody
        })
      case Failure(error)=>
        logger.error(error,s"Unable to update Vidispine: ${error.getMessage}")
        origSender ! error
    })
    case _=>logger.error(s"Received an unknown message")
  }


  private def authString:String = Base64.getEncoder.encodeToString(s"$plutoUser:$plutoPass".getBytes)

  private def send(itemId:String, xmlString:String):Future[Response[Source[ByteString, Any]]] = {
    val bs = ByteString(xmlString,"UTF-8")

    logger.info("Got xml body")
    val source:Source[ByteString, Any] = Source.single(bs)
    val hdr = Map(
      "Accept"->"application/xml",
      "Authorization"->s"Basic $authString",
      "Content-Type"->"application/xml"
    )

    logger.info(hdr.toString())
    val uri=uri"$proto://$plutoHost:$plutoPort/API/item/$itemId/metadata"
    logger.info(s"Got headers, initiating send to $uri")
    sttp
      .put(uri)
      .streamBody(source)
      .headers(hdr)
      .response(asStream[Source[ByteString, Any]])
      .send()
  }

  private def consumeSource(source:Source[ByteString,Any]):Future[String] = {
    logger.info("Consuming returned body")
    val sink = Sink.reduce((acc:ByteString, unit:ByteString)=>acc.concat(unit))
    val runnable = source.toMat(sink)(Keep.right)
    runnable.run().map(_.utf8String)
  }

  def request(itemId:String, xmlString:String):Future[Try[Future[String]]] = send(itemId, xmlString).map({response=>
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

  def getMasterId(atom:Atom):Option[String] = atom.data.asInstanceOf[AtomData.Media].media.metadata.flatMap(_.pluto.flatMap(_.masterId))

  def doUpdate(atom:Atom):Future[Try[Future[String]]] = {
    val xmlDoc = UpdateXmlGenerator.makeContentXml(atom,LocalDateTime.now())
    val mediaContent = atom.data.asInstanceOf[AtomData.Media].media

    val maybeItemId = mediaContent.metadata.flatMap(_.pluto.flatMap(_.masterId))
    maybeItemId match {
      case Some(itemId)=>request(itemId,xmlDoc.toString())
      case None=>Future(Failure(ErrorSend("No Item ID in atom data")))
    }
  }
}
