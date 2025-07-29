package dtos

import play.api.libs.json._

case class UpdateEmployeeDto(
  firstName: Option[String],
  lastName: Option[String],
  email: Option[String],
  mobileNumber: Option[String],
  address: Option[String]
)

object UpdateEmployeeDto {
  implicit val format: OFormat[UpdateEmployeeDto] = Json.format[UpdateEmployeeDto]
}

