package actors

import java.time.LocalDateTime
import java.util.Base64

import PlutoNextGen.{DeliverablesCommunicator, UpdateJsonGenerator}
import actors.messages._
import akka.actor.{Actor, ActorRef, Props}
import akka.event.{DiagnosticLoggingAdapter, Logging}
import akka.pattern.ask
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import com.gu.contentatom.thrift.{Atom, AtomData}
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.softwaremill.sttp._
import com.typesafe.config.Config

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

class PlutoUpdaterActor(config:Config) extends Actor with DeliverablesCommunicator{
  implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]] = AkkaHttpBackend.usingActorSystem(context.system)
  import context.dispatcher

  implicit val materializer = Materializer(context.system)

  override protected val sharedSecret = config.getString("pluto_shared_secret")
  private val plutoHost=config.getString("pluto_host")
  private val plutoPort=config.getInt("pluto_port")
  private val proto=config.getString("pluto_proto")

  protected implicit val logger:DiagnosticLoggingAdapter = Logging.getLogger(this)

  override def receive: Receive = {
    case DoUpdate(atom)=>
      logger.info(s"Received update request from ${sender()}")
      val origSender = sender() //need to make a copy of this so when it's called from another thread in map() below it is passed correctly.
      doUpdate(atom).onComplete({
        case Success(serverResponse)=>
          logger.info(s"Successfully updated vidispine: $serverResponse")
          origSender ! SuccessfulSend()
        case Failure(error)=>
          logger.error(s"Unable to update vidispine: $error")
          origSender ! ErrorSend(error.getMessage, -1)
      })
    case _=>logger.error(s"Received an unknown message")
  }

  def doUpdate(atom:Atom):Future[String] = {
    val contentString = UpdateJsonGenerator.makeContentJson(atom, LocalDateTime.now()).toString()
    request(uri"$proto://$plutoHost:$plutoPort/deliverables/api/atom/${atom.id}",
      contentString,
      Map(),
    )
  }

}
