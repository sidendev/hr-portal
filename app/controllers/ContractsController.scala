package controllers

import play.api.mvc._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

import models.ContractsModel
import repositories.ContractsRepository
import dtos.{CreateContractDto, UpdateContractDto}
import utils.ApiError
import validators.ContractsValidator

@Singleton
class ContractsController @Inject()(
  cc: ControllerComponents,
  contractsRepo: ContractsRepository
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val contractFormat: OFormat[ContractsModel] = Json.format[ContractsModel]
  implicit val createDtoFormat: OFormat[CreateContractDto] = Json.format[CreateContractDto]
  implicit val updateDtoFormat: OFormat[UpdateContractDto] = Json.format[UpdateContractDto]

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateContractDto].fold(
      errors => Future.successful(ApiError.InvalidJson(JsError(errors)).toResult),
      dto => {
        val validationErrors = ContractsValidator.validateCreate(dto)
        if (validationErrors.nonEmpty)
          Future.successful(ApiError.ValidationError(validationErrors).toResult)
        else {
          val contract = ContractsModel(
            employeeId = dto.employeeId,
            contractType = dto.contractType,
            startDate = dto.startDate,
            endDate = dto.endDate,
            fullTime = dto.fullTime,
            hoursPerWeek = dto.hoursPerWeek
          )
          contractsRepo.create(contract).map(_ => Created(Json.toJson(contract)))
        }
      }
    )
  }

  def listAll: Action[AnyContent] = Action.async {
    contractsRepo.listAll().map(contracts => Ok(Json.toJson(contracts)))
  }

  def findById(id: Int): Action[AnyContent] = Action.async {
    contractsRepo.findById(id).map {
      case Some(contract) => Ok(Json.toJson(contract))
      case None => NotFound(Json.obj("error" -> "Contract not found"))
    }
  }

  def delete(id: Int): Action[AnyContent] = Action.async {
    contractsRepo.deleteById(id).map {
      case 0 => NotFound(Json.obj("error" -> "Contract not found"))
      case _ => NoContent
    }
  }

  def update(id: Int): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[UpdateContractDto].fold(
      errors => Future.successful(ApiError.InvalidJson(JsError(errors)).toResult),
      dto => {
        val validationErrors = ContractsValidator.validateUpdate(dto)
        if (validationErrors.nonEmpty)
          Future.successful(ApiError.ValidationError(validationErrors).toResult)
        else {
          contractsRepo.findById(id).flatMap {
            case Some(existing) =>
              val merged = existing.copy(
                contractType = dto.contractType.getOrElse(existing.contractType),
                startDate = dto.startDate.getOrElse(existing.startDate),
                endDate = dto.endDate.orElse(existing.endDate),
                fullTime = dto.fullTime.getOrElse(existing.fullTime),
                hoursPerWeek = dto.hoursPerWeek.orElse(existing.hoursPerWeek)
              )
              contractsRepo.update(id, merged).map(_ => Ok(Json.toJson(merged)))
            case None =>
              Future.successful(NotFound(Json.obj("error" -> "Contract not found")))
          }
        }
      }
    )
  }
}

