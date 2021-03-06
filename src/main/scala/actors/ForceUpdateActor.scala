package actors
import actors.messages._
import akka.actor.{Actor, ActorRef, Props}
import akka.event.{DiagnosticLoggingAdapter, Logging}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.pattern.ask
import com.gu.contentapi.client.GuardianContentClient
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import capi.CapiCommunicator
import com.gu.contentatom.thrift.{Atom, AtomData}
import com.gu.contentatom.thrift.atom.media.MediaAtom
import com.typesafe.config.Config

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ForceUpdateActor(config:Config) extends Actor with CapiCommunicator {
  implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]] = AkkaHttpBackend.usingActorSystem(context.system)
  import context.dispatcher
  implicit val materializer = ActorMaterializer()

  protected implicit val logger:DiagnosticLoggingAdapter = Logging.getLogger(this)

  protected val updater:ActorRef = context.actorOf(Props(new PlutoUpdaterActor(config)))
  protected val unattachedAtomActor:ActorRef = context.actorOf(Props(new UnattachedAtomActor(config)))

  protected lazy val client: GuardianContentClient = new GuardianContentClient(config.getString("capi_api_key"))

  override def receive: Receive = {
    case LookupAtomId(atomId)=>
      logger.info(s"Requested atom ID $atomId")

      val originalSender = sender

      implicit val timeout:akka.util.Timeout = 25.seconds

      lookupAtom(atomId,"media").onComplete({
        case Success(maybeAtom)=>
          maybeAtom match {
            case Some(atom) =>
              logger.info(s"orignal sender is $originalSender")
              logger.info(s"Got atom with title ${atom.title}")

              (updater ? DoUpdate(atom)).onComplete({
                case Success(result:SuccessfulSend)=>
                  unattachedAtomActor ! MasterNowAttached(atom.id)
                  originalSender ! Right(result)
                case Success(result:ErrorSend)=>
                    originalSender ! Left(result)
                case Failure(error)=>originalSender ! Left(error)
              })
            case None =>
              logger.error(s"Atom $atomId is valid but not a media atom")
              originalSender ! Left(ErrorSend(s"Atom $atomId is valid but not a media atom", -1))
          }
        case Failure(error)=>
          logger.error(s"Atom $atomId could not be loaded: $error")
          originalSender ! Left(ErrorSend(s"Atom $atomId could not be loaded: $error", -1))
      })
    case SuccessfulSend()=>
      //just ignore this, it comes through as a side-effect to the DoUpdate completing but it's properly handled in the ask block above
    case msg:Any=>
      logger.warning(s"ForceUpdateActor received an unexpected message: ${msg.getClass.getTypeName} ${msg.getClass.getName} ${msg.getClass.getCanonicalName}")
  }
}
