package actors

import akka.actor.{Actor, Props}
import akka.event.{DiagnosticLoggingAdapter, Logging}
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import actors.messages._
import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClientBuilder}
import com.gu.contentatom.thrift.{ChangeRecord, User}
import com.gu.scanamo._
import com.gu.scanamo.syntax._
import models.UnattachedAtom
import vidispine.UpdateXmlGenerator

import scala.concurrent.Future
import scala.util.{Failure, Success}
import java.time.LocalDateTime

class UnattachedAtomActor(config:Config) extends Actor {
  import context.dispatcher
  implicit val materializer:ActorMaterializer = ActorMaterializer()

  private val maxRetries = 10
  private val tableName = config.getString("unattached_atoms_table")
  private val region = config.getString("region")

  protected implicit val logger:DiagnosticLoggingAdapter = Logging.getLogger(this)

  override def receive: Receive = {
    case MasterNotFound(atomId,createdChange,updatedChange, attempt)=>
      val origSender = sender() //we message the sender back in the onComplete handler, i.e. a different thread.
                                //so, we must save a copy of the sender ref here as the subthread doesn't have one.

      logAtomUnattached(atomId, createdChange, updatedChange).onComplete({
        case Success(maybeResult)=>maybeResult match {
          case None=> //we expect no data back from an insert
            logger.info("Saved unattached atom to table")
            origSender ! SuccessfulSend
          case Some(response)=>response match {
            case Left(error)=>
              logger.error(s"Unable to record missing item: $error")
              if(attempt>=maxRetries){
                logger.error(s"Not able to record after $maxRetries attempts, giving up.")
                sender ! ErrorSend(s"Not able to record after $maxRetries attempts, giving up.")
              } else {
                //retry after a short delay
                Thread.sleep(2000)
                self ! MasterNotFound(atomId, createdChange, updatedChange, attempt + 1)
              }
            case Right(successReply)=>
              logger.info(s"Saved item to table: ${successReply.toString}")
              origSender ! SuccessfulSend
          }
        }
        case Failure(error)=>
          logger.error(s"Could not output to dynamo: ${error.toString}")
          origSender ! ErrorSend(error.toString)
      })
    case _=>
      logger.error("Unrecognised message sent to UnattachedAtomActor")
  }

  protected def getClient:AmazonDynamoDB = {
    val chain = new AWSCredentialsProviderChain(
      new EnvironmentVariableCredentialsProvider(),
      new ProfileCredentialsProvider("multimedia"),
      new InstanceProfileCredentialsProvider()
    )

    AmazonDynamoDBClientBuilder.standard()
      .withCredentials(chain)
      .withRegion(region)
      .build()
  }

  def logAtomUnattached(atomId: String, createdChange: Option[ChangeRecord], updatedChange:Option[ChangeRecord]) = Future {
    val table = Table[UnattachedAtom](tableName)

    val createdUserEmail = for {
      change <- createdChange
      user <- change.user
    } yield user.email

    val createdChangeTime = for {
      change <- createdChange
    } yield UpdateXmlGenerator.asIsoTimeString(change.date/1000)

    val lastModChangeTime = for {
      change <- updatedChange
    } yield UpdateXmlGenerator.asIsoTimeString(change.date/1000)

    val record = UnattachedAtom(atomId,
      createdUserEmail.getOrElse("unknown user"),
      createdChangeTime.getOrElse(LocalDateTime.now().toString),
      lastModChangeTime.getOrElse(LocalDateTime.now().toString)
    )

    val op = table.put(record)
    Scanamo.exec(getClient)(op)
  }
}
