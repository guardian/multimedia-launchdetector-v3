package actors

import java.time.LocalDateTime
import java.util.Base64

import actors.messages._
import akka.actor.{Actor, ActorRef, Props}
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
import vidispine.{UpdateXmlGenerator, VSCommunicator, VSError, VSSearchResponse}

import scala.xml.Elem

class PlutoUpdaterActor(config:Config) extends Actor with VSCommunicator{
  implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]] = AkkaHttpBackend.usingActorSystem(context.system)
  import context.dispatcher
  implicit val materializer = ActorMaterializer()

  private val plutoHost=config.getString("pluto_host")
  private val plutoPort=config.getInt("pluto_port")
  override protected val plutoUser=config.getString("pluto_user")
  override protected val plutoPass=config.getString("pluto_pass")
  private val proto="http"

  override protected implicit val logger:DiagnosticLoggingAdapter = Logging.getLogger(this)
  protected val lookupActor = context.actorOf(Props(new PlutoLookupActor(config)))

  override def receive: Receive = {
    case DoUpdate(atom)=>
      logger.info(s"Received update request from ${sender()}")
      val origSender = sender() //need to make a copy of this so when it's called from another thread in map() below it is passed correctly.
      doUpdate(atom).onComplete({
        case Success(serverResponse)=>
          logger.info(s"Successfully updated vidispine: $serverResponse")
          origSender ! Right(SuccessfulSend())
        case Failure(error)=>
          logger.error(s"Unable to update vidispine: $error")
          origSender ! Left(ErrorSend(error.getMessage, -1))
      })
    case _=>logger.error(s"Received an unknown message")
  }

  def updateViaLookup(atom:Atom, xmlDoc:Elem):Future[String] = {
    implicit val timeout:akka.util.Timeout = 55.seconds
    val itemIdFuture:Future[ActorMessage] = (lookupActor ? LookupPlutoId(atom)).mapTo[ActorMessage]
    itemIdFuture.flatMap({
      case GotPlutoId(itemId)=>
        request(uri"$proto://$plutoHost:$plutoPort/API/item/$itemId/metadata",xmlDoc.toString(),Map())
      case ErrorSend(message, statusCode)=>
        throw ErrorSend(message, statusCode) //return a Future.failed, which will output the error from the main actor receive
    })
  }

  def doUpdate(atom:Atom):Future[String] = {
    val xmlDoc = UpdateXmlGenerator.makeContentXml(atom,LocalDateTime.now())
    val mediaContent = atom.data.asInstanceOf[AtomData.Media].media

    val maybeItemId = mediaContent.metadata.flatMap(_.pluto.flatMap(_.masterId))
    logger.debug(s"got item id $maybeItemId")
    maybeItemId match {
      case Some(itemId)=> //the record already has an item id, so we don't need to look up
        logger.info(s"Directly sending to $proto://$plutoHost:$plutoPort/API/item/$itemId/metadata")
        request(uri"$proto://$plutoHost:$plutoPort/API/item/$itemId/metadata",xmlDoc.toString,Map())
      case None=> //the record does not have an item id, so we must perform a search in the asset management to find it.
        //the atom ID should be set as an external ID on the item, so we should be able to just make a call to it.
        //if not, this will return a 404
        request(uri"$proto://$plutoHost:$plutoPort/API/item/${atom.id}/metadata",xmlDoc.toString, Map())
          .recoverWith({
            //if we got a 404 from the asset management backend then we should fall back to searching
            case ErrorSend(str, code)=>
              if(code==404){
                logger.debug(str)
                logger.warning(s"Atom ${atom.id} had no item, or no external ID. Trying via search.")
                updateViaLookup(atom, xmlDoc)
              } else {
                logger.error(s"Could not look up item with atom id ${atom.id} by external ID: $str")
                throw ErrorSend(str, code)
              }
            // any other exception cases are handled by the caller
          })
    }
  }

}
