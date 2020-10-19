package PlutoNextGen

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID

import com.gu.contentatom.thrift.ChangeRecord

case class ImageRef(
                   mimeType: Option[String],
                   file: String,
                   credit: Option[String],
                   copyright: Option[String],
                   source: Option[String],
                   mediaId: Option[String]
                   )

case class ContentChange(

                        )

case class InlineChangeRecord (
                              user:Option[String],
                              at: ZonedDateTime,
                              )
object InlineChangeRecord {
  def fromChangeRecord(rec:ChangeRecord) = {
    new  InlineChangeRecord(
      user=rec.user.map(_.email),
      at=ZonedDateTime.ofInstant(Instant.ofEpochMilli(rec.date), ZoneId.systemDefault())
    )
  }
}
case class UpdateMessage(
                        title: String,
                        category: String,
                        atomId: String,
                        duration: Option[Long],
                        source: Option[String],
                        description: Option[String],
                        posterImage: Option[ImageRef],
                        trailText: Option[String],
                        byline: Option[Seq[String]],
                        keywords:Option[Seq[String]],
                        trailImage: Option[ImageRef],
                        commissionId: Option[String],
                        projectId: Option[String],
                        masterId: Option[String],
                        published: Option[InlineChangeRecord],
                        lastModified: Option[InlineChangeRecord]
                        )
