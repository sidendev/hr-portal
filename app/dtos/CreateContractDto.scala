package dtos

import java.time.LocalDate
import play.api.libs.json._

case class CreateContractDto(
  employeeId: Int,
  contractType: String,
  startDate: LocalDate,
  endDate: Option[LocalDate],
  fullTime: Boolean,
  hoursPerWeek: Option[Int]
)

object CreateContractDto {
  implicit val format: OFormat[CreateContractDto] = Json.format[CreateContractDto]
}

