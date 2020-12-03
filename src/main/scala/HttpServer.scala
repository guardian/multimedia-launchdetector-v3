import akka.actor.ActorSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import akka.pattern.ask

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import actors.messages.{ActorMessage, ErrorSend, LookupAtomId, SuccessfulSend}
import akka.event.{DiagnosticLoggingAdapter, Logging}

import scala.concurrent.duration._

class HttpServer(forceActorUpdater: ActorRef) {
  private final val logger:Logger = LoggerFactory.getLogger(this.getClass)

  //see https://groups.google.com/forum/#!topic/akka-dev/ei-0OzzgKd0
  def setup(config:Config)(implicit system:ActorSystem, mat:ActorMaterializer):Future[Http.ServerBinding] = {
    implicit val timeout:akka.util.Timeout = 60.seconds

    val route =
      get {
        path("is-online") {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<pre>online</pre>"))
        }
      } ~
      put {
        pathPrefix("update" / Segment) {
          docId=>
            onSuccess((forceActorUpdater ? LookupAtomId(docId)).mapTo[Either[ActorMessage,SuccessfulSend]]) {
              case Left(error: ActorMessage)=>
                complete(HttpEntity(ContentTypes.`application/json`, s"""{"status":"error","atomid":"$docId","detail":"Unable to process: ${error.getMessage}"}"""))
              case Right(_: SuccessfulSend)=>
                complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"""{"status":"ok","atomid":"$docId"}"""))
            }
        }
      }

    val bindIpAddress = Try {
      config.getString("bind_ip_address")
    }.getOrElse("0.0.0.0")

    val bindingFuture = Http().bindAndHandle(route, bindIpAddress, 9000)

    logger.info(s"Server online at http://$bindIpAddress:9000/")

    bindingFuture
  }

  def remove(bindingFuture:Future[Http.ServerBinding])(onCompleteHander:(Nothing=>Any))(implicit ec:ExecutionContext) = {
    logger.info("Removing healthcheck handler")
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_=>onCompleteHander) // and shutdown when done
  }
}
