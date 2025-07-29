package dtos

import play.api.libs.json._

case class CreateEmployeeDto(
  firstName: String,
  lastName: String,
  email: String,
  mobileNumber: String,
  address: Option[String]
)

object CreateEmployeeDto {
  implicit val format: OFormat[CreateEmployeeDto] = Json.format[CreateEmployeeDto]
}
