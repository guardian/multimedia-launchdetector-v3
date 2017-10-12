import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.firehose.client.StreamListener
import com.gu.crier.model.event.v1.RetrievableContent

class LaunchdetectorStreamListener extends StreamListener {
  /**
    * When content is updated or created on the Guardian an `update` event will be sent to the events stream. This
    * update event contains the entire payload.
    *
    * @param content
    */
  override def contentUpdate(content: Content): Unit = {

  }

  /**
    * It is possible for an `update` event to exceed the upper limit of an event for our stream implementation.
    * In the event that this occurs, a `RetrievableUpdate` is sent containing a payload which allows the client
    * to retrieve the update themselves.
    *
    * @param content
    */
  override def contentRetrievableUpdate(content: RetrievableContent): Unit = {

  }

  /**
    * When content is removed from the Guardian a `takedown` event will be sent to the events stream. We expect all
    * consumers of the Content API and this events stream to respect these takedowns and remove the content from their
    * own downstream systems.
    *
    * @param contentId
    */
  override def contentTakedown(contentId: String): Unit = {

  }
}
