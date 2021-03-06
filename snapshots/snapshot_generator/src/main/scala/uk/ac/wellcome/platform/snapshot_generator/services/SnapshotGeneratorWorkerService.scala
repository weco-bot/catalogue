package uk.ac.wellcome.platform.snapshot_generator.services

import akka.Done
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSMessageSender}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.platform.snapshot_generator.models.SnapshotJob
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.{ExecutionContext, Future}

class SnapshotGeneratorWorkerService(
  snapshotService: SnapshotService,
  sqsStream: SQSStream[NotificationMessage],
  snsWriter: SNSMessageSender
)(implicit ec: ExecutionContext)
    extends Runnable {

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] =
    for {
      snapshotJob <- Future.fromTry(fromJson[SnapshotJob](message.body))
      completedSnapshotJob <- snapshotService.generateSnapshot(
        snapshotJob = snapshotJob)
      _ <- Future.fromTry(
        snsWriter.sendT(
          completedSnapshotJob
        ))
    } yield ()
}
