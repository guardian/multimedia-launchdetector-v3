import PlutoNextGen.DeliverablesCommunicator
import akka.actor.ActorSystem
import akka.event.{DiagnosticLoggingAdapter, Logging}
import com.softwaremill.sttp.{SttpBackend, Uri, UriContext}
import org.scalatest.{Matchers, MustMatchers, WordSpecLike}
import akka.http.scaladsl.model.DateTime
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend

import scala.concurrent.Future

class TestDeliverablesCommunicator extends WordSpecLike with MustMatchers {
  val actorSystem = ActorSystem.create("test")

  class TestClass extends DeliverablesCommunicator {
    override val sharedSecret = "something_here"
    implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]] = AkkaHttpBackend.usingActorSystem(actorSystem)

    def callMakeAuthHeaders(uri:Uri,
                             contentType: String,
                             bodyContent:String,
                             method:String,
                             httpDate:akka.http.scaladsl.model.DateTime=akka.http.scaladsl.model.DateTime.now):Map[String,String]
    = makeAuthHeaders(uri, contentType, bodyContent, method, httpDate)
  }

  "DeliverablesCommmunicator.makeAuthHeaders" must {
    "calculate the correct digest and checksum for the content" in {
      val toTest = new TestClass()

      val result = toTest.callMakeAuthHeaders(
        uri"http://myserver/api/atom/2505E696-B90A-4172-9B15-4ECC61F0C649",
        "application/json",
        """{"key":"value"}""",
        "POST",
        DateTime.fromIsoDateTimeString("2020-01-02T03:04:05").get
      )

      result.get("Authorization") must be(Some("HMAC 411eab34b4ab4969578739fb817ae43906d0f375f3a0411524ba48943b94834808afc3be38a5f8f581dc4646a743bedc"))
      result.get("Digest") must be(Some("SHA-384=b491c9142540686efa07e25e090562ae3f7c1cac3cb00d730f1aaf251e3e703d19a3ca88de1d7cd5375d14c76346a3c2"))
      result.get("Date") must be(Some("Thu, 02 Jan 2020 03:04:05 GMT"))
      result.get("Content-Type") must be(Some("application/json"))
    }
  }
}
