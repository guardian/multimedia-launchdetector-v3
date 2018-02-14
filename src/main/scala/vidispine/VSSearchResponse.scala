package vidispine

import scala.xml._

/*
sample xml:
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ItemListDocument xmlns="http://xml.vidispine.com/schema/vidispine">
    <hits>1</hits>
    <item id="KP-2788261" start="-INF" end="+INF">
        <timespan start="-INF" end="+INF"/>
    </item>
</ItemListDocument>
 */

object VSSearchResponse {
  def apply(incomingXmlString:String): VSSearchResponse = apply(XML.loadString(incomingXmlString))

  def apply(incomingXml: Elem):VSSearchResponse = {
    val itemIdList = (incomingXml \ "item")
      .map(_.attribute("id"))
      .flatMap(_.map(_.text))

    new VSSearchResponse((incomingXml \ "hits").text.toInt, itemIdList)
  }
}

case class VSSearchResponse(hits:Int, itemIds:Seq[String])
