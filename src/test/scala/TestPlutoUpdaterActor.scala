import java.time.{LocalDateTime, LocalTime}

import PlutoNextGen.UpdateJsonGenerator
import actors.{messages, _}
import actors.messages._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.DiagnosticLoggingAdapter
import akka.testkit.{ImplicitSender, TestActors, TestKit, TestProbe}
import com.gu.contentatom.thrift.{Atom, AtomData, AtomType, ContentChangeDetails}
import com.gu.contentatom.thrift.atom.media._
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream.Materializer
import com.softwaremill.sttp
import com.softwaremill.sttp.UriContext
import org.mockito.Matchers._
import org.mockito.Mockito.{doReturn, times, verify}
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.{OneInstancePerTest, Sequential}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class TestPlutoUpdaterActor extends TestKit(ActorSystem("TestPlutoUpdaterActor"))
  with WordSpecLike
  with BeforeAndAfterAll
  with Matchers {

  implicit val timeout:akka.util.Timeout = 10.seconds

  private val config = ConfigFactory.defaultApplication()
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "actors.PlutoUpdaterActor" must {
    "make an update request to the atom ID" in {
      val atomMetadata: Metadata = Metadata(tags = Some(Seq("tom", "dick", "harry")),
        categoryId = Some("xxxCategoryIdxxxx"),
        license = Some("Ridiculously restrictive"),
        commentsEnabled = Some(false),
        channelId = Some("xxxChannelIdxxx"),
        privacyStatus = Some(PrivacyStatus.Public),
        expiryDate = Some(1508252652),
        pluto = Some(PlutoData(commissionId = Some("VX-123"), projectId = Some("VX-456"), masterId = None))
      )

      val ytAsset: Asset = Asset(assetType = AssetType.Video,
        version = 1,
        id = "abcdefg12345",
        platform = Platform.Youtube
      )

      val data: MediaAtom = MediaAtom(title = "media title", category = Category.News, description = Some("this is a test"),
        source = Some("my fridge"), duration = Some(543), activeVersion = Some(1), keywords = Some(Seq("this", "that", "other")),
        metadata = Some(atomMetadata), assets = Seq(ytAsset))

      val changeDetails = ContentChangeDetails(revision = 1)

      val currentTime = LocalDateTime.now()
      val ad: AtomData = new AtomData.Media(data)
      val testAtom = Atom("fakeAtomId", AtomType.Media, Seq("no-label"), "<p>default html</p>", ad,
        changeDetails, title = Some("atom title"))

      val requestMock = mock[Function3[sttp.Uri, String, Map[String,String], Future[String]]]

      doReturn(Future("ok")).when(requestMock).apply(any[sttp.Uri],any[String],any[Map[String,String]])

      val updateractor = system.actorOf(Props(new PlutoUpdaterActor(config) {
        override def request(uri: sttp.Uri, xmlString: String, headers: Map[String, String])
                            (implicit localLogger: DiagnosticLoggingAdapter, materializer: Materializer, ec: ExecutionContext): Future[String] =
          requestMock(uri, xmlString, headers)
      }), "UpdaterWithId")

      val reply = Await.result((updateractor ? DoUpdate(testAtom)).mapTo[ActorMessage], 10 seconds)

      val expectedContent = UpdateJsonGenerator.makeContentJson(testAtom, LocalDateTime.now()).toString()
      reply should be(messages.SuccessfulSend())
      verify(requestMock, times(1)).apply(uri"http://localhost:8089/deliverables/api/atom/fakeAtomId", expectedContent, Map())
    }

    "report failure if the send fails" in {
      val atomMetadata: Metadata = Metadata(tags = Some(Seq("tom", "dick", "harry")),
        categoryId = Some("xxxCategoryIdxxxx"),
        license = Some("Ridiculously restrictive"),
        commentsEnabled = Some(false),
        channelId = Some("xxxChannelIdxxx"),
        privacyStatus = Some(PrivacyStatus.Public),
        expiryDate = Some(1508252652),
        pluto = Some(PlutoData(commissionId = Some("VX-123"), projectId = Some("VX-456"), masterId = None))
      )

      val ytAsset: Asset = Asset(assetType = AssetType.Video,
        version = 1,
        id = "abcdefg12345",
        platform = Platform.Youtube
      )

      val data: MediaAtom = MediaAtom(title = "media title", category = Category.News, description = Some("this is a test"),
        source = Some("my fridge"), duration = Some(543), activeVersion = Some(1), keywords = Some(Seq("this", "that", "other")),
        metadata = Some(atomMetadata), assets = Seq(ytAsset))

      val changeDetails = ContentChangeDetails(revision = 1)

      val currentTime = LocalDateTime.now()
      val ad: AtomData = new AtomData.Media(data)
      val testAtom = Atom("fakeAtomId", AtomType.Media, Seq("no-label"), "<p>default html</p>", ad,
        changeDetails, title = Some("atom title"))

      val requestMock = mock[Function3[sttp.Uri, String, Map[String,String], Future[String]]]

      doReturn(Future.failed(new RuntimeException("kaboom"))).when(requestMock).apply(any[sttp.Uri],any[String],any[Map[String,String]])

      val updateractor = system.actorOf(Props(new PlutoUpdaterActor(config) {
        override def request(uri: sttp.Uri, xmlString: String, headers: Map[String, String])
                            (implicit localLogger: DiagnosticLoggingAdapter, materializer: Materializer, ec: ExecutionContext): Future[String] =
          requestMock(uri, xmlString, headers)
      }))

      val reply = Await.result((updateractor ? DoUpdate(testAtom)).mapTo[ActorMessage], 10 seconds)

      val expectedContent = UpdateJsonGenerator.makeContentJson(testAtom, LocalDateTime.now()).toString()
      reply should be(messages.ErrorSend("kaboom", -1))
      verify(requestMock, times(1)).apply(uri"http://localhost:8089/deliverables/api/atom/fakeAtomId", expectedContent, Map())
    }
  }
}
