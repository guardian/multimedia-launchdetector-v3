import java.io.FileNotFoundException

import com.gu.contentatom.thrift.atom.media.{Category, MediaAtom}
import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.media.MediaAtom
import com.typesafe.config.ConfigFactory
import org.scalatest.{FunSuite, MustMatchers, PrivateMethodTester}

import scala.util
import scala.util.{Failure, Success}

class TestConfig extends FunSuite with MustMatchers {
  test("should pick up config"){
    val config = ConfigFactory.load()
    config.getString("capi_stream_name") must be("STREAM_NAME_HERE")
  }

}
