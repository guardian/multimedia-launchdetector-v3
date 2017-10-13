import java.util.Base64

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.gu.contentapi.client.model.v1.Atoms
import com.gu.contentatom.thrift.{Atom, AtomData}
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp._
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Future

class PlutoUpdater(config:Config, actorSystem:ActorSystem) {
  implicit val sttpBackend:SttpBackend[Future,Source[ByteString, Any]] = AkkaHttpBackend.usingActorSystem(actorSystem)

  private val plutoHost=config.getString("pluto_host")
  private val plutoPort=config.getInt("pluto_port")
  private val plutoUser=config.getString("pluto_user")
  private val plutoPass=config.getString("pluto_pass")
  private val proto="http"

  def makeContentXml(atom:Atom)= {
    val mediaContent = atom.data.asInstanceOf[AtomData.Media].media

    <MetadataDocument xmlns="http://xml.vidispine.com/schema/vidispine">
      <timespan start="-INF" end="+INF">
        <group>Asset</group>
          <field>
            <name>title</name>
            <value>{atom.title}</value>
          </field>
          <field>
            <name>gnm_master_website_headline</name>
            <value>{atom.title}</value>
          </field>
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
