package uk.ac.wellcome.bigmessaging.message

import uk.ac.wellcome.storage.s3.S3ObjectLocation

/** Most of our inter-application messages are fairly small, and can be
  * sent inline on SNS/SQS.  But occasionally, we have an unusually large
  * message that can't be sent this way.  In that case, we store a copy
  * of the message in S3, and send an S3ObjectLocation telling the consumer
  * where to find the main message.
  *
  * This trait is the base for the two notification classes -- so we
  * can do `fromJson[MessageNotification]` in the consumer, and get the
  * correct type back.
  */
sealed trait MessageNotification

case class RemoteNotification(location: S3ObjectLocation)
    extends MessageNotification

case class InlineNotification(jsonString: String) extends MessageNotification
