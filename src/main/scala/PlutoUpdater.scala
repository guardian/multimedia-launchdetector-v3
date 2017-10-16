import java.util.Base64

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.gu.contentapi.client.model.v1.Atoms
import com.gu.contentatom.thrift.{Atom, AtomData}
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp._
import com.typesafe.config.{Config, ConfigFactory}
import java.time.{Instant, ZoneOffset, ZonedDateTime}

import com.gu.contentatom.thrift.atom.media.{Asset, AssetType, Platform}

import scala.concurrent.Future
import scala.xml.Elem

class PlutoUpdater(config:Config, actorSystem:ActorSystem) {
  implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]] = AkkaHttpBackend.usingActorSystem(actorSystem)

  private val plutoHost=config.getString("pluto_host")
  private val plutoPort=config.getInt("pluto_port")
  private val plutoUser=config.getString("pluto_user")
  private val plutoPass=config.getString("pluto_pass")
  private val proto="http"

  private def fieldOptionIterable[T](fieldName:String, atomField:Option[Iterable[T]])=atomField.map({ value=>
    <field>
      <name>{fieldName}</name>
      {value.map({ entry=> <value>{entry}</value>})}
    </field>
  })

  private def fieldOption[T](fieldName:String, atomField:Option[T])=atomField.map({ value=>
    <field>
      <name>{fieldName}</name>
      <value>{value}</value>
    </field>
  })

  def asIsoTimeString(epochTime:Long):String = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochTime),ZoneOffset.UTC).toString

  private def youtubePortion(atom:Atom):Option[Elem] = {
    val mediaContent = atom.data.asInstanceOf[AtomData.Media].media

    val ytAssets=mediaContent.assets.filter(_.platform==Platform.Youtube)

    if(ytAssets.isEmpty) return Some(
      <list>
        <field>
          <name>gnm_master_youtube_status</name>
          <value>Unpublished</value>
        </field>
      </list>
    )

    Some(
      // the containing <list> is not used by Vidispine, but is used here so that the scala compiler has a single <elem> to return
    <list>
      <field>
        <name>gnm_master_youtube_title</name>
        <value>{mediaContent.title}</value>
      </field>
      <field>
        <name>gnm_master_youtube_description</name>
        <value>{mediaContent.description}</value>
      </field>
      {fieldOptionIterable("gnm_master_youtube_keywords",mediaContent.metadata.flatMap(_.tags)).getOrElse("")}
      {fieldOption("gnm_master_youtube_category", mediaContent.metadata.map(_.categoryId)).getOrElse("")}
      <field>
        <name>gnm_master_youtube_allowcomments</name>
        <value>{mediaContent.metadata.flatMap(_.commentsEnabled.map(if(_) "allow_comments" else ""))}</value>
      </field>
      <field>
        <name>gnm_master_youtube_uploadstatus</name>
        <value>Upload Succeeded</value>
      </field>
      {fieldOption("gnm_master_youtube_remove",mediaContent.metadata.flatMap(_.expiryDate).map(asIsoTimeString(_)))}
      <field>
        <name>gnm_master_youtube_holdingimage_16x9</name>
        <value/>
      </field>
      {fieldOption("gnm_master_youtube_channelid",mediaContent.metadata.map(_.channelId))}
      {fieldOption("gnm_master_youtube_license",mediaContent.metadata.map(_.license))}
      <field>
        <name>gnm_master_youtube_status</name>
        <value>Published</value>
      </field>
      <field>
        <name>gnm_master_youtube_holdingimage</name>
        <value/>
      </field>
      <field>
        <name>gnm_master_youtube_youtubeurl</name>
        {ytAssets.flatMap({ asset:Asset => <value>https://www.youtube.com/watch?v={asset.id}</value>})}
      </field>
      <field>
        <name>gnm_masteryoutube_publication_date_and_time</name>
        <value>{ZonedDateTime.now().toString}</value>
      </field>
    </list>
    )
  }

  def getMasterId(atom:Atom):Option[String] = atom.data.asInstanceOf[AtomData.Media].media.metadata.flatMap(_.pluto.flatMap(_.masterId))

  def makeContentXml(atom:Atom):Elem = {
    val mediaContent = atom.data.asInstanceOf[AtomData.Media].media
    mediaContent.posterImage.map(_.master.map(_.file))
    mediaContent.posterUrl  //deprecated
    mediaContent.keywords
    mediaContent.description
    mediaContent.assets
    mediaContent.byline
    mediaContent.category
    mediaContent.commissioningDesks
    mediaContent.metadata.flatMap(_.channelId)
    mediaContent.metadata.flatMap(_.categoryId)
    mediaContent.metadata.flatMap(_.commentsEnabled)

    mediaContent.metadata.flatMap(_.license)
    mediaContent.metadata.flatMap(_.privacyStatus)
    mediaContent.metadata.flatMap(_.tags)
    mediaContent.metadata.flatMap(_.pluto.flatMap(_.masterId))



    <MetadataDocument xmlns="http://xml.vidispine.com/schema/vidispine">
      <timespan start="-INF" end="+INF">
        <group>Asset</group>
          {fieldOption("title",atom.title).getOrElse("")}
          <field>
            <name>gnm_master_website_headline</name>
            <value>{mediaContent.title}</value>
          </field>
          {fieldOption("gnm_master_generic_source",mediaContent.source).getOrElse("")}
          {fieldOption("gnm_master_website_description",mediaContent.description).getOrElse("")}
          {youtubePortion(atom).map(_ \ "list").getOrElse("")}
      </timespan>
    </MetadataDocument>
  }

  def authString:String = Base64.getEncoder.encode(s"$plutoUser:$plutoPass".getBytes).toString

//  def send(itemId:String, xmlString:String):Future[Response[Source[ByteString, Any]]] = {
//    val source = Source(xmlString)
//    val hdr = Map(
//      "Accept"->"application/xml",
//      "Authorization"->s"Basic ${}"
//    )
//
//    sttp
//      .streamBody(source)
//      .headers(hdr)
//      .put(uri"$proto://$plutoHost:$plutoPort/API/item/$itemId/metadata")
//      .response(asStream[Source[ByteString,Any]])
//      .send()
//  }
}
