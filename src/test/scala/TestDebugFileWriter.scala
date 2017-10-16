import java.io.FileNotFoundException

import com.gu.contentatom.thrift.atom.media.{Category, MediaAtom}
import com.gu.contentatom.thrift._
import com.gu.contentatom.thrift.atom.media.MediaAtom
import org.scalatest.{FunSuite, MustMatchers, PrivateMethodTester}
import scala.util
import scala.util.{Failure, Success}

class TestDebugFileWriter extends FunSuite with MustMatchers {
  test("DebugFileWriter should write a file"){
    val data: MediaAtom = MediaAtom(title = "media title", category = Category.News, description = Some("this is a test"),
      source = Some("my fridge"), duration = Some(543), activeVersion = Some(1), keywords = Some(Seq("this", "that", "other")))
    val changeDetails = ContentChangeDetails(revision = 1)

    val ad: AtomData = new AtomData.Media(data)
    val testAtom = Atom("fakeAtomId", AtomType.Media, Seq("no-label"), "<p>default html</p>", ad,
      changeDetails, title = Some("atom title"))

    val result = DebugFileWriter.writeToFile("/tmp/testfile.txt", testAtom)
    result must be(Success())
  }

  test("DebugFileWriter should return exceptions as a Failure"){
    val data: MediaAtom = MediaAtom(title = "media title", category = Category.News, description = Some("this is a test"),
      source = Some("my fridge"), duration = Some(543), activeVersion = Some(1), keywords = Some(Seq("this", "that", "other")))
    val changeDetails = ContentChangeDetails(revision = 1)

    val ad: AtomData = new AtomData.Media(data)
    val testAtom = Atom("fakeAtomId", AtomType.Media, Seq("no-label"), "<p>default html</p>", ad,
      changeDetails, title = Some("atom title"))

    val result = DebugFileWriter.writeToFile("/invalid/path/testfile.txt", testAtom)
    val failure = 'failure
    result must be(failure)
    result.fold({ actualException=>
      actualException.getMessage must be("/invalid/path/testfile.txt (No such file or directory)")
    },{unit=>true})
  }
}
