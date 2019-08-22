package uk.ac.wellcome.platform.recorder.services

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.monitoring.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.recorder.fixtures.WorkerServiceFixture
import uk.ac.wellcome.platform.recorder.EmptyMetadata

import uk.ac.wellcome.storage.{Version, Identified, ObjectLocation}
import uk.ac.wellcome.storage.store.HybridStoreEntry

import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.bigmessaging.fixtures.BigMessagingFixture

class RecorderWorkerServiceTest
    extends FunSpec
    with Matchers
    with MockitoSugar
    with Akka
    with SQS
    with ScalaFutures
    with BigMessagingFixture
    with MetricsSenderFixture
    with IntegrationPatience
    with WorkerServiceFixture
    with WorksGenerators {

  it("records an UnidentifiedWork") {
    withLocalSqsQueue { queue =>
      withMemoryMessageSender { msgSender =>
        withRecorderVhs { vhs => 
          withWorkerService(queue, vhs, msgSender) { service =>
            val work = createUnidentifiedWork
            sendMessage[TransformedBaseWork](queue = queue, obj = work)
            eventually {
              assertWorkStored(vhs, work)
            }
          }
        }
      }
    }
  }

  it("stores UnidentifiedInvisibleWorks") {
    withLocalSqsQueue { queue =>
      withMemoryMessageSender { msgSender =>
        withRecorderVhs { vhs => 
          withWorkerService(queue, vhs, msgSender) { service =>
            val invisibleWork = createUnidentifiedInvisibleWork
            sendMessage[TransformedBaseWork](queue = queue, invisibleWork)
            eventually {
              assertWorkStored(vhs, invisibleWork)
            }
          }
        }
      }
    }
  }

  it("doesn't overwrite a newer work with an older work") {
    withLocalSqsQueue { queue =>
      withMemoryMessageSender { msgSender =>
        withRecorderVhs { vhs => 
          withWorkerService(queue, vhs, msgSender) { service =>
            val olderWork = createUnidentifiedWork
            val newerWork = olderWork.copy(version = 10, title = "A nice new thing")
            sendMessage[TransformedBaseWork](queue = queue, newerWork)
            eventually {
              assertWorkStored(vhs, newerWork)
              sendMessage[TransformedBaseWork](
                queue = queue,
                obj = olderWork)
              eventually {
                assertWorkStored(vhs, newerWork)
              }
            }
          }
        }
      }
    }
  }

  it("overwrites an older work with an newer work") {
    withLocalSqsQueue { queue =>
      withMemoryMessageSender { msgSender =>
        withRecorderVhs { vhs => 
          withWorkerService(queue, vhs, msgSender) { service =>
            val olderWork = createUnidentifiedWork
            val newerWork = olderWork.copy(version = 10, title = "A nice new thing")
            sendMessage[TransformedBaseWork](queue = queue, obj = olderWork)
            eventually {
              assertWorkStored(vhs, olderWork)
              sendMessage[TransformedBaseWork](queue = queue, obj = newerWork)
              eventually {
                assertWorkStored(
                  vhs,
                  newerWork,
                  expectedVhsVersion = 1)
              }
            }
          }
        }
      }
    }
  }

  it("fails if saving to the store fails") {
    withLocalSqsQueueAndDlq { case SQS.QueuePair(queue, dlq) =>
      withMemoryMessageSender { msgSender =>
        withBrokenRecorderVhs { vhs => 
          withWorkerService(queue, vhs, msgSender) { service =>
            val work = createUnidentifiedWork
            sendMessage[TransformedBaseWork](queue = queue, obj = work)
            eventually {
              assertQueueEmpty(queue)
              assertQueueHasSize(dlq, 1)
              assertWorkNotStored(vhs, work)
              msgSender.getMessages[ObjectLocation].toList shouldBe Nil
            }
          }
        }
      }
    }
  }

  it("sends the object location to the queue") {
    withLocalSqsQueue { queue =>
      withMemoryMessageSender { msgSender =>
        withRecorderVhs { vhs => 
          withWorkerService(queue, vhs, msgSender) { service =>
            val work = createUnidentifiedWork
            sendMessage[TransformedBaseWork](queue = queue, obj = work)
            eventually {
              val id = work.sourceIdentifier.toString
              msgSender.getMessages[ObjectLocation].toList shouldBe List(
                ObjectLocation("test", s"${id}/${0}")
              )
            }
          }
        }
      }
    }
  }

  private def assertWorkStored[T <: TransformedBaseWork](
    vhs: RecorderVhs,
    work: T,
    expectedVhsVersion: Int = 0) = {

    val id = work.sourceIdentifier.toString
    vhs.getLatest(id) shouldBe
      Right(Identified(
        Version(id, expectedVhsVersion),
        HybridStoreEntry(work, EmptyMetadata())))
  }

  private def assertWorkNotStored[T <: TransformedBaseWork](
    vhs: RecorderVhs,
    work: T) = {

    val id = work.sourceIdentifier.toString
    val workExists = vhs.getLatest(id) match {
      case Left(_) => false
      case Right(_) => true
    }
    workExists shouldBe false
  }
}
