package uk.ac.wellcome.platform.api.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.Operator.AND
import com.sksamuel.elastic4s.requests.searches.queries.BoolQuery
import com.sksamuel.elastic4s.requests.searches.queries.matches.MultiMatchQueryBuilderType.{
  BEST_FIELDS,
  CROSS_FIELDS,
  PHRASE
}
import com.sksamuel.elastic4s.requests.searches.queries.matches.{
  FieldWithOptionalBoost,
  MultiMatchQuery
}

trait WorksQuery {
  def apply(q: String): BoolQuery
}

case object WorksMultiMatcher extends WorksQuery {
  override def apply(q: String): BoolQuery = {
    boolQuery().should(
      prefixQuery("data.title.keyword", q).boost(1000),
      MultiMatchQuery(
        q,
        `type` = Some(BEST_FIELDS),
        operator = Some(AND),
        fields = Seq(
          ("canonicalId.text", Some(1000)),
          ("sourceIdentifier.value.text", Some(1000)),
          ("data.otherIdentifiers.value.text", Some(1000)),
          ("data.items.id.canonicalId.text", Some(1000)),
          ("data.items.id.sourceIdentifier.value.text", Some(1000)),
          ("data.items.id.otherIdentifiers.value.text", Some(1000)),
          ("data.title", Some(100)),
          ("data.title.english", Some(100)),
          ("data.title.shingles", Some(100)),
          ("data.alternativeTitles", Some(100)),
          ("data.subjects.concepts.label", Some(10)),
          ("data.genres.concepts.label", Some(10)),
          ("data.contributors.agent.label", Some(10)),
          ("data.production.*.label", Some(10)),
          ("data.description", None),
          ("data.physicalDescription", None),
          ("data.language.label", None),
          ("data.edition", None),
          ("data.notes.content", None),
        ).map(f => FieldWithOptionalBoost(f._1, f._2.map(_.toDouble)))
      )
    )
  }
}

case object WorksPhraserBeam extends WorksQuery {
  def apply(q: String): BoolQuery = {
    val fields = Seq(
      "data.subjects.concepts.label",
      "data.genres.concepts.label",
      "data.contributors.agent.label",
      "data.title.english",
      "data.description.english",
      "data.alternativeTitles.english",
      "data.physicalDescription.english",
      "data.production.*.label",
      "data.language.label",
      "data.edition",
      "data.notes.content.english",
      "data.collectionPath.path",
      "data.collectionPath.label",
    ).map(FieldWithOptionalBoost(_, None))

    val idFields = Seq(
      "canonicalId.text",
      "sourceIdentifier.value.text",
      "data.otherIdentifiers.value.text",
      "data.items.id.canonicalId.text",
      "data.items.id.sourceIdentifier.value.text",
      "data.items.id.otherIdentifiers.value.text",
    ).map(FieldWithOptionalBoost(_, None))

    boolQuery().must(
      should(
        MultiMatchQuery(
          text = q,
          fields = fields,
          minimumShouldMatch = Some("60%"),
          `type` = Some(CROSS_FIELDS),
          operator = Some(AND),
          boost = Some(2) // Double the OR query
        ),
        MultiMatchQuery(
          fields = idFields,
          text = q,
          `type` = Some(CROSS_FIELDS)
        ),
        MultiMatchQuery(
          text = q,
          `type` = Some(PHRASE),
          fields = Seq(
            FieldWithOptionalBoost("data.title.keyword", Some(5000)),
            FieldWithOptionalBoost("data.title.english", Some(2000))
          )
        ),
        MultiMatchQuery(
          text = q,
          `type` = Some(PHRASE),
          fields = Seq(
            FieldWithOptionalBoost(
              "data.genres.concepts.label.keyword",
              Some(5000)),
            FieldWithOptionalBoost("data.genres.concepts.label", Some(2000))
          )
        ),
        MultiMatchQuery(
          text = q,
          `type` = Some(PHRASE),
          fields = Seq(
            FieldWithOptionalBoost(
              "data.subjects.concepts.label.keyword",
              Some(5000)),
            FieldWithOptionalBoost("data.subjects.concepts.label", Some(2000))
          )
        ),
        MultiMatchQuery(
          text = q,
          `type` = Some(PHRASE),
          fields = Seq(
            FieldWithOptionalBoost(
              "data.contributors.agent.label.keyword",
              Some(5000)),
            FieldWithOptionalBoost("data.contributors.agent.label", Some(2000))
          )
        )
      )
    )
  }
}
