package uk.ac.wellcome.platform.reindex.reindex_worker.config

import com.typesafe.config.Config
import grizzled.slf4j.Logging
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSConfig
import uk.ac.wellcome.platform.reindex.reindex_worker.models.ReindexJobConfig
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

object ReindexJobConfigBuilder extends Logging {
  def buildReindexJobConfigMap(
    config: Config): Map[String, ReindexJobConfig[SNSConfig]] = {
    val jsonString = config.requireString("reindexer.jobConfig")
    val configMap =
      fromJson[Map[String, ReindexJobConfig[SNSConfig]]](jsonString).getOrElse(
        throw new RuntimeException(
          s"Unable to parse reindexer.jobConfig: <<$jsonString>>")
      )

    info(s"Read config map $configMap")
    configMap
  }
}
