import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessor, IRecordProcessorCheckpointer, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.gu.contentapi.firehose.client.StreamListener
import com.gu.contentapi.firehose.kinesis.KinesisStreamReaderConfig
import com.gu.contentapi.firehose.{ContentApiEventProcessor, ContentApiFirehoseConsumer}

import scala.concurrent.duration.Duration

class MyApiFirehoseConsumer(override val kinesisStreamReaderConfig: KinesisStreamReaderConfig,
                            override val streamListener: StreamListener,
                            override val filterProductionMonitoring: Boolean = false)
  extends ContentApiFirehoseConsumer(kinesisStreamReaderConfig,streamListener,filterProductionMonitoring) {

  override val eventProcessorFactory:IRecordProcessorFactory = new IRecordProcessorFactory {
    override def createProcessor(): IRecordProcessor =
      new ContentApiEventProcessor(filterProductionMonitoring, kinesisStreamReaderConfig.checkpointInterval, kinesisStreamReaderConfig.maxCheckpointBatchSize, streamListener)
  }
}

class MyApiFirehoseProcessor(filterProductionMonitoring: Boolean,
                             override val checkpointInterval: Duration,
                             override val maxCheckpointBatchSize: Int,
                             streamListener: StreamListener)
  extends ContentApiEventProcessor(filterProductionMonitoring, checkpointInterval, maxCheckpointBatchSize,streamListener) {

  /* This method may be called by KCL, e.g. in case of shard splits/merges */
  override def shutdown(checkpointer: IRecordProcessorCheckpointer, reason: ShutdownReason): Unit = {
    if (reason == ShutdownReason.TERMINATE) {
      checkpointer.checkpoint()
    }
    logger.info(s"Shutdown event processor for one shard because $reason")
  }
}