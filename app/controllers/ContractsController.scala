package controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import models.ContractsModel
import repositories.ContractsRepository

@Singleton
class ContractsController @Inject() (
  val controllerComponents: ControllerComponents,
  contractsRepository: ContractsRepository
)(implicit ec: ExecutionContext) extends BaseController {

  implicit val contractsFormat: OFormat[ContractsModel] = Json.format[ContractsModel]

  def listAll: Action[AnyContent] = Action.async {
    contractsRepository.listAll().map { contracts =>
      Ok(Json.toJson(contracts))
    }
  }

  def findById(id: Int): Action[AnyContent] = Action.async {
    contractsRepository.findById(id).map {
      case Some(contract) => Ok(Json.toJson(contract))
      case None           => NotFound(Json.obj("error" -> "Contract not found"))
    }
  }

  def create: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[ContractsModel].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      contract => contractsRepository.create(contract).map { _ =>
        Created(Json.toJson(contract))
      }
    )
  }

  def delete(id: Int): Action[AnyContent] = Action.async {
    contractsRepository.deleteById(id).map { deleted =>
      if (deleted > 0) Ok(Json.obj("status" -> "Deleted"))
      else NotFound(Json.obj("error" -> "Contract not found"))
    }
  }

  def update(id: Int): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[ContractsModel].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      updated => contractsRepository.update(id, updated).map { result =>
        if (result > 0) Ok(Json.obj("status" -> "Updated"))
        else NotFound(Json.obj("error" -> "Contract not found"))
      }
    )
  }
}

