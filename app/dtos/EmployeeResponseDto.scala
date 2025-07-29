package dtos

import models.EmployeesModel
import play.api.libs.json._

case class EmployeeResponseDto(
  id: Int,
  firstName: String,
  lastName: String,
  email: String,
  mobileNumber: String,
  address: Option[String]
)

object EmployeeResponseDto {
  implicit val format: OFormat[EmployeeResponseDto] = Json.format[EmployeeResponseDto]

  def fromModel(model: EmployeesModel): EmployeeResponseDto =
    EmployeeResponseDto(
      id = model.id.getOrElse(0),
      firstName = model.firstName,
      lastName = model.lastName,
      email = model.email,
      mobileNumber = model.mobileNumber,
      address = model.address
    )
}

