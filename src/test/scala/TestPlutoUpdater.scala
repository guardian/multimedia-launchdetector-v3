import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.gu.contentatom.thrift.atom.media._
import com.gu.contentatom.thrift._
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.logging.log4j.scala.Logging
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSuite, MustMatchers, PrivateMethodTester}
import org.scalatest.PrivateMethodTester._
import org.scalatest.matchers.MatchResult

import scala.xml.Node

class TestPlutoUpdater extends FunSuite with MustMatchers with PrivateMethodTester with VsXml {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  val config = ConfigFactory.defaultApplication()

  test("isoTimeString should convert a number of seconds into an ISO 8601 string"){
    val u = new PlutoUpdater(config, system)
    val asIsoTimeString = PrivateMethod[String]('asIsoTimeString)

    val result = u.asIsoTimeString(1508147980)

    result must be("2017-10-16T09:59:40Z")
  }

  test("makeContentXml should generate an XML document from an atom without youtube data") {
    val data: MediaAtom = MediaAtom(title = "media title", category = Category.News, description = Some("this is a test"),
      source = Some("my fridge"), duration = Some(543), activeVersion = Some(1), keywords = Some(Seq("this", "that", "other")))
    val changeDetails = ContentChangeDetails(revision = 1)

    val ad: AtomData = new AtomData.Media(data)
    val testAtom = Atom("fakeAtomId", AtomType.Media, Seq("no-label"), "<p>default html</p>", ad,
      changeDetails, title = Some("atom title"))

    val u = new PlutoUpdater(config, system)

    val xml = scala.xml.Utility.trim(u.makeContentXml(testAtom, LocalDateTime.now()))
    xml must be(scala.xml.Utility.trim(<MetadataDocument xmlns="http://xml.vidispine.com/schema/vidispine">
      <timespan start="-INF" end="+INF">
        <group>Asset</group>
        <field>
          <name>title</name> <value>atom title</value>
        </field>
        <field>
          <name>gnm_master_website_headline</name> <value>media title</value>
        </field>
        <field>
          <name>gnm_master_generic_source</name> <value>my fridge</value>
        </field>
        <field>
          <name>gnm_master_website_description</name> <value>this is a test</value>
        </field>
        <field>
          <name>gnm_asset_keywords</name> <value>this</value> <value>that</value> <value>other</value>
        </field>
        <field>
          <name>gnm_master_youtube_status</name> <value>Unpublished</value>
        </field>
      </timespan>
    </MetadataDocument>))
  }

  /** Check that the XMLs are the same, ignoring empty text nodes (whitespace). */
  private def assertEqual(actual: xml.Node, expected: xml.Node) {

    def recurse(actual: xml.Node, expected: xml.Node) {
      // depth-first checks, to get specific failures
      for ((actualChild, expectedChild) <- actual.child zip expected.child) {
        //println(s"$actualChild ?= $expectedChild")
        recurse(actualChild, expectedChild)
      }
      actual mustEqual expected
    }

    recurse(actual, expected)

  }

  test("makeContentXml should generate an XML document from an atom with youtube data"){
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

    val u = new PlutoUpdater(config, system)
    val xml=scala.xml.Utility.trim(u.makeContentXml(testAtom, currentTime))

//    println(xml)
//    println((xml \ "timespan" \ "group").text)
//    val source = (xml \ "timespan" \ "field" \ "name" ).collect(PartialFunction[Node,String]{
//      case n:Node if n.text=="gnm_master_generic_source" =>(n.descendant.head \ "value").text
//    })
//

    val shouldXml = <MetadataDocument xmlns="http://xml.vidispine.com/schema/vidispine"><timespan start="-INF" end="+INF"><group>Asset</group><field><name>title</name><value>atom title</value></field><field><name>gnm_master_website_headline</name><value>media title</value></field><field><name>gnm_master_generic_source</name><value>my fridge</value></field><field><name>gnm_master_website_description</name><value>this is a test</value></field><field><name>gnm_asset_keywords</name><value>this</value><value>that</value><value>other</value></field><field><name>gnm_master_youtube_title</name><value>media title</value></field><field><name>gnm_master_youtube_description</name><value>this is a test</value></field><field><name>gnm_master_youtube_keywords</name><value>tom</value><value>dick</value><value>harry</value></field><field><name>gnm_master_youtube_category</name><value>xxxCategoryIdxxxx</value></field><field><name>gnm_master_youtube_allowcomments</name><value></value></field><field><name>gnm_master_youtube_uploadstatus</name><value>Upload Succeeded</value></field><field><name>gnm_master_youtube_remove</name><value>2017-10-17T15:04:12Z</value></field><field><name>gnm_master_youtube_holdingimage_16x9</name><value/></field><field><name>gnm_master_youtube_channelid</name><value>xxxChannelIdxxx</value></field><field><name>gnm_master_youtube_license</name><value>Ridiculously restrictive</value></field><field><name>gnm_master_youtube_status</name><value>Published</value></field><field><name>gnm_master_youtube_holdingimage</name><value/></field><field><name>gnm_master_youtube_youtubeurl</name><value>https://www.youtube.com/watch?v=abcdefg12345</value></field><field><name>gnm_master_youtube_publication_date_and_time</name><value>{currentTime.toString}</value></field></timespan></MetadataDocument>
    println(xml.toString())
    xml.toString() must be(shouldXml.toString())
    valuesForField(xml,"gnm_master_generic_source") must be(List("my fridge"))
    valuesForField(xml,"gnm_master_website_description") must be(List("this is a test"))
    valuesForField(xml,"gnm_master_youtube_category") must be(List("xxxCategoryIdxxxx"))
    valuesForField(xml,"gnm_master_youtube_keywords") must be(List("tom","dick","harry"))
    valuesForField(xml,"gnm_master_youtube_youtubeurl") must be(List("https://www.youtube.com/watch?v=abcdefg12345"))
//    //assert(xml == shouldXml, xml.child diff shouldXml.child mkString(", "))
    //assertEqual(xml, shouldXml)
  }
}
