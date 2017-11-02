package actors

import actors.messages.{ErrorSend, GotPlutoId, LookupPlutoId}
import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.config.Config
import vidispine.{UpdateXmlGenerator, VSCommunicator, VSSearchResponse}
import actors.messages._
import akka.event.{DiagnosticLoggingAdapter, Logging}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.gu.contentatom.thrift.Atom
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.softwaremill.sttp._

import scala.concurrent.Future
import scala.util.{Failure, Success}

class PlutoLookupActor(config:Config) extends Actor with VSCommunicator{
  implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]] = AkkaHttpBackend.usingActorSystem(context.system)
  import context.dispatcher
  implicit val materializer = ActorMaterializer()

  private val plutoHost=config.getString("pluto_host")
  private val plutoPort=config.getInt("pluto_port")
  override protected val plutoUser=config.getString("pluto_user")
  override protected val plutoPass=config.getString("pluto_pass")
  private val proto="http"

  protected val logUnattachedActor:ActorRef = context.actorOf(Props(new UnattachedAtomActor(config)))

  override protected implicit val logger:DiagnosticLoggingAdapter = Logging.getLogger(this)
  override def receive: Receive = {
    case LookupPlutoId(atom)=>
      logger.info(s"Received lookup request from ${sender()}")
      val origSender = sender() //need to make a copy of this so when it's called from another thread in map() below it is passed correctly.
      doLookup(atom).onComplete({
        case Success(plutoId)=>
          logger.info(s"Got plutoId $plutoId")
          origSender ! GotPlutoId(plutoId)
        case Failure(error)=>
          logger.error(s"Unable to lookup ${atom.id} in Vidispine: $error")
          logger.error(error.getStackTraceString)
          origSender ! ErrorSend(error.getMessage)
      })
    case _=>logger.error(s"Received an unknown message")
  }

  def doLookup(atom:Atom):Future[String] = {
    val xmlDoc = UpdateXmlGenerator.makeSearchXml(atom.id)
    val resultFuture = request(uri"$proto://$plutoHost:$plutoPort/API/item", xmlDoc.toString(),Map())

    resultFuture.map({ returnedXml=>
      logger.info("Got returned XML")
      val searchResult = VSSearchResponse(returnedXml)
      logger.info(searchResult.toString)
      if(searchResult.hits>1) logger.warning(s"Multiple results returned for atom ID ${atom.id}: ${searchResult.itemIds}")

      if(searchResult.hits==0){
        val errorString=s"No items found for atom ID ${atom.id}"
        logUnattachedActor ! MasterNotFound(atom.id,atom.contentChangeDetails.created,atom.contentChangeDetails.lastModified, attempt=1)
        logger.warning(errorString)
        throw new RuntimeException(errorString)
      } else {
        searchResult.itemIds.head
      }
    })

  }
}
