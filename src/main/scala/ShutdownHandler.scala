import com.gu.contentapi.firehose.ContentApiFirehoseConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.misc.Signal
import sun.misc.SignalHandler

class ShutdownHandler(consumer:ContentApiFirehoseConsumer) extends SignalHandler {
  private final val logger = LoggerFactory.getLogger(this.getClass)

  override def handle(signal: Signal): Unit = {
      logger.info("Caught shutdown signal, trying to terminate cleanly")
      consumer.shutdown()
  }
}