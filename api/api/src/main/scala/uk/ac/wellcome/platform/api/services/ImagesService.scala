package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.{ElasticError, Index}
import uk.ac.wellcome.json.JsonUtil.fromJson
import uk.ac.wellcome.models.work.internal.AugmentedImage
import uk.ac.wellcome.models.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ImagesService(searchService: ElasticsearchService)(
  implicit ec: ExecutionContext) {

  def findImageById(id: String)(
    index: Index): Future[Either[ElasticError, Option[AugmentedImage]]] =
    searchService
      .findResultById(id)(index)
      .map {
        _.map { response =>
          if (response.exists) {
            Some(fromJson[AugmentedImage](response.sourceAsString) match {
              case Success(image) => image
              case Failure(e) =>
                throw new RuntimeException(
                  s"Unable to parse JSON as Image ($e): ${response.sourceAsString}"
                )
            })
          } else {
            None
          }
        }
      }

}