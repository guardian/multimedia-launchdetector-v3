import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{Uri, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.event.Logging
import scala.concurrent.Future

/**
  * Mock Akka HTTP Server, will handle requests which is provided with handle request
  */
object MockServer {

  /**
    * Instructs to launch new instance of the Mock server and serve mocked requests
    * @param system ActorSystem, used to initialize mocks
    * @param response mocked response
    * @param httpRequest request to which mock server must react
    * @return Future containing bind event
    */
  def handleRequest(response: HttpResponse, httpRequest: HttpRequest)(implicit system: ActorSystem): Future[Http.ServerBinding] = {
    implicit val materializer = ActorMaterializer()

    val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
      Http().bind(interface = "localhost", port = 8089)

    val requestPath = httpRequest.uri.path.toString()

    val requestHandler: HttpRequest => HttpResponse = {
      case HttpRequest(httpRequest.method, Uri.Path(`requestPath`), _, _, _) =>
        response
      case HttpRequest(gotMethod, gotUri, _,_,_) =>
        HttpResponse(404, entity = s"Unknown resource $gotMethod $gotUri, expected ${httpRequest.method} ${Uri.Path(`requestPath`)}")
    }

    val actorSystemRefCopy = system

    serverSource.to(Sink.foreach { connection =>
      val logger = Logging.getLogger(actorSystemRefCopy, "MockServer")
      logger.info("Mock Server accepted new connection from " + connection.remoteAddress)
      connection handleWithSyncHandler requestHandler
    }).run()
  }
}