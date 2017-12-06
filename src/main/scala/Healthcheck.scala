import akka.actor.ActorSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Healthcheck  {
  private final val logger:Logger = LoggerFactory.getLogger(Healthcheck.getClass)

  def setup(config:Config)(implicit system:ActorSystem, mat:ActorMaterializer):Future[Http.ServerBinding] = {
    val route =
      path("is-online") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<pre>online</pre>"))
        }
      }

    val bindIpAddress = Try {
      config.getString("bind_ip_address")
    }.getOrElse("0.0.0.0")

    val bindingFuture = Http().bindAndHandle(route, bindIpAddress, 9000)

    logger.info(s"Healthcheck online at http://$bindIpAddress:9000/")

    bindingFuture
  }

  def remove(bindingFuture:Future[Http.ServerBinding])(onCompleteHander:(Nothing=>Any))(implicit ec:ExecutionContext) = {
    logger.info("Removing healthcheck handler")
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_=>onCompleteHander) // and shutdown when done
  }
}
