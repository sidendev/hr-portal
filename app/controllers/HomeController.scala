package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {

  def index: Action[AnyContent] = Action {
    Ok(Json.obj("message" -> "HR Portal API is running"))
  }
}
