package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._

class ContractsControllerSpec
  extends PlaySpec
    with GuiceOneAppPerTest
    with Injecting {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
        "slick.dbs.default.db.driver" -> "org.h2.Driver",
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:contracts;DB_CLOSE_DELAY=-1",
        "slick.dbs.default.db.user" -> "sa",
        "slick.dbs.default.db.password" -> "",
        "play.evolutions.db.default.autoApply" -> true
      )
      .disable[Module]
      .build()

  private def createEmployee(): Int = {
    val json = Json.obj(
      "firstName" -> "Carl",
      "lastName" -> "Employee",
      "email" -> "carl@example.com",
      "mobileNumber" -> "111222333",
      "address" -> "Somewhere"
    )
    route(app, FakeRequest(POST, "/employees").withJsonBody(json)).get
    val list = route(app, FakeRequest(GET, "/employees")).get
    (contentAsJson(list).as[Seq[JsValue]].last \ "id").as[Int]
  }

  "ContractsController" should {

    "create and list contracts" in {
      val employeeId = createEmployee()
      val json = Json.obj(
        "employeeId" -> employeeId,
        "contractType" -> "Permanent",
        "startDate" -> "2024-01-01",
        "endDate" -> JsNull,
        "fullTime" -> true,
        "hoursPerWeek" -> 40
      )
      val res =
        route(app, FakeRequest(POST, "/contracts").withJsonBody(json)).get
      status(res) mustBe CREATED

      val list = route(app, FakeRequest(GET, "/contracts")).get
      status(list) mustBe OK
      contentAsJson(list).as[Seq[JsValue]] must not be empty
    }

    "return validation errors for bad data" in {
      val employeeId = createEmployee()
      val bad = Json.obj(
        "employeeId" -> employeeId,
        "contractType" -> "",
        "startDate" -> "2024-01-01",
        "fullTime" -> true
      )
      val res =
        route(app, FakeRequest(POST, "/contracts").withJsonBody(bad)).get

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "validation_errors")
        .as[JsObject].keys must contain("contractType")
    }

    "update and delete a contract" in {
      val employeeId = createEmployee()
      val create = Json.obj(
        "employeeId" -> employeeId,
        "contractType" -> "Permanent",
        "startDate" -> "2024-01-01",
        "endDate" -> JsNull,
        "fullTime" -> true,
        "hoursPerWeek" -> 40
      )
      route(app, FakeRequest(POST, "/contracts").withJsonBody(create)).get
      val list = route(app, FakeRequest(GET, "/contracts")).get
      val id = (contentAsJson(list).as[Seq[JsValue]].last \ "id").as[Int]

      val upd = Json.obj("hoursPerWeek" -> 20)
      val updRes =
        route(app, FakeRequest(PATCH, s"/contracts/$id").withJsonBody(upd)).get
      status(updRes) mustBe OK
      (contentAsJson(updRes) \ "hoursPerWeek").as[Int] mustBe 20

      val delRes = route(app, FakeRequest(DELETE, s"/contracts/$id")).get
      status(delRes) mustBe NO_CONTENT
    }
  }
}
