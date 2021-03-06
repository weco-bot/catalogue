package uk.ac.wellcome.platform.api.rest

import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.display.models.Implicits._
import uk.ac.wellcome.elasticsearch.ElasticConfig
import uk.ac.wellcome.platform.api.Tracing
import uk.ac.wellcome.platform.api.models.ApiConfig
import uk.ac.wellcome.platform.api.services.{
  ElasticsearchService,
  ImagesService
}

import scala.concurrent.{ExecutionContext, Future}

class ImagesController(
  elasticsearchService: ElasticsearchService,
  implicit val apiConfig: ApiConfig,
  elasticConfig: ElasticConfig)(implicit ec: ExecutionContext)
    extends CustomDirectives
    with Tracing
    with FailFastCirceSupport {
  import DisplayResultList.encoder
  import ResultResponse.encoder

  def singleImage(id: String, params: SingleImageParams): Route =
    getWithFuture {
      transactFuture("GET /images/{imageId}") {
        val index =
          params._index.map(Index(_)).getOrElse(elasticConfig.imagesIndex)
        imagesService
          .findImageById(id)(index)
          .flatMap {
            case Right(Some(image))
                if params.include.exists(_.visuallySimilar) =>
              imagesService.retrieveSimilarImages(index, image).map {
                similarImages =>
                  complete(
                    ResultResponse(
                      context = contextUri,
                      result = DisplayImage(image, similarImages)
                    )
                  )
              }
            case Right(Some(image)) =>
              Future.successful(
                complete(
                  ResultResponse(
                    context = contextUri,
                    result = DisplayImage(image)
                  )
                ))
            case Right(None) =>
              Future.successful(notFound(s"Image not found for identifier $id"))
            case Left(err) => Future.successful(elasticError(err))
          }
      }
    }

  def multipleImages(params: MultipleImagesParams): Route =
    getWithFuture {
      transactFuture("GET /images") {
        val searchOptions = params.searchOptions(apiConfig)
        val index =
          params._index.map(Index(_)).getOrElse(elasticConfig.imagesIndex)
        imagesService
          .listOrSearchImages(index, searchOptions)
          .map {
            case Left(err) => elasticError(err)
            case Right(resultList) =>
              extractPublicUri { uri =>
                complete(
                  DisplayResultList(
                    resultList,
                    searchOptions,
                    uri,
                    contextUri
                  )
                )
              }
          }
      }
    }

  private lazy val imagesService = new ImagesService(elasticsearchService)
}
