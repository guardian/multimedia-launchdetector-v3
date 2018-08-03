import java.time.LocalDateTime

import actors._
import actors.messages._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActors, TestKit, TestProbe}
import com.gu.contentatom.thrift.{Atom, AtomData, AtomType, ContentChangeDetails}
import com.gu.contentatom.thrift.atom.media._
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.http.scaladsl.model._
import org.scalatest.{OneInstancePerTest, Sequential}

import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

class TestPlutoUpdaterActor extends WordSpecLike with BeforeAndAfterAll with Matchers with OneInstancePerTest
{
  private implicit val system:ActorSystem = ActorSystem("TestPlutoUpdaterActor")

  private val config = ConfigFactory.defaultApplication()
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "actors.PlutoUpdaterActor" must {
    "make an update request to the atom ID" in {
      val atomMetadata: Metadata = Metadata(tags = Some(Seq("tom","dick","harry")),
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

      val mockServer = MockServer.handleRequest(HttpResponse(StatusCodes.OK),HttpRequest(method = HttpMethods.PUT,uri="http://localhost:8089/API/item/fakeAtomId/metadata"),8089)
      //val mockServer = MockServer.handleRequest(HttpResponse(StatusCodes.OK),HttpRequest(method=HttpMethods.GET,uri="http://localhost:8089/API/item/fakeAtomId/metadata"),8089)
      val sender = TestProbe("ProbeNoId")
      implicit val senderRef = sender.ref

      val updateractor = system.actorOf(Props(new PlutoUpdaterActor(config)), "UpdaterWithId")
      updateractor ! DoUpdate(testAtom)

      sender.expectMsg(30 seconds, Right(SuccessfulSend()))
      mockServer.flatMap(_.unbind())
    }

    "ask for the item ID from the atom ID if there is no ID in the data and no external ID is present" in {
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
      val mockLookup = TestProbe("MockLookup")
      val updateractor = system.actorOf(Props(new PlutoUpdaterActor(config){
        override protected val lookupActor:ActorRef=mockLookup.ref
      }), "UpdaterNoId")


      MockServer.handleRequest(HttpResponse(StatusCodes.NotFound),HttpRequest(method=HttpMethods.GET,uri="http://localhost:8089/API/item/fakeAtomId/metadata"),8089)

      updateractor tell(DoUpdate(testAtom), sender.ref)
      mockLookup.expectMsg(LookupPlutoId(testAtom))
    }
  }

  "look up the internal id of an atom id" in {
    val tempConfig = config.withValue("pluto_port",ConfigValueFactory.fromAnyRef(8097))
    val xmlText = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    |<ItemListDocument xmlns="http://xml.vidispine.com/schema/vidispine">
                    |    <hits>1</hits>
                    |    <item id="KP-2788261" start="-INF" end="+INF">
                    |        <timespan start="-INF" end="+INF"/>
                    |    </item>
                    |</ItemListDocument>"""

    MockServer.handleRequest(
      HttpResponse(StatusCodes.OK, entity = HttpEntity(xmlText.stripMargin)),
      HttpRequest(method = HttpMethods.PUT,
      uri="http://localhost:8097/API/item"),
      8097
    )

    val testAtom = Atom("xxxx", AtomType.Media, Seq("no-label"), "<p>default html", null, ContentChangeDetails(revision=1))

    val sender = TestProbe("ProbeInternalId")
    val updateractor = system.actorOf(Props(new PlutoLookupActor(tempConfig)), "UpdaterLookup")
    updateractor tell(LookupPlutoId(testAtom), sender.ref)

    sender.expectMsg(30 seconds, GotPlutoId("KP-2788261"))
  }

  "report a non-existing item" in {
    val tempConfig = config.withValue("pluto_port",ConfigValueFactory.fromAnyRef(8098))
    val xmlText = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    |<ItemListDocument xmlns="http://xml.vidispine.com/schema/vidispine">
                    |    <hits>0</hits>
                    |</ItemListDocument>"""

    MockServer.handleRequest(
      HttpResponse(StatusCodes.OK, entity = HttpEntity(xmlText.stripMargin)),
      HttpRequest(method = HttpMethods.PUT,
        uri="http://localhost:8098/API/item"),
      8098
    )

    val testAtom = Atom("xxxx", AtomType.Media, Seq("no-label"), "<p>default html", null, ContentChangeDetails(revision=1))

    val sender = TestProbe("ProbeInternalIdNoResult")
    val logUnattachedProbe = TestProbe("ProbeLogUnattached")

    val updateractor = system.actorOf(Props(new PlutoLookupActor(tempConfig){
      override protected val logUnattachedActor=logUnattachedProbe.ref
    }), "UpdaterLookupNoResult")

    updateractor tell(LookupPlutoId(testAtom), sender.ref)

    logUnattachedProbe.expectMsg(30 seconds, MasterNotFound("xxxx", None, None, 1))
  }
}
