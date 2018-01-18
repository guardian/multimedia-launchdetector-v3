package capi

import com.gu.contentapi.client.model._
import java.time.Instant

import com.gu.contentapi.client.model.v1.ItemResponse
import com.gu.contentatom.thrift.Atom
import com.gu.contentatom.thrift.atom.media.MediaAtom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait CapiCommunicator {
  private val client = new GuardianContentClientInternal("multimedia-launchdetector")

  def lookupAtom(atomId:String, atomType:String):Future[Option[Atom]] = {
    val atomQuery = ItemQuery(s"atom/$atomType/$atomId")
    client.getResponse(atomQuery).map(_.media)
  }
}
