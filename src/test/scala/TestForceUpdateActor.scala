import actors._
import actors.messages._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{TestKit, TestProbe}
import com.gu.contentatom.thrift.{Atom, AtomData, AtomType, ContentChangeDetails}
import com.gu.contentatom.thrift.atom.media._
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.{BeforeAndAfterAll, Matchers, OneInstancePerTest, WordSpecLike}
import com.gu.contentapi.client.GuardianContentClient

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class TestForceUpdateActor extends WordSpecLike with BeforeAndAfterAll with Matchers with OneInstancePerTest {
  private implicit val system:ActorSystem = ActorSystem("TestPlutoUpdaterActor")

  private val config = ConfigFactory.defaultApplication()
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  protected val timeout:Duration = 15.seconds
  implicit val akkaTimeout:akka.util.Timeout = 15.seconds

  "actors.ForceUpdateActor" must {
    "respond to a LookupAtomId message by looking up atom ID and requesting an update from the Updater actor" in {
      val fakeSender = TestProbe("force-update-test")
      val fakeUpdater = TestProbe("fake-updater-test")

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

      val ad: AtomData = new AtomData.Media(data)
      val changeDetails = ContentChangeDetails(revision = 1)

      val testAtom = Atom("fakeAtomId", AtomType.Media, Seq("no-label"), "<p>default html</p>", ad,
        changeDetails, title = Some("atom title"))

      val forceUpdateActor = system.actorOf(Props(new ForceUpdateActor(config){
        override protected val updater:ActorRef = fakeUpdater.ref
        override protected lazy val client:GuardianContentClient = null

        override def lookupAtom(atomId: String, atomType: String): Future[Option[Atom]] = Future(Some(testAtom))
      }), "ForceUpdater")

      val resultFuture = forceUpdateActor.ask(LookupAtomId("some-atom-id"))

      fakeUpdater.expectMsg(10.seconds, DoUpdate(testAtom))
      fakeUpdater.reply(SuccessfulSend())

      val result = Await.result(resultFuture, 15.seconds)
      result should be(Right(SuccessfulSend()))
    }
  }

  "not request an update from the Updater actor when the atom in question is not a media atom" in {
    val fakeSender = TestProbe("force-update-test")
    val fakeUpdater = TestProbe("fake-updater-test")


    val forceUpdateActor = system.actorOf(Props(new ForceUpdateActor(config){
      override protected val updater:ActorRef = fakeUpdater.ref
      override protected lazy val client:GuardianContentClient = null

      override def lookupAtom(atomId: String, atomType: String): Future[Option[Atom]] = Future(None)
    }), "ForceUpdater")

    val resultFuture = forceUpdateActor.ask(LookupAtomId("some-atom-id"))

    fakeUpdater.expectNoMessage(15.seconds)
    val result = Await.result(resultFuture, 15.seconds)
    result should be (Left(ErrorSend("Atom some-atom-id is valid but not a media atom", -1)))
  }

  "report an error if CAPI returns an error" in {
    val fakeSender = TestProbe("force-update-test")
    val fakeUpdater = TestProbe("fake-updater-test")


    val forceUpdateActor = system.actorOf(Props(new ForceUpdateActor(config){
      override protected val updater:ActorRef = fakeUpdater.ref
      override protected lazy val client:GuardianContentClient = null

      override def lookupAtom(atomId: String, atomType: String): Future[Option[Atom]] = Future(throw new Exception("something broke"))
    }), "ForceUpdater")

    val resultFuture = forceUpdateActor.ask(LookupAtomId("some-atom-id"))

    fakeUpdater.expectNoMessage(15.seconds)
    val result = Await.result(resultFuture, 15.seconds)
    result should be(Left(ErrorSend("Atom some-atom-id could not be loaded: java.lang.Exception: something broke", -1)))
  }
}
