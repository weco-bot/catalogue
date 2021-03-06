package uk.ac.wellcome.bigmessaging.message

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import io.circe.Decoder
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message
import uk.ac.wellcome.bigmessaging.BigMessageReader
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.{SQSConfig, SQSStream}
import uk.ac.wellcome.monitoring.Metrics
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.store.Store

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class BigMessageStream[T](sqsClient: SqsAsyncClient,
                          sqsConfig: SQSConfig,
                          metrics: Metrics[Future, StandardUnit])(
  implicit
  actorSystem: ActorSystem,
  decoderT: Decoder[T],
  storeT: Store[S3ObjectLocation, T],
  ec: ExecutionContext) {

  private val bigMessageReader = new BigMessageReader[T] {
    override val store: Store[S3ObjectLocation, T] = storeT
    override implicit val decoder: Decoder[T] = decoderT
  }

  private val sqsStream = new SQSStream[NotificationMessage](
    sqsClient = sqsClient,
    sqsConfig = sqsConfig,
    metricsSender = metrics
  )

  def runStream(
    streamName: String,
    modifySource: Source[(Message, T), NotUsed] => Source[Message, NotUsed])
    : Future[Done] =
    sqsStream.runStream(
      streamName,
      source => modifySource(messageFromS3Source(source)))

  def foreach(streamName: String, process: T => Future[Unit]): Future[Done] =
    sqsStream.foreach(
      streamName = streamName,
      process = (notification: NotificationMessage) => {
        for {
          body <- Future.fromTry {
            getBody(notification.body)
          }
          result <- process(body)
        } yield result
      }
    )

  private def messageFromS3Source(
    source: Source[(Message, NotificationMessage), NotUsed])
    : Source[(Message, T), NotUsed] = {
    source.mapAsyncUnordered(sqsConfig.parallelism) {
      case (message, notification) =>
        for {
          deserialisedObject <- Future.fromTry {
            getBody(notification.body)
          }
        } yield (message, deserialisedObject)
    }
  }

  private def getBody(messageString: String): Try[T] =
    fromJson[MessageNotification](messageString).flatMap {
      bigMessageReader.read
    }
}
