package controllers

import play.api.mvc._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

import models.EmployeesModel
import repositories.EmployeesRepository
import dtos.CreateEmployeeDto
import utils.ApiError
import validators.EmployeesValidator

@Singleton
class EmployeesController @Inject()(
  cc: ControllerComponents,
  employeesRepo: EmployeesRepository
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val employeesFormat: OFormat[EmployeesModel] = Json.format[EmployeesModel]
  implicit val createEmployeeDtoFormat: OFormat[CreateEmployeeDto] = Json.format[CreateEmployeeDto]

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateEmployeeDto].fold(
      errors => Future.successful(ApiError.InvalidJson(JsError(errors)).toResult),
      dto => {
        val validationErrors = EmployeesValidator.validateCreate(dto)
        if (validationErrors.nonEmpty)
          Future.successful(ApiError.ValidationError(validationErrors).toResult)
        else {
          val employee = EmployeesModel(
            firstName = dto.firstName,
            lastName = dto.lastName,
            email = dto.email,
            mobileNumber = dto.mobileNumber,
            address = dto.address
          )
          employeesRepo.create(employee).map(_ => Created(Json.toJson(employee)))
        }
      }
    )
  }

  def listAll: Action[AnyContent] = Action.async {
    employeesRepo.listAll().map { employees =>
      Ok(Json.toJson(employees))
    }
  }

  def findById(id: Int): Action[AnyContent] = Action.async {
    employeesRepo.findById(id).map {
      case Some(employee) => Ok(Json.toJson(employee))
      case None => NotFound(Json.obj("error" -> "Employee not found"))
    }
  }

  def delete(id: Int): Action[AnyContent] = Action.async {
    employeesRepo.deleteById(id).map {
      case 0 => NotFound(Json.obj("error" -> "Employee not found"))
      case _ => NoContent
    }
  }

  def update(id: Int): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateEmployeeDto].fold(
      errors => Future.successful(ApiError.InvalidJson(JsError(errors)).toResult),
      dto => {
        val validationErrors = EmployeesValidator.validateCreate(dto)
        if (validationErrors.nonEmpty)
          Future.successful(ApiError.ValidationError(validationErrors).toResult)
        else {
          val updatedEmployee = EmployeesModel(
            id = Some(id),
            firstName = dto.firstName,
            lastName = dto.lastName,
            email = dto.email,
            mobileNumber = dto.mobileNumber,
            address = dto.address
          )
          employeesRepo.update(id, updatedEmployee).map {
            case 0 => NotFound(Json.obj("error" -> "Employee not found"))
            case _ => Ok(Json.toJson(updatedEmployee))
          }
        }
      }
    )
  }
}


