package validators

import utils.validation.Validator
import dtos.{CreateEmployeeDto, UpdateEmployeeDto}

object EmployeesValidator extends Validator {
  def validateCreate(dto: CreateEmployeeDto): Map[String, String] = {
    List(
      isNotEmpty("firstName", dto.firstName),
      isNotEmpty("lastName", dto.lastName),
      isNotEmpty("mobileNumber", dto.mobileNumber),
      isNonBlankIfDefined("address", dto.address)
    ).flatten.toMap
  }

  def validateUpdate(dto: UpdateEmployeeDto): Map[String, String] = {
    List(
      isNonBlankIfDefined("firstName", dto.firstName),
      isNonBlankIfDefined("lastName", dto.lastName),
      isNonBlankIfDefined("mobileNumber", dto.mobileNumber),
      isNonBlankIfDefined("address", dto.address)
    ).flatten.toMap
  }
}

