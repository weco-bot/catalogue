package uk.ac.wellcome.bigmessaging.memory

import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.bigmessaging.BigMessageSender
import uk.ac.wellcome.bigmessaging.message.InlineNotification
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.store.memory.MemoryTypedStore
import uk.ac.wellcome.storage.streaming.Codec

class MemoryBigMessageSender[T](
  maxSize: Int = Int.MaxValue,
  storeNamespace: String = "MemoryBigMessageSender",
  messageDestination: String = "MemoryBigMessageSender"
)(
  implicit
  val encoder: Encoder[T],
  codecT: Codec[T]
) extends BigMessageSender[String, T] {

  override val messageSender: MemoryMessageSender = new MemoryMessageSender {
    override val destination: String = messageDestination
  }

  override val typedStore: MemoryTypedStore[ObjectLocation, T] =
    MemoryTypedStoreCompanion[ObjectLocation, T]()

  override val namespace: String = storeNamespace
  override val maxMessageSize: Int = maxSize

  def messages: List[messageSender.underlying.MemoryMessage] =
    messageSender.messages

  def getMessages[S]()(implicit decoder: Decoder[S]): Seq[S] =
    messageSender
      .getMessages[InlineNotification]()
      .map { _.jsonString }
      .map { fromJson[S](_).get }
}