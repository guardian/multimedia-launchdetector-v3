package capi

import com.gu.contentapi.client.model._
import com.gu.contentapi.client.GuardianContentClient
import com.gu.contentatom.thrift.Atom
import com.gu.contentatom.thrift.atom.media.MediaAtom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait CapiCommunicator {
  protected def client: GuardianContentClient

  def lookupAtom(atomId:String, atomType:String):Future[Option[Atom]] = {
    val atomQuery = ItemQuery(s"atom/$atomType/$atomId")
    client.getResponse(atomQuery).map(_.media)
  }
}
