import java.time.LocalDateTime

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.gu.contentatom.thrift.atom.media._
import com.gu.contentatom.thrift._
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{FunSuite, MustMatchers, PrivateMethodTester}
import vidispine.{UpdateXmlGenerator, VsXml}
import scala.concurrent.ExecutionContext.Implicits.global

class TestUpdateXmlGenerator extends FunSuite with MustMatchers with PrivateMethodTester with VsXml {
  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  val config = ConfigFactory.defaultApplication()

  test("isoTimeString should convert a number of seconds into an ISO 8601 string"){
    //val u = new PlutoUpdater(config, system)
    val asIsoTimeString = PrivateMethod[String]('asIsoTimeString)

    val result = UpdateXmlGenerator.asIsoTimeString(1508147980)

    result must be("2017-10-16T09:59:40Z")
  }

  test("makeContentXml should generate an XML document from an atom without youtube data") {
    val data: MediaAtom = MediaAtom(title = "media title", category = Category.News, description = Some("this is a test"),
      source = Some("my fridge"), duration = Some(543), activeVersion = Some(1), keywords = Some(Seq("this", "that", "other")))
    val changeDetails = ContentChangeDetails(revision = 1)

    val ad: AtomData = new AtomData.Media(data)
    val testAtom = Atom("fakeAtomId", AtomType.Media, Seq("no-label"), "<p>default html</p>", ad,
      changeDetails, title = Some("atom title"))

    val xml = scala.xml.Utility.trim(UpdateXmlGenerator.makeContentXml(testAtom, LocalDateTime.now()))
    xml must be(scala.xml.Utility.trim(<MetadataDocument xmlns="http://xml.vidispine.com/schema/vidispine"><group>Asset</group><timespan start="-INF" end="+INF"><field><name>title</name><value>atom title</value></field><field><name>gnm_master_website_headline</name><value>media title</value></field><field><name>gnm_master_generic_source</name><value>my fridge</value></field><field><name>gnm_master_website_standfirst</name><value>this is a test</value></field><field><name>gnm_asset_keywords</name><value>this</value><value>that</value><value>other</value></field><field><name>gnm_master_website_uploadstatus</name><value>Upload Succeeded</value></field><field><name>gnm_master_website_item_published</name><value>true</value></field><field mode="add"><name>gnm_master_website_uploadlog</name><value>None: Updated by None</value></field><field><name>gnm_master_generic_status</name><value>Published</value></field><field><name>gnm_master_generic_intendeduploadplatforms</name><value>Website</value><value>Youtube</value></field><field><name>gnm_master_youtube_status</name><value>Unpublished</value></field></timespan></MetadataDocument>))
  }

  test("makeContentXml should generate an XML document from an atom with youtube data") {
    val atomMetadata: Metadata = Metadata(tags = Some(Seq("tom", "dick", "harry")),
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

    val xml = scala.xml.Utility.trim(UpdateXmlGenerator.makeContentXml(testAtom, currentTime))

    val shouldXml = <MetadataDocument xmlns="http://xml.vidispine.com/schema/vidispine"><group>Asset</group><timespan start="-INF" end="+INF"><field><name>title</name><value>atom title</value></field><field><name>gnm_master_website_headline</name><value>media title</value></field><field><name>gnm_master_generic_source</name><value>my fridge</value></field><field><name>gnm_master_website_standfirst</name><value>this is a test</value></field><field><name>gnm_asset_keywords</name><value>this</value><value>that</value><value>other</value></field><field><name>gnm_master_website_uploadstatus</name><value>Upload Succeeded</value></field><field><name>gnm_master_website_item_published</name><value>true</value></field><field mode="add"><name>gnm_master_website_uploadlog</name><value>None: Updated by None</value></field><field><name>gnm_master_generic_status</name><value>Published</value></field><field><name>gnm_master_generic_intendeduploadplatforms</name><value>Website</value><value>Youtube</value></field><field><name>gnm_master_youtube_title</name><value>media title</value></field><field><name>gnm_master_youtube_description</name><value>this is a test</value></field><field><name>gnm_master_youtube_keywords</name><value>tom</value><value>dick</value><value>harry</value></field><field><name>gnm_master_youtube_category</name><value>xxxCategoryIdxxxx</value></field><field><name>gnm_master_youtube_allowcomments</name><value></value></field><field><name>gnm_master_youtube_uploadstatus</name><value>Upload Succeeded</value></field><field><name>gnm_master_youtube_remove</name><value>2017-10-17T15:04:12Z</value></field><field><name>gnm_master_youtube_holdingimage_16x9</name><value/></field><field><name>gnm_master_youtube_channelid</name><value>xxxChannelIdxxx</value></field><field><name>gnm_master_youtube_license</name><value>Ridiculously restrictive</value></field><field><name>gnm_master_youtube_status</name><value>Published</value></field><field><name>gnm_master_youtube_holdingimage</name><value/></field><field><name>gnm_master_youtube_youtubeurl</name><value>https://www.youtube.com/watch?v=abcdefg12345</value></field><field><name>gnm_master_youtube_publish</name><value>{currentTime.toString}</value></field></timespan></MetadataDocument>

    xml.toString() must be(shouldXml.toString())
  }

}
