package uk.ac.wellcome.models.work.internal

sealed trait Location {
  val locationType: LocationType
  val accessConditions: List[AccessCondition]

  def isRestrictedOrClosed =
    accessConditions.exists { accessCondition =>
      accessCondition.status match {
        case Some(AccessStatus.Restricted) => true
        case Some(AccessStatus.Closed)     => true
        case _                             => false
      }
    }
}

case class DigitalLocation(
  url: String,
  locationType: LocationType,
  license: Option[License] = None,
  credit: Option[String] = None,
  accessConditions: List[AccessCondition] = Nil,
  ontologyType: String = "DigitalLocation"
) extends Location

case class PhysicalLocation(
  locationType: LocationType,
  label: String,
  accessConditions: List[AccessCondition] = Nil,
  ontologyType: String = "PhysicalLocation"
) extends Location
