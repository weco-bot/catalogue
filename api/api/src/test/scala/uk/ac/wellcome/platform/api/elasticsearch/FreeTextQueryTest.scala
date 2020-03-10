package uk.ac.wellcome.platform.api.elasticsearch

import com.sksamuel.elastic4s.{ElasticError, Index}
import com.sksamuel.elastic4s.requests.searches.{SearchHit, SearchResponse}
import org.scalatest.{FunSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil.fromJson
import uk.ac.wellcome.models.work.generators.{
  ContributorGenerators,
  GenreGenerators,
  SubjectGenerators,
  WorksGenerators
}
import uk.ac.wellcome.models.work.internal.IdentifiedBaseWork
import uk.ac.wellcome.platform.api.generators.SearchOptionsGenerators
import uk.ac.wellcome.platform.api.models.{SearchQuery, SearchQueryType}
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.platform.api.services.{
  ElasticsearchQueryOptions,
  ElasticsearchService
}

import scala.concurrent.ExecutionContext.Implicits.global

class FreeTextQueryTest
    extends FunSpec
    with Matchers
    with ElasticsearchFixtures
    with ScalaFutures
    with SearchOptionsGenerators
    with SubjectGenerators
    with GenreGenerators
    with WorksGenerators
    with ContributorGenerators {

  val searchService = new ElasticsearchService(
    elasticClient = elasticClient
  )

  describe("Free text query functionality") {

    it("should use the english analyser for titles") {
      withLocalWorksIndex { index =>
        val works = List(
          "Vlad the impaler",
          "Dad the impala",
        ).map { t =>
          createIdentifiedWorkWith(title = Some(t))
        }

        insertIntoElasticsearch(index, works: _*)

        // If we search the non-english analysed fields with the base query
        // `the` would in the search as we're using the `OR` operator
        // and would be matched in both examples above as the root field
        // (not `field` rather than `field.english`, see `WorksIndex.scala`)
        // does not use the english analyser.

        // We wouldn't want to use the english analyser at query time though
        // as we would lose detail used in other where we use exact matching
        val query = "vlad the impaler"
        assertResultsMatchForAllowedQueryTypes(index, query, List(works.head))
      }
    }

    it("searches the canonicalId") {
      withLocalWorksIndex { index =>
        val work = createIdentifiedWorkWith(
          canonicalId = "abc123"
        )

        val query = "abc123"

        insertIntoElasticsearch(index, work)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work))
      }
    }

    it("searches the sourceIdentifiers") {
      withLocalWorksIndex { index =>
        val work = createIdentifiedWorkWith(
          canonicalId = "abc123",
          sourceIdentifier = createSourceIdentifierWith()
        )
        val workNotMatching = createIdentifiedWorkWith(
          canonicalId = "123abc",
          sourceIdentifier = createSourceIdentifierWith()
        )
        val query = work.sourceIdentifier.value

        insertIntoElasticsearch(index, work, workNotMatching)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work))
      }
    }

    it("searches the otherIdentifiers") {
      withLocalWorksIndex { index =>
        val work = createIdentifiedWorkWith(
          canonicalId = "abc123",
          otherIdentifiers = List(createSourceIdentifierWith())
        )
        val workNotMatching = createIdentifiedWorkWith(
          canonicalId = "123abc",
          otherIdentifiers = List(createSourceIdentifierWith())
        )
        val query = work.otherIdentifiers.head.value

        insertIntoElasticsearch(index, work, workNotMatching)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work))
      }
    }

    it("searches the items.canonicalId as keyword") {
      withLocalWorksIndex { index =>
        val work = createIdentifiedWorkWith(
          canonicalId = "abc123",
          items = List(createIdentifiedItemWith(canonicalId = "def"))
        )
        val workNotMatching = createIdentifiedWorkWith(
          canonicalId = "123abc",
          items = List(createIdentifiedItemWith(canonicalId = "def456"))
        )
        val query = "def"

        insertIntoElasticsearch(index, work, workNotMatching)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work))
      }
    }

    it("searches the items.sourceIdentifiers") {
      withLocalWorksIndex { index =>
        val work = createIdentifiedWorkWith(
          canonicalId = "abc123",
          items = List(
            createIdentifiedItemWith(sourceIdentifier =
              createSourceIdentifierWith(value = "sourceIdentifier123")))
        )
        val workNotMatching = createIdentifiedWorkWith(
          canonicalId = "123abc",
          items = List(
            createIdentifiedItemWith(sourceIdentifier =
              createSourceIdentifierWith(value = "sourceIdentifier456")))
        )

        val query = "sourceIdentifier123"

        insertIntoElasticsearch(index, work, workNotMatching)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work))
      }
    }

    it("searches the items.otherIdentifiers") {
      withLocalWorksIndex { index =>
        val work = createIdentifiedWorkWith(
          canonicalId = "abc123",
          items = List(
            createIdentifiedItemWith(otherIdentifiers =
              List(createSourceIdentifierWith(value = "sourceIdentifier123"))))
        )
        val workNotMatching = createIdentifiedWorkWith(
          canonicalId = "def456",
          items = List(
            createIdentifiedItemWith(otherIdentifiers =
              List(createSourceIdentifierWith(value = "sourceIdentifier456"))))
        )
        val query = "sourceIdentifier123"

        insertIntoElasticsearch(index, work, workNotMatching)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work))
      }
    }

    it("matches when searching for an ID") {
      withLocalWorksIndex { index =>
        val work = createIdentifiedWorkWith(
          canonicalId = "abc123"
        )
        val query = "abc123"

        insertIntoElasticsearch(index, work)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work))
      }
    }

    it("matches when searching for multiple IDs") {
      withLocalWorksIndex { index =>
        val work1 = createIdentifiedWorkWith(
          canonicalId = "abc123"
        )
        val work2 = createIdentifiedWorkWith(
          canonicalId = "123abc"
        )
        val query = "abc123 123abc"

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work1, work2))
      }
    }

    it("doesn't match on partial IDs") {
      withLocalWorksIndex { index =>
        val work1 = createIdentifiedWorkWith(
          canonicalId = "abcdefg"
        )
        val work2 = createIdentifiedWorkWith(
          canonicalId = "1234567"
        )
        val query = "123 abcdefg"

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work1))
      }
    }

    it("matches IDs case insensitively") {
      withLocalWorksIndex { index =>
        val work1 = createIdentifiedWorkWith(
          canonicalId = "AbCDeF1234"
        )
        val work2 = createIdentifiedWorkWith(
          canonicalId = "12345Ef"
        )
        val query = "abcdef1234 12345ef"

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work1, work2))
      }
    }

    it("matches if there is extra terms in the query") {
      withLocalWorksIndex { index =>
        val work1 = createIdentifiedWorkWith(
          canonicalId = "AbCDeF1234"
        )
        val work2 = createIdentifiedWorkWith(
          canonicalId = "12345Ef"
        )
        val query = "abcdef1234 12345ef hats, dogs and dolomites"

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work1, work2))
      }
    }

    it("puts ID matches at the top of the results") {
      withLocalWorksIndex { index =>
        val workWithMatchingTitle = createIdentifiedWorkWith(
          title = Some("Standing on wrong side of horse")
        )
        val workWithMatchingId = createIdentifiedWorkWith(
          canonicalId = "AbCDeF1234"
        )

        val query = "abcdef1234 Standing on wrong side of horse"

        insertIntoElasticsearch(
          index,
          workWithMatchingTitle,
          workWithMatchingId)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query,
          List(workWithMatchingId, workWithMatchingTitle))
      }
    }

    it("Searches for contributors") {
      withLocalWorksIndex { index =>
        val matchingWork = createIdentifiedWorkWith(
          contributors = List(createPersonContributorWith("Matching"))
        )
        val notMatchingWork = createIdentifiedWorkWith(
          contributors = List(createPersonContributorWith("Notmatching"))
        )

        val query = "matching"

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(index, query, List(matchingWork))
      }
    }

    it("Searches for genres") {
      withLocalWorksIndex { index =>
        val matchingWork = createIdentifiedWorkWith(
          genres = List(createGenreWithMatchingConcept("Matching"))
        )
        val notMatchingWork = createIdentifiedWorkWith(
          genres = List(createGenreWithMatchingConcept("Notmatching"))
        )

        val query = "matching"

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(index, query, List(matchingWork))
      }
    }

    it("Searches for subjects") {
      withLocalWorksIndex { index =>
        val matchingWork = createIdentifiedWorkWith(
          subjects = List(createSubjectWithMatchingConcept("Matching"))
        )
        val notMatchingWork = createIdentifiedWorkWith(
          subjects = List(createSubjectWithMatchingConcept("Notmatching"))
        )

        val query = "matching"

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(index, query, List(matchingWork))
      }
    }
  }

  private def assertResultsMatchForAllowedQueryTypes(
    index: Index,
    query: String,
    matches: List[IdentifiedBaseWork]) = {
    SearchQueryType.allowed foreach { queryType =>
      val results = searchResults(
        index,
        queryOptions = createElasticsearchQueryOptionsWith(
          searchQuery = Some(SearchQuery(query, queryType))))

      withClue(s"Using: ${queryType.name}") {
        results should contain theSameElementsAs (matches)
      }
    }
  }

  private def searchResults(index: Index,
                            queryOptions: ElasticsearchQueryOptions) = {
    val searchResponseFuture =
      searchService.queryResults(index, queryOptions)
    whenReady(searchResponseFuture) { response =>
      searchResponseToWorks(response)
    }
  }

  private def searchResponseToWorks(
    response: Either[ElasticError, SearchResponse]): List[IdentifiedBaseWork] =
    response.right.get.hits.hits.map { searchHit: SearchHit =>
      jsonToIdentifiedBaseWork(searchHit.sourceAsString)
    }.toList

  private def jsonToIdentifiedBaseWork(document: String): IdentifiedBaseWork =
    fromJson[IdentifiedBaseWork](document).get
}