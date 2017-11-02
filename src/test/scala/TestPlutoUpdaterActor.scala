import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActors, TestKit, TestProbe}
import com.gu.contentatom.thrift.{Atom, AtomData, AtomType, ContentChangeDetails}
import com.gu.contentatom.thrift.atom.media._
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.http.scaladsl.model._

import scala.concurrent.duration._

class TestPlutoUpdaterActor extends WordSpecLike with BeforeAndAfterAll with Matchers
{
  private implicit val system:ActorSystem = ActorSystem("TestPlutoUpdaterActor")

  private val config = ConfigFactory.defaultApplication()
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "PlutoUpdaterActor" must {
    "make an update request" in {
      val atomMetadata: Metadata = Metadata(tags = Some(Seq("tom","dick","harry")),
        categoryId = Some("xxxCategoryIdxxxx"),
        license = Some("Ridiculously restrictive"),
        commentsEnabled = Some(false),
        channelId = Some("xxxChannelIdxxx"),
        privacyStatus = Some(PrivacyStatus.Public),
        expiryDate = Some(1508252652),
        pluto = Some(PlutoData(commissionId = Some("VX-123"), projectId = Some("VX-456"), masterId = Some("VX-789")))
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

      MockServer.handleRequest(HttpResponse(StatusCodes.OK),HttpRequest(method = HttpMethods.PUT,uri="http://localhost:8089/API/item/VX-789/metadata"))
      val sender = TestProbe("ProbeNoId")
      implicit val senderRef = sender.ref

      val updateractor = system.actorOf(Props(new PlutoUpdaterActor(config)), "UpdaterWithId")
      updateractor ! DoUpdate(testAtom)

      sender.expectMsg(30 seconds, SuccessfulSend)
    }

    "not make a request if there is no ID in the data" in {
      val atomMetadata: Metadata = Metadata(tags = Some(Seq("tom","dick","harry")),
        categoryId = Some("xxxCategoryIdxxxx"),
        license = Some("Ridiculously restrictive"),
        commentsEnabled = Some(false),
        channelId = Some("xxxChannelIdxxx"),
        privacyStatus = Some(PrivacyStatus.Public),
        expiryDate = Some(1508252652),
        pluto = None
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

      val sender = TestProbe("ProbeNoId")
      val updateractor = system.actorOf(Props(new PlutoUpdaterActor(config)), "UpdaterNoId")

      updateractor tell(DoUpdate(testAtom), sender.ref)
      sender.expectMsg(ErrorSend("No Item ID in atom data"))
    }
  }

}
