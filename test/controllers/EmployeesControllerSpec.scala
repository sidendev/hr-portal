package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._

class EmployeesControllerSpec
  extends PlaySpec
    with GuiceOneAppPerTest
    with Injecting {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
        "slick.dbs.default.db.driver" -> "org.h2.Driver",
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:employees;DB_CLOSE_DELAY=-1",
        "slick.dbs.default.db.user" -> "sa",
        "slick.dbs.default.db.password" -> "",
        "play.evolutions.db.default.autoApply" -> true
      )
      .disable[Module]
      .build()

  "EmployeesController" should {

    "create an employee" in {
      val json = Json.obj(
        "firstName" -> "Jane",
        "lastName" -> "Doe",
        "email" -> "jane@example.com",
        "mobileNumber" -> "1234567890",
        "address" -> "1 Test St"
      )
      val res = route(app,
        FakeRequest(POST, "/employees").withJsonBody(json)).get

      status(res) mustBe CREATED
    }

    "list employees" in {
      val res = route(app, FakeRequest(GET, "/employees")).get
      status(res) mustBe OK
      contentAsJson(res).as[Seq[JsValue]] must not be empty
    }

    "return validation errors for blank fields" in {
      val bad = Json.obj(
        "firstName" -> "",
        "lastName" -> "",
        "email" -> "",
        "mobileNumber" -> "",
        "address" -> ""
      )
      val res = route(app,
        FakeRequest(POST, "/employees").withJsonBody(bad)).get

      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "validation_errors")
        .as[JsObject].keys must contain("firstName")
    }

    "update and delete an employee" in {
      val createJson = Json.obj(
        "firstName" -> "Temp",
        "lastName" -> "User",
        "email" -> "temp@example.com",
        "mobileNumber" -> "000",
        "address" -> "Temp St"
      )
      route(app, FakeRequest(POST, "/employees").withJsonBody(createJson)).get

      // GETTING NEW ID
      val list = route(app, FakeRequest(GET, "/employees")).get
      val id = (contentAsJson(list).as[Seq[JsValue]].last \ "id").as[Int]

      // UPDATE
      val upd = createJson + ("firstName" -> JsString("Updated"))
      val updRes =
        route(app, FakeRequest(PATCH, s"/employees/$id").withJsonBody(upd)).get
      status(updRes) mustBe OK

      // DELETE
      val delRes = route(app, FakeRequest(DELETE, s"/employees/$id")).get
      status(delRes) mustBe NO_CONTENT
    }
  }
}
