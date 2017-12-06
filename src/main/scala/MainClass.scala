import org.slf4j.Logger
import org.slf4j.LoggerFactory
import actors.{ForceUpdateActor, PlutoUpdaterActor}
import akka.actor.{ActorRef, ActorSystem, Props}

import akka.stream.ActorMaterializer
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider, STSAssumeRoleSessionCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.gu.contentapi.firehose.ContentApiFirehoseConsumer
import com.gu.contentapi.firehose.kinesis.KinesisStreamReaderConfig
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.typesafe.config._
import sun.misc.Signal

import scala.util.Try

object MainClass {

  private final val logger:Logger = LoggerFactory.getLogger(MainClass.getClass)

  def main(args:Array[String]):Unit = {
    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    logger.info("Starting up")

    val config = ConfigFactory.defaultApplication()

    val forceUpdateActor:ActorRef = system.actorOf(Props(new ForceUpdateActor(config)))

    val httpServer = new HttpServer(forceUpdateActor)
    val bindingFuture = httpServer.setup(config)

    logger.info(s"Connecting to ${config.getString("capi_stream_name")} with role ${config.getString("capi_role_name")}")

    val stsProvider = new STSAssumeRoleSessionCredentialsProvider
          .Builder(config.getString("capi_role_name"), "multimedia-launchdetector-capi")
        .build()

    val kinesisCredsProvider = new AWSCredentialsProviderChain(
      new ProfileCredentialsProvider("capi"),
      stsProvider,
    )

    val dynamoCredsProvider = new AWSCredentialsProviderChain(
      new ProfileCredentialsProvider("multimedia"),
      new ProfileCredentialsProvider(), //also try default profile if explicit one does not work
      InstanceProfileCredentialsProvider.getInstance()
    )

    val kinesisStreamReaderConfig = KinesisStreamReaderConfig(
      streamName = config.getString("capi_stream_name"),
      app = config.getString("app_name"), // must match the table name in CrierDynamoDBPolicy
      stage = config.getString("app_stage"),
      mode = config.getString("capi_mode"),
      suffix = None,
      kinesisCredentialsProvider = kinesisCredsProvider,
      dynamoCredentialsProvider = dynamoCredsProvider,
      awsRegion = config.getString("region")
    )

    val updater = system.actorOf(Props(new PlutoUpdaterActor(config)))

    val listener = new LaunchdetectorStreamListener(updater)

    val contentApiFirehoseConsumer: ContentApiFirehoseConsumer = new ContentApiFirehoseConsumer(
      kinesisStreamReaderConfig, listener
    )

    var shutdownHandler = new ShutdownHandler(contentApiFirehoseConsumer)
    Signal.handle(new Signal("INT"), shutdownHandler)
    Signal.handle(new Signal("TERM"), shutdownHandler)

    val thread = contentApiFirehoseConsumer.start()
    thread.join() //block while contentApiFirehoseConsumer is running


    httpServer.remove(bindingFuture)(_ => system.terminate())

  }

}
