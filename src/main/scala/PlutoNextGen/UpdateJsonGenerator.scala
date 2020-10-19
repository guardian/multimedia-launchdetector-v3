package PlutoNextGen

import java.time.{Instant, LocalDateTime, ZoneOffset, ZonedDateTime}

import io.circe.generic.auto._
import io.circe.syntax._
import com.gu.contentatom.thrift.{Atom, AtomData}

object UpdateJsonGenerator {
  def asIsoTimeString(epochTime:Long):String = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochTime),ZoneOffset.UTC).toString

  def makeContentJson(atom:Atom, currentTime: LocalDateTime) = {
    val mediaContent = atom.data.asInstanceOf[AtomData.Media].media
    val contentChangeDetails = atom.contentChangeDetails

    val msg = UpdateMessage (
      title = mediaContent.title,
      category = mediaContent.category.name,
      atomId = atom.id,
      duration = mediaContent.duration,
      source = mediaContent.source,
      description = mediaContent.description,
      posterImage = mediaContent.posterImage.flatMap(_.master).map(img=>ImageRef(
        img.mimeType,
        img.file,
        img.credit,
        img.copyright,
        img.source,
        mediaContent.posterImage.map(_.mediaId)
      )),
      trailText = mediaContent.trailText,
      byline = mediaContent.byline,
      keywords = mediaContent.keywords,
      trailImage = mediaContent.trailImage.flatMap(_.master).map(img=>ImageRef(
        img.mimeType,
        img.file,
        img.credit,
        img.copyright,
        img.source,
        mediaContent.posterImage.map(_.mediaId)
      )),
      commissionId = mediaContent.metadata.flatMap(_.pluto).flatMap(_.commissionId),
      projectId = mediaContent.metadata.flatMap(_.pluto).flatMap(_.projectId),
      masterId = mediaContent.metadata.flatMap(_.pluto).flatMap(_.masterId),
      published = contentChangeDetails.published.map(InlineChangeRecord.fromChangeRecord),
      lastModified = contentChangeDetails.lastModified.map(InlineChangeRecord.fromChangeRecord)
    )

    msg.asJson
  }
}
