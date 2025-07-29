package dtos

import models.ContractsModel
import play.api.libs.json._
import java.time.LocalDate

case class ContractResponseDto(
  id: Int,
  employeeId: Int,
  contractType: String,
  startDate: LocalDate,
  endDate: Option[LocalDate],
  fullTime: Boolean,
  hoursPerWeek: Option[Int]
)

object ContractResponseDto {
  implicit val format: OFormat[ContractResponseDto] = Json.format[ContractResponseDto]

  def fromModel(model: ContractsModel): ContractResponseDto =
    ContractResponseDto(
      id = model.id.getOrElse(0),
      employeeId = model.employeeId,
      contractType = model.contractType,
      startDate = model.startDate,
      endDate = model.endDate,
      fullTime = model.fullTime,
      hoursPerWeek = model.hoursPerWeek
    )
}

