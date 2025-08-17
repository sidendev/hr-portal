package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json

class HomeControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "play.modules.disabled" -> Seq("Module"),
        "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
        "slick.dbs.default.db.driver" -> "org.h2.Driver",
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "slick.dbs.default.db.user" -> "sa",
        "slick.dbs.default.db.password" -> "",
        "play.evolutions.db.default.autoApply" -> true
      )
      .build()

  "HomeController GET" should {

    "return JSON from a new instance of controller" in {
      val controller = new HomeController(stubControllerComponents())
      val result = controller.index().apply(FakeRequest(GET, "/"))

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Json.obj("message" -> "HR Portal API is running")
    }

    "return JSON from the application" in {
      val controller = inject[HomeController]
      val result = controller.index().apply(FakeRequest(GET, "/"))

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Json.obj("message" -> "HR Portal API is running")
    }

    "return JSON from the router" in {
      val request = FakeRequest(GET, "/")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result) mustBe Json.obj("message" -> "HR Portal API is running")
    }
  }
}
