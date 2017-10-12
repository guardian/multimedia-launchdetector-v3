import org.apache.logging.log4j.scala.Logging
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.gu.contentapi.firehose.ContentApiFirehoseConsumer
import com.gu.contentapi.firehose.kinesis.KinesisStreamReaderConfig
import com.typesafe.config._

object MainClass extends Logging {
  def main(args:Array[String]):Unit = {
    logger.info("Starting up")

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val route =
      path("is-online") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 9000)

    logger.info(s"Healthcheck online at http://localhost:9000/")

    val config = ConfigFactory.defaultApplication()

    logger.info(s"Connecting to ${config.getString("capi_stream_name")} with role ${config.getString("capi_role_name")}")

    val kinesisCredsProvider = new AWSCredentialsProviderChain(
      new ProfileCredentialsProvider("capi"),
      new STSAssumeRoleSessionCredentialsProvider(config.getString("capi_role_name"), "capi")
    )

    val dynamoCredsProvider = new AWSCredentialsProviderChain(
      new ProfileCredentialsProvider("multimedia"),
      new InstanceProfileCredentialsProvider()
    )

    val kinesisStreamReaderConfig = KinesisStreamReaderConfig(
      streamName = config.getString("capi_stream_name"),
      app = config.getString("app_name"), // must match the table name in CrierDynamoDBPolicy
      stage = "PROD",
      mode = "live",
      suffix = None,
      kinesisCredentialsProvider = kinesisCredsProvider,
      dynamoCredentialsProvider = dynamoCredsProvider,
      awsRegion = "eu-west-1"
    )

    val listener = new LaunchdetectorStreamListener
    val contentApiFirehoseConsumer: ContentApiFirehoseConsumer = new ContentApiFirehoseConsumer(
      kinesisStreamReaderConfig, listener
    )

    contentApiFirehoseConsumer.start()

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

}
