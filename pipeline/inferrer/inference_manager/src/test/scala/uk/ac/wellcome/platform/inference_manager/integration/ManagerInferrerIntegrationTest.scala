package uk.ac.wellcome.platform.inference_manager.integration

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.{FunSpec, Inside, Matchers, OptionValues}
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.generators.ImageGenerators
import uk.ac.wellcome.models.work.internal.{
  AugmentedImage,
  InferredData,
  MergedImage,
  Minted
}
import uk.ac.wellcome.platform.inference_manager.fixtures.InferenceManagerWorkerServiceFixture
import uk.ac.wellcome.platform.inference_manager.services.FeatureVectorInferrerAdapter

import scala.concurrent.duration._
import scala.io.Source

class ManagerInferrerIntegrationTest
    extends FunSpec
    with Matchers
    with ImageGenerators
    with OptionValues
    with Inside
    with InferenceManagerWorkerServiceFixture[
      MergedImage[Minted],
      AugmentedImage[Minted]
    ] {

  it("augments images with feature vectors") {
    // This is (more than) enough time for the inferrer to have
    // done its prestart work and be ready to use
    eventually(Timeout(scaled(90 seconds))) {
      inferrerIsHealthy shouldBe true
    }
    withWorkerServiceFixtures {
      case (QueuePair(queue, dlq), topic) =>
        val image = createMergedImageWith(
          location = createDigitalLocationWith(
            url = "http://image_server/test-image.jpg"
          )
        ).toMinted
        sendMessage(queue, image)
        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)
          val augmentedImage = getMessages[AugmentedImage[Minted]](topic).head
          inside(augmentedImage) {
            case AugmentedImage(id, _, _, _, inferredData) =>
              id should be(image.id)
              inside(inferredData.value) {
                case InferredData(features1, features2, lshEncodedFeatures) =>
                  features1 should have length 2048
                  features2 should have length 2048
                  every(lshEncodedFeatures) should fullyMatch regex """(\d+)-(\d+)"""
              }
          }
        }
    }
  }

  val localInferrerPort = 3141

  def inferrerIsHealthy: Boolean = {
    val source =
      Source.fromURL(s"http://localhost:${localInferrerPort}/healthcheck")
    try source.mkString.nonEmpty
    catch { case _: Exception => false } finally source.close()
  }

  def withWorkerServiceFixtures[R](
    testWith: TestWith[(QueuePair, Topic), R]): R =
    withLocalSqsQueueAndDlq { queuePair =>
      withLocalSnsTopic { topic =>
        withWorkerService(
          queuePair.queue,
          topic,
          FeatureVectorInferrerAdapter,
          localInferrerPort) { _ =>
          testWith((queuePair, topic))
        }
      }
    }

}