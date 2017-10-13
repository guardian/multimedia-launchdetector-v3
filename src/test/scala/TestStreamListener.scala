import akka.actor.ActorSystem
import com.gu.contentatom.thrift.atom.media.{Category, MediaAtom}
import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.media.MediaAtom
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, MustMatchers}

class TestStreamListener extends FunSuite with MustMatchers with MockFactory {
  test("should decode an incoming atom event") {
    val data: MediaAtom = MediaAtom(title = "media title", category = Category.News, description = Some("this is a test"),
      source = Some("my fridge"), duration = Some(543), activeVersion = Some(1), keywords = Some(Seq("this", "that", "other")))
    val changeDetails = ContentChangeDetails(revision = 1)

    val ad: AtomData = new AtomData.Media(data)
    val testAtom = Atom("fakeAtomId", AtomType.Media, Seq("no-label"), "<p>default html</p>", ad,
      changeDetails, title = Some("atom title"))

    val l = new LaunchdetectorStreamListener(null)
    l.atomUpdateInfo(testAtom) must be("Got atom update for Media Some(atom title): Some(Map(duration -> Some(543), source -> Some(my fridge), assets -> List(), description -> Some(this is a test), posterUrl -> None, posterImage -> None, activeVersion -> Some(1), keywords -> Some(List(this, that, other))))")
  }
}
