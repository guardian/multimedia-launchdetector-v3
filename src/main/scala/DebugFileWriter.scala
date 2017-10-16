import java.io._

import com.gu.contentatom.thrift.Atom
import org.apache.logging.log4j.scala.Logging
import scala.util.{Failure, Try}

object DebugFileWriter extends Logging{
  def writeToFile(filename:String, atom:Atom): Try[Unit] = {
    Try {
      val f = new File(filename)
      val writer = new BufferedWriter(new FileWriter(f))
      writer.write(atom.toString())
      writer.close()
    }
  }
}
