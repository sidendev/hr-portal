package controllers

import play.api.mvc._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

import dtos.CreateEmployeeDto
import models.EmployeesModel
import services.EmployeesService
import utils.ApiError

@Singleton
class EmployeesController @Inject()(
  cc: ControllerComponents,
  employeesService: EmployeesService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val employeesFormat: OFormat[EmployeesModel] = Json.format[EmployeesModel]
  implicit val createEmployeeDtoFormat: OFormat[CreateEmployeeDto] = Json.format[CreateEmployeeDto]

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateEmployeeDto].fold(
      errors => Future.successful(ApiError.InvalidJson(JsError(errors)).toResult),
      dto => employeesService.create(dto).map(e => e.fold(_.toResult, emp => Created(Json.toJson(emp))))
    )
  }

  def listAll(
    contractType: Option[String],
    q: Option[String],
    expiring: Option[String],
    page: Option[Int],
    size: Option[Int]
  ): Action[AnyContent] = Action.async {
    employeesService.search(contractType, q, expiring, page, size)
      .map(_.fold(_.toResult, es => Ok(Json.toJson(es))))
  }

  def findById(id: Int): Action[AnyContent] = Action.async {
    employeesService.findById(id).map(e => e.fold(_.toResult, emp => Ok(Json.toJson(emp))))
  }

  def delete(id: Int): Action[AnyContent] = Action.async {
    employeesService.delete(id).map {
      case Left(err) => err.toResult
      case Right(_) => NoContent
    }
  }

  def update(id: Int): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateEmployeeDto].fold(
      errors => Future.successful(ApiError.InvalidJson(JsError(errors)).toResult),
      dto => employeesService.update(id, dto).map(e => e.fold(_.toResult, emp => Ok(Json.toJson(emp))))
    )
  }
}


