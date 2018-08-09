package vidispine

import java.time.{Instant, LocalDateTime, ZoneOffset, ZonedDateTime}

import com.gu.contentatom.thrift.atom.media.{Asset, Platform}
import com.gu.contentatom.thrift.{Atom, AtomData, ContentChangeDetails}

import scala.xml.{Elem, NodeSeq}

object UpdateXmlGenerator {
  private def fieldOptionIterable[T](fieldName:String, atomField:Option[Iterable[T]])=atomField.map({ value=>
    <field><name>{fieldName}</name>{value.map({ entry=> <value>{entry}</value>})}</field>
  })

  private def fieldOption[T](fieldName:String, atomField:Option[T])=atomField.map({ value=>
    <field><name>{fieldName}</name> <value>{value}</value></field>
  })

  def asIsoTimeString(epochTime:Long):String = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochTime),ZoneOffset.UTC).toString

  private def youtubePortion(atomData:AtomData.Media, currentTime: LocalDateTime):Option[Elem] = {
    val mediaContent = atomData.media

    val ytAssets=mediaContent.assets.filter(_.platform==Platform.Youtube)

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
      {fieldOption("gnm_master_youtube_remove",mediaContent.metadata.flatMap(_.expiryDate).map({time=>asIsoTimeString(time/1000)})).getOrElse()}
      <field><name>gnm_master_youtube_holdingimage_16x9</name> <value/></field>
      {fieldOption("gnm_master_youtube_channelid",mediaContent.metadata.flatMap(_.channelId)).getOrElse("")}
      {fieldOption("gnm_master_youtube_license",mediaContent.metadata.flatMap(_.license)).getOrElse("")}
      <field><name>gnm_master_youtube_status</name> <value>Published</value></field>
      <field><name>gnm_master_youtube_holdingimage</name><value/></field>
      <field><name>gnm_master_youtube_youtubeurl</name>{ytAssets.flatMap({ asset:Asset => <value>https://www.youtube.com/watch?v={asset.id}</value>})}</field>
      <field><name>gnm_master_youtube_publish</name> <value>{currentTime.toString}</value></field>
    </list>
    )
  }

  private def mainstreamPortion(atomData:AtomData.Media, currentTime: LocalDateTime):Option[Elem] = {
    val mediaContent = atomData.media

    Some(
      // the containing <list> is not used by Vidispine, but is used here so that the scala compiler has a single <elem> to return
      <list>
        <field><name>gnm_master_mainstreamsyndication_title</name> <value>{mediaContent.title}</value></field>
        {fieldOption("gnm_master_mainstreamsyndication_description",mediaContent.description).getOrElse("")}
        {fieldOptionIterable("gnm_master_mainstreamsyndication_keywords",mediaContent.metadata.flatMap(_.tags)).getOrElse("")}
        {fieldOption("gnm_master_mainstreamsyndication_category", mediaContent.metadata.flatMap(_.categoryId)).getOrElse("")}
        {fieldOption("gnm_master_mainstreamsyndication_remove",mediaContent.metadata.flatMap(_.expiryDate).map({time=>asIsoTimeString(time/1000)})).getOrElse()}
        <field><name>gnm_master_mainstreamsyndication_holdingimage</name><value/></field>
      </list>
    )
  }

  private def dailymotionPortion(atomData:AtomData.Media, currentTime: LocalDateTime):Option[Elem] = {
    val mediaContent = atomData.media

    Some(
      // the containing <list> is not used by Vidispine, but is used here so that the scala compiler has a single <elem> to return
      <list>
        <field><name>gnm_master_dailymotion_title</name> <value>{mediaContent.title}</value></field>
        {fieldOption("gnm_master_dailymotion_description",mediaContent.description).getOrElse("")}
        {fieldOptionIterable("gnm_master_dailymotion_keywords",mediaContent.metadata.flatMap(_.tags)).getOrElse("")}
        {fieldOption("gnm_master_dailymotion_dailymotioncategory", mediaContent.metadata.flatMap(_.categoryId)).getOrElse("")}
        {fieldOption("gnm_master_dailymotion_remove",mediaContent.metadata.flatMap(_.expiryDate).map({time=>asIsoTimeString(time/1000)})).getOrElse()}
        <field><name>gnm_master_dailymotion_holdingimage_16x9</name><value/></field>
      </list>
    )
  }
  /* For reference: fields
     mediaContent.posterImage.map(_.master.map(_.file))
    mediaContent.posterUrl  //deprecated
    mediaContent.byline
    mediaContent.category
    mediaContent.commissioningDesks
    mediaContent.metadata.flatMap(_.commentsEnabled)

    mediaContent.metadata.flatMap(_.license)
    mediaContent.metadata.flatMap(_.privacyStatus)
   */
  /*
  return a string to go into the upload log
   */
  def makeUploadLog(details: ContentChangeDetails) = {
    val lastModifiedTime = details.lastModified.map({pubTime=>asIsoTimeString(pubTime.date/1000)}).getOrElse("unknown time")

    s"$lastModifiedTime: Updated by ${details.lastModified.flatMap(_.user).map(_.email).getOrElse("unknown user")}"
  }

  def makeContentXml(atom:Atom, currentTime: LocalDateTime):Elem = {
    val mediaContent = atom.data.asInstanceOf[AtomData.Media].media

    mediaContent.metadata.flatMap(_.pluto.flatMap(_.masterId))

    val contentChangeDetails = atom.contentChangeDetails

    contentChangeDetails.lastModified.flatMap(_.user).map(_.email)

    <MetadataDocument xmlns="http://xml.vidispine.com/schema/vidispine">
      <group>Asset</group>
      <timespan start="-INF" end="+INF">
          {fieldOption("title",atom.title).getOrElse("")}
          <field><name>gnm_master_website_headline</name><value>{mediaContent.title}</value></field>
          {fieldOption("gnm_master_generic_source",mediaContent.source).getOrElse("")}
          {fieldOption("gnm_master_website_standfirst",mediaContent.description).getOrElse("")}
          {fieldOptionIterable("gnm_asset_keywords",mediaContent.keywords).getOrElse("")}
          {fieldOption("gnm_master_publication_time", atom.contentChangeDetails.published.map({pubTime=>asIsoTimeString(pubTime.date/1000)})).getOrElse("")}
          <field><name>gnm_master_website_uploadstatus</name><value>Upload Succeeded</value></field>
          <field><name>gnm_master_website_item_published</name><value>true</value></field>
          <field mode="add"><name>gnm_master_website_uploadlog</name><value>{makeUploadLog(atom.contentChangeDetails)}</value></field>
          <field><name>gnm_master_generic_status</name><value>Published</value></field>
          {youtubePortion(atom.data.asInstanceOf[AtomData.Media],currentTime).map(_ \ "field").getOrElse(NodeSeq)}
          {mainstreamPortion(atom.data.asInstanceOf[AtomData.Media],currentTime).map(_ \ "field").getOrElse(NodeSeq)}
          {dailymotionPortion(atom.data.asInstanceOf[AtomData.Media],currentTime).map(_ \ "field").getOrElse(NodeSeq)}
          <field><name>gnm_master_generic_intendeduploadplatforms</name><value>Website</value><value>YouTube</value></field>
      </timespan>
    </MetadataDocument>
  }

  def makeSearchXml(atomId:String):Elem = {
    <ItemSearchDocument xmlns="http://xml.vidispine.com/schema/vidispine">
      <field>
        <name>gnm_master_mediaatom_atomid</name>
        <value>{atomId}</value>
      </field>
    </ItemSearchDocument>
  }
}
