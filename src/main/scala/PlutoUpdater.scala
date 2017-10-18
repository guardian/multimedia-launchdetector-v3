import java.util.Base64

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import org.apache.logging.log4j.scala.Logging
import com.gu.contentatom.thrift.{Atom, AtomData}
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp._
import com.typesafe.config.{Config, ConfigFactory}
import java.time.{Instant, LocalDateTime, ZoneOffset, ZonedDateTime}

import akka.stream.ActorMaterializer
import com.gu.contentatom.thrift.atom.media.{Asset, AssetType, Platform}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, NodeSeq}

class PlutoUpdater(config:Config, actorSystem:ActorSystem) extends Logging {
  implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]] = AkkaHttpBackend() //AkkaHttpBackend.usingActorSystem(actorSystem)

  private val plutoHost=config.getString("pluto_host")
  private val plutoPort=config.getInt("pluto_port")
  private val plutoUser=config.getString("pluto_user")
  private val plutoPass=config.getString("pluto_pass")
  private val proto="http"

  private def fieldOptionIterable[T](fieldName:String, atomField:Option[Iterable[T]])=atomField.map({ value=>
    <field><name>{fieldName}</name>{value.map({ entry=> <value>{entry}</value>})}</field>
  })

  private def fieldOption[T](fieldName:String, atomField:Option[T])=atomField.map({ value=>
    <field><name>{fieldName}</name> <value>{value}</value></field>
  })

  def asIsoTimeString(epochTime:Long):String = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochTime),ZoneOffset.UTC).toString

  private def youtubePortion(atom:Atom, currentTime: LocalDateTime):Option[Elem] = {
    val mediaContent = atom.data.asInstanceOf[AtomData.Media].media

    val ytAssets=mediaContent.assets.filter(_.platform==Platform.Youtube)

    //logger.debug(s"ytAssets: $ytAssets")
    if(ytAssets.isEmpty) return Some(
      <list><field><name>gnm_master_youtube_status</name> <value>Unpublished</value></field></list>
    )

    Some(
      // the containing <list> is not used by Vidispine, but is used here so that the scala compiler has a single <elem> to return
    <list>
      <field><name>gnm_master_youtube_title</name> <value>{mediaContent.title}</value></field>
      {fieldOption("gnm_master_youtube_description",mediaContent.description).getOrElse("")}
      {fieldOptionIterable("gnm_master_youtube_keywords",mediaContent.metadata.flatMap(_.tags)).getOrElse("")}
      {fieldOption("gnm_master_youtube_category", mediaContent.metadata.flatMap(_.categoryId)).getOrElse("")}
      <field><name>gnm_master_youtube_allowcomments</name> <value>{mediaContent.metadata.flatMap(_.commentsEnabled.map(if(_) "allow_comments" else "")).getOrElse("")}</value></field>
      <field><name>gnm_master_youtube_uploadstatus</name> <value>Upload Succeeded</value></field>
      {fieldOption("gnm_master_youtube_remove",mediaContent.metadata.flatMap(_.expiryDate).map(asIsoTimeString(_))).getOrElse()}
      <field><name>gnm_master_youtube_holdingimage_16x9</name> <value/></field>
      {fieldOption("gnm_master_youtube_channelid",mediaContent.metadata.flatMap(_.channelId)).getOrElse("")}
      {fieldOption("gnm_master_youtube_license",mediaContent.metadata.flatMap(_.license)).getOrElse("")}
      <field><name>gnm_master_youtube_status</name> <value>Published</value></field>
      <field><name>gnm_master_youtube_holdingimage</name><value/></field>
      <field><name>gnm_master_youtube_youtubeurl</name>{ytAssets.flatMap({ asset:Asset => <value>https://www.youtube.com/watch?v={asset.id}</value>})}</field>
      <field><name>gnm_master_youtube_publication_date_and_time</name> <value>{currentTime.toString}</value></field>
    </list>
    )
  }

  def getMasterId(atom:Atom):Option[String] = atom.data.asInstanceOf[AtomData.Media].media.metadata.flatMap(_.pluto.flatMap(_.masterId))

  def makeContentXml(atom:Atom, currentTime: LocalDateTime):Elem = {
    val mediaContent = atom.data.asInstanceOf[AtomData.Media].media
    mediaContent.posterImage.map(_.master.map(_.file))
    mediaContent.posterUrl  //deprecated
    mediaContent.byline
    mediaContent.category
    mediaContent.commissioningDesks
    mediaContent.metadata.flatMap(_.commentsEnabled)

    mediaContent.metadata.flatMap(_.license)
    mediaContent.metadata.flatMap(_.privacyStatus)

    mediaContent.metadata.flatMap(_.pluto.flatMap(_.masterId))

    val contentChangeDetails = atom.contentChangeDetails

//    val lastModUser = for {
//      lastMod <- contentChangeDetails.lastModified
//      user <- lastMod.user
//      email <- user.email
//    } yield email.toString

    contentChangeDetails.lastModified.flatMap(_.user).map(_.email)
    //logger.debug(youtubePortion(atom,currentTime).getOrElse(""))

    //logger.warn(youtubePortion(atom,currentTime).map({content=>content \ "field"}).getOrElse(NodeSeq))

    <MetadataDocument xmlns="http://xml.vidispine.com/schema/vidispine">
      <timespan start="-INF" end="+INF">
        <group>Asset</group>
          {fieldOption("title",atom.title).getOrElse("")}
          <field><name>gnm_master_website_headline</name><value>{mediaContent.title}</value></field>
          {fieldOption("gnm_master_generic_source",mediaContent.source).getOrElse("")}
          {fieldOption("gnm_master_website_description",mediaContent.description).getOrElse("")}
          {fieldOptionIterable("gnm_asset_keywords",mediaContent.keywords).getOrElse("")}

          {youtubePortion(atom,currentTime).map(_ \ "field").getOrElse(NodeSeq)}
      </timespan>
    </MetadataDocument>
  }

//  private def authString:String = Base64.getEncoder.encode(s"$plutoUser:$plutoPass".getBytes).toString
//
//  private def send(itemId:String, xmlString:String):Future[Response[Source[ByteString, Any]]] = {
//    val source:Source[ByteString, Any] = Source(ByteString(xmlString)
//    val hdr = Map(
//      "Accept"->"application/xml",
//      "Authorization"->s"Basic ${}"
//    )
//
//    sttp
//      .put(uri"$proto://$plutoHost:$plutoPort/API/item/$itemId/metadata")
//      .streamBody(source)
//      .headers(hdr)
//      .response(asStream[Source[ByteString, Any]])
//      .send()
//  }
//
//  private def consumeSource(source:Source[ByteString,Any]):Future[String] = {
//    val sink = Sink.reduce((acc:ByteString, unit:ByteString)=>acc.concat(unit))
//    val runnable = source.toMat(sink)(Keep.right)
//    runnable.run().map(_.toString)
//  }
//
//  def request(itemId:String, xmlString:String):Future[Try[Future[String]]] = send(itemId, xmlString).map(_.body match {
//    case Right(source)=>Success(consumeSource(source))
//    case Left(errorString)=>Failure(new RuntimeException(errorString))
//  })
}
