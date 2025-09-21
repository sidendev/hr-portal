package controllers

import javax.inject._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

import dtos.{CreateContractDto, UpdateContractDto}
import models.ContractsModel
import services.ContractsService
import utils.ApiError

@Singleton
class ContractsController @Inject()(
  cc: ControllerComponents,
  contractsService: ContractsService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val contractFormat: OFormat[ContractsModel] = Json.format[ContractsModel]
  implicit val createDtoFormat: OFormat[CreateContractDto] = Json.format[CreateContractDto]
  implicit val updateDtoFormat: OFormat[UpdateContractDto] = Json.format[UpdateContractDto]

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateContractDto].fold(
      errors => Future.successful(ApiError.InvalidJson(JsError(errors)).toResult),
      dto => contractsService.create(dto).map(e => e.fold(_.toResult, c => Created(Json.toJson(c))))
    )
  }

  def listAll(
    contractType: Option[String],
    q: Option[String],
    expiring: Option[String],
    page: Option[Int],
    size: Option[Int]
  ): Action[AnyContent] = Action.async {
    contractsService.search(contractType, q, expiring, page, size)
      .map(_.fold(_.toResult, cs => Ok(Json.toJson(cs))))
  }

  def findById(id: Int): Action[AnyContent] = Action.async {
    contractsService.findById(id).map(e => e.fold(_.toResult, c => Ok(Json.toJson(c))))
  }

  def delete(id: Int): Action[AnyContent] = Action.async {
    contractsService.delete(id).map {
      case Left(err) => err.toResult
      case Right(_) => NoContent
    }
  }

  def update(id: Int): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[UpdateContractDto].fold(
      errors => Future.successful(ApiError.InvalidJson(JsError(errors)).toResult),
      dto => contractsService.update(id, dto).map(e => e.fold(_.toResult, merged => Ok(Json.toJson(merged))))
    )
  }
}
