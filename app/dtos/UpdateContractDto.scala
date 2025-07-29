package dtos

import java.time.LocalDate
import play.api.libs.json._

case class UpdateContractDto(
  contractType: Option[String],
  startDate: Option[LocalDate],
  endDate: Option[LocalDate],
  fullTime: Option[Boolean],
  hoursPerWeek: Option[Int]
)

object UpdateContractDto {
  implicit val format: OFormat[UpdateContractDto] = Json.format[UpdateContractDto]
}

