import com.gu.contentapi.firehose.ContentApiFirehoseConsumer
import org.apache.logging.log4j.scala.Logging
import sun.misc.Signal
import sun.misc.SignalHandler

class ShutdownHandler(consumer:ContentApiFirehoseConsumer) extends SignalHandler with Logging {
  override def handle(signal: Signal): Unit = {
      logger.info("Caught shutdown signal, trying to terminate cleanly")
      consumer.shutdown()
  }
}