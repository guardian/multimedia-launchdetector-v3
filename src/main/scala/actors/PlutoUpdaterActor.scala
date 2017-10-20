package actors

import java.time.LocalDateTime
import java.util.Base64
import actors.messages._
import akka.actor.Actor
import akka.event.{DiagnosticLoggingAdapter, Logging}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import com.gu.contentatom.thrift.{Atom, AtomData}
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.softwaremill.sttp._
import com.typesafe.config.Config
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import vidispine.{VSError,UpdateXmlGenerator, VSSearchResponse}

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
    case LookupPlutoId(atomId)=>
      logger.info(s"Received lookup request from ${sender()}")
      val origSender = sender() //need to make a copy of this so when it's called from another thread in map() below it is passed correctly.
      doLookup(atomId).onComplete({
        case Success(plutoId)=>
          logger.info(s"Got plutoId $plutoId")
          origSender ! GotPlutoId(plutoId)
        case Failure(error)=>
          logger.error(s"Unable to lookup $atomId in Vidispine: $error")
          logger.error(error.getStackTraceString)
          origSender ! ErrorSend(error.getMessage)
      })
    case _=>logger.error(s"Received an unknown message")
  }


  private def authString:String = Base64.getEncoder.encodeToString(s"$plutoUser:$plutoPass".getBytes)

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

  private def consumeSource(source:Source[ByteString,Any]):Future[String] = {
    logger.info("Consuming returned body")
    val sink = Sink.reduce((acc:ByteString, unit:ByteString)=>acc.concat(unit))
    val runnable = source.toMat(sink)(Keep.right)
    runnable.run().map(_.utf8String)
  }

  def request(uri:Uri,xmlString:String,headers:Map[String,String]):Future[Try[Future[String]]] = sendPut(uri, xmlString, headers).map({ response=>
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
      case Some(itemId)=>request(uri"$proto://$plutoHost:$plutoPort/API/item/$itemId/metadata",xmlDoc.toString(),Map())
      case None=>
        implicit val timeout:akka.util.Timeout = 60 seconds
        val itemIdFuture:Future[String] = (self ? LookupPlutoId(atom.id)).mapTo[String]
        itemIdFuture.flatMap({ itemId=>
          request(uri"$proto://$plutoHost:$plutoPort/API/item/$itemId/metadata",xmlDoc.toString(),Map())
        })
    }
  }

  def doLookup(atomId:String):Future[String] = {
    val xmlDoc = UpdateXmlGenerator.makeSearchXml(atomId)
    val resultFuture = request(uri"$proto://$plutoHost:$plutoPort/API/item", xmlDoc.toString(),Map())

    resultFuture.flatMap({
      case Success(futureString)=>
        futureString.map({ returnedXml =>
          logger.info("Got returned search xml")
          val searchResult = VSSearchResponse(returnedXml)
          logger.info(searchResult.toString)
          if(searchResult.hits>1) logger.warning(s"Multiple results returned for atom ID $atomId: ${searchResult.itemIds}")

          if(searchResult.hits==0){
            val errorString=s"No items found for atom ID $atomId"
            logger.warning(errorString)
            throw new RuntimeException(errorString)
          } else {
            searchResult.itemIds.head
          }
        })
      case Failure(error)=>Future.failed(error)
    })
  }
}
