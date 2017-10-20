import org.scalatest.{FunSuite, MustMatchers, PrivateMethodTester}
import akka.event.{DiagnosticLoggingAdapter, Logging}

class TestVsSearchResponse extends FunSuite with MustMatchers{
  test("VSSearchResponse should parse a search xml"){
    val xmlText = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    |<ItemListDocument xmlns="http://xml.vidispine.com/schema/vidispine">
                    |    <hits>2</hits>
                    |    <item id="KP-2788261" start="-INF" end="+INF">
                    |        <timespan start="-INF" end="+INF"/>
                    |    </item>
                    |    <item id="KP-4425235" start="-INF" end="+INF">
                    |        <timespan start="-INF" end="+INF"/>
                    |    </item>
                    |</ItemListDocument>"""

    val result = VSSearchResponse(xmlText.stripMargin)
    result.hits must be(2)
    result.itemIds.length must be(2)
    result.itemIds.head must be("KP-2788261")
    result.itemIds(1) must be("KP-4425235")
  }
}
