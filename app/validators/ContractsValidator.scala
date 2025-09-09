package validators

import utils.validation.Validator
import dtos.{CreateContractDto, UpdateContractDto}

object ContractsValidator extends Validator {
  def validateCreate(dto: CreateContractDto): Map[String, String] = {
    val baseErrs =
      List(
        isNotEmpty("contractType", dto.contractType),
        isNotEmpty("startDate", dto.startDate.toString),
        isNonBlankIfDefined("endDate", dto.endDate.map(_.toString)),
        isNotEmpty("fullTime", dto.fullTime.toString),
        isNonBlankIfDefined("hoursPerWeek", dto.hoursPerWeek.map(_.toString))
      ).flatten

    val crossErrs =
      dto.endDate match {
        case Some(ed) if ed.isBefore(dto.startDate) =>
          List("endDate" -> "endDate cannot be before startDate")
        case _ => Nil
      }

    (baseErrs ++ crossErrs).toMap
  }

  def validateUpdate(dto: UpdateContractDto): Map[String, String] = {
    val baseErrs =
      List(
        isNonBlankIfDefined("contractType", dto.contractType),
        isNonBlankIfDefined("startDate", dto.startDate.map(_.toString)),
        isNonBlankIfDefined("endDate", dto.endDate.map(_.toString)),
        isNonBlankIfDefined("fullTime", dto.fullTime.map(_.toString)),
        isNonBlankIfDefined("hoursPerWeek", dto.hoursPerWeek.map(_.toString))
      ).flatten

    val crossErrs =
      (dto.startDate, dto.endDate) match {
        case (Some(sd), Some(ed)) if ed.isBefore(sd) =>
          List("endDate" -> "endDate cannot be before startDate")
        case _ => Nil
      }

    (baseErrs ++ crossErrs).toMap
  }
}


