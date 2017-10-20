package vidispine

import scala.xml.{Node, NodeSeq}

trait VsXml {
  def valuesForField(xmlNode:NodeSeq, fieldName: String):Seq[String] = {
    val field = (xmlNode \ "timespan" \ "field").filter({node:Node=> (node \ "name").text==fieldName})
    (field \ "value").map(_.text)
  }
}
