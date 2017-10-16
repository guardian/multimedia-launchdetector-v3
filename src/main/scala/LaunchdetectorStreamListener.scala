import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.firehose.client.StreamListener
import com.gu.contentatom.thrift.Atom
import com.gu.crier.model.event.v1.RetrievableContent
import org.apache.logging.log4j.scala.Logging

import scala.util.{Failure, Success}

class LaunchdetectorStreamListener extends StreamListener with Logging {
  /**
    * When content is updated or created on the Guardian an `update` event will be sent to the events stream. This
    * update event contains the entire payload.
    *
    * @param content
    */
  override def contentUpdate(content: Content): Unit = {
    logger.info(s"Got content update")
  }

  /**
    * It is possible for an `update` event to exceed the upper limit of an event for our stream implementation.
    * In the event that this occurs, a `RetrievableUpdate` is sent containing a payload which allows the client
    * to retrieve the update themselves.
    *
    * @param content
    */
  override def contentRetrievableUpdate(content: RetrievableContent): Unit = {
    logger.info(s"Got retrievable update")
  }

  /**
    * When content is removed from the Guardian a `takedown` event will be sent to the events stream. We expect all
    * consumers of the Content API and this events stream to respect these takedowns and remove the content from their
    * own downstream systems.
    *
    * @param contentId
    */
  override def contentTakedown(contentId: String): Unit = {
    logger.info(s"Got takedown")
  }

  def atomUpdate(atom: Atom): Unit = {
    //output to a file for testing
    val homedir = sys.env.getOrElse("HOME","/tmp")
    val filepath = Seq(homedir, atom.id).mkString("/")

    logger.info(s"Got atom update, writing to $filepath")
    DebugFileWriter.writeToFile(filepath, atom) match {
      case Failure(except)=>
        val newFilePath = s"/tmp/${atom.id}"
        logger.error(s"Could not write to $filepath, trying $newFilePath. (${except.getMessage}")
        DebugFileWriter.writeToFile(newFilePath, atom) match {
          case Failure(newExcept)=>
            logger.error(s"Still could not write to $newFilePath: ${newExcept.getMessage}")
          case Success(result)=>Success(result)
        }
      case Success(result)=>
        Success(result)
    }
  }
}
