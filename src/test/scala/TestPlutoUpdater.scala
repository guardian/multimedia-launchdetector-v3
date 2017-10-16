import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.gu.contentatom.thrift.atom.media.{Category, MediaAtom}
import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.media.MediaAtom
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.logging.log4j.scala.Logging
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, MustMatchers, PrivateMethodTester}
import org.scalatest.PrivateMethodTester._

class TestPlutoUpdater extends FunSuite with MustMatchers with MockFactory with PrivateMethodTester{
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  val config = ConfigFactory.defaultApplication()

  test("makeContentXml should generate an XML document from an atom"){
    val data: MediaAtom = MediaAtom(title = "media title", category = Category.News, description = Some("this is a test"),
      source = Some("my fridge"), duration = Some(543), activeVersion = Some(1), keywords = Some(Seq("this", "that", "other")))
    val changeDetails = ContentChangeDetails(revision = 1)

    val ad: AtomData = new AtomData.Media(data)
    val testAtom = Atom("fakeAtomId", AtomType.Media, Seq("no-label"), "<p>default html</p>", ad,
      changeDetails, title = Some("atom title"))

    val u = new PlutoUpdater(config, system)

    val xml=scala.xml.Utility.trim(u.makeContentXml(testAtom))
    println(xml)
    println((xml \ "timespan" \ "group").text)
    xml must be(<MetadataDocument xmlns="http://xml.vidispine.com/schema/vidispine"><timespan start="-INF" end="+INF"><group>Asset</group><field><name>title</name><value>atom title</value></field><field><name>gnm_master_website_headline</name><value>media title</value></field><field><name>gnm_master_generic_source</name><value>my fridge</value></field><field><name>gnm_master_website_description</name><value>this is a test</value></field></timespan></MetadataDocument>)
  }

  test("isoTimeString should convert a number of seconds into an ISO 8601 string"){
    val u = new PlutoUpdater(config, system)
    val asIsoTimeString = PrivateMethod[String]('asIsoTimeString)

    val result = u.asIsoTimeString(1508147980)

    result must be("2017-10-16T09:59:40Z")
  }
}
