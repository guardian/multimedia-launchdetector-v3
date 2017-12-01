package actors
import actors.messages.{ErrorSend, LookupAtomId, SuccessfulSend}
import akka.actor.{Actor, ActorRef, Props}
import akka.event.{DiagnosticLoggingAdapter, Logging}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend

import scala.concurrent.Future

class ForceUpdateActor extends Actor {
  implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]] = AkkaHttpBackend.usingActorSystem(context.system)
  import context.dispatcher
  implicit val materializer = ActorMaterializer()

  private val proto="http"

  protected implicit val logger:DiagnosticLoggingAdapter = Logging.getLogger(this)

  override def receive: Receive = {
    case LookupAtomId(atomId)=>
      logger.info(s"Requested atom ID $atomId")
      //sender ! Right(SuccessfulSend())
      sender ! Left(ErrorSend("something went KABOOM!"))
  }
}
