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

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
        "slick.dbs.default.db.driver" -> "org.h2.Driver",
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:testdb;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS PUBLIC\\;RUNSCRIPT FROM 'classpath:test-schema.sql'",
        "slick.dbs.default.db.user" -> "sa",
        "slick.dbs.default.db.password" -> "",
        "play.evolutions.enabled" -> false
      )
      .build()
  }

  private def createEmployee(): Int = {
    val json = Json.obj(
      "firstName" -> "Carl",
      "lastName" -> "Employee",
      "mobileNumber" -> "111222333",
      "address" -> "Somewhere"
    )
    route(app, FakeRequest(POST, "/employees").withJsonBody(json)).get
    val list = route(app, FakeRequest(GET, "/employees")).get
    (contentAsJson(list).as[Seq[JsValue]].last \ "id").as[Int]
  }

  "ContractsController" should {

    // CRUD TESTS:
    "create and list contracts" in {
      // Arrange - create test employee and contract data
      val employeeId = createEmployee()
      val json = Json.obj(
        "employeeId" -> employeeId,
        "contractType" -> "Permanent",
        "startDate" -> "2024-01-01",
        "endDate" -> JsNull,
        "fullTime" -> true,
        "hoursPerWeek" -> 40
      )
      
      // Act
      val res = route(app, FakeRequest(POST, "/contracts").withJsonBody(json)).get
      val list = route(app, FakeRequest(GET, "/contracts")).get

      // Assert - contract was created successfully and list is not empty
      status(res) mustBe CREATED
      status(list) mustBe OK
      contentAsJson(list).as[Seq[JsValue]] must not be empty
    }

    "return validation errors for bad data" in {
      // Arrange - create test employee and invalid contract data
      val employeeId = createEmployee()
      val bad = Json.obj(
        "employeeId" -> employeeId,
        "contractType" -> "",  // empty contract type will trigger validation
        "startDate" -> "2024-01-01",
        "fullTime" -> true
      )
      
      // Act
      val res = route(app, FakeRequest(POST, "/contracts").withJsonBody(bad)).get

      // Assert
      status(res) mustBe BAD_REQUEST
      (contentAsJson(res) \ "validation_errors")
        .as[JsObject].keys must contain("contractType")
    }

    "update and delete a contract" in {
      // Arrange
      val employeeId = createEmployee()
      val create = Json.obj(
        "employeeId" -> employeeId,
        "contractType" -> "Permanent",
        "startDate" -> "2024-01-01",
        "endDate" -> JsNull,
        "fullTime" -> true,
        "hoursPerWeek" -> 40
      )
      
      // Act - create contract and get its ID
      route(app, FakeRequest(POST, "/contracts").withJsonBody(create)).get
      val list = route(app, FakeRequest(GET, "/contracts")).get
      val id = (contentAsJson(list).as[Seq[JsValue]].last \ "id").as[Int]

      // Act - Update the contract
      val upd = Json.obj("hoursPerWeek" -> 20)
      val updRes = route(app, FakeRequest(PATCH, s"/contracts/$id").withJsonBody(upd)).get
      
      // Assert - check the update was successful
      status(updRes) mustBe OK
      (contentAsJson(updRes) \ "hoursPerWeek").as[Int] mustBe 20

      // Act - then delete contract
      val delRes = route(app, FakeRequest(DELETE, s"/contracts/$id")).get
      
      // Assert - check the deletion worked
      status(delRes) mustBe NO_CONTENT
    }

    // ERROR HANDLING TESTS:
    "return 404 when trying to get non-existent contract" in {
      // Arrange
      val nonExistentId = 99999

      // Act
      val res = route(app, FakeRequest(GET, s"/contracts/$nonExistentId")).get

      // Assert
      status(res) mustBe NOT_FOUND
      val responseJson = contentAsJson(res)
      (responseJson \ "error").as[String] must include("Contract not found")
    }

    "return 404 when trying to update non-existent contract" in {
      // Arrange
      val nonExistentId = 99999
      val updateJson = Json.obj("hoursPerWeek" -> 30)

      // Act
      val res = route(app, FakeRequest(PATCH, s"/contracts/$nonExistentId").withJsonBody(updateJson)).get

      // Assert
      status(res) mustBe NOT_FOUND
      val responseJson = contentAsJson(res)
      (responseJson \ "error").as[String] must include("Contract not found")
    }

    "return 404 when trying to delete non-existent contract" in {
      // Arrange
      val nonExistentId = 99999

      // Act
      val res = route(app, FakeRequest(DELETE, s"/contracts/$nonExistentId")).get

      // Assert
      status(res) mustBe NOT_FOUND
      val responseJson = contentAsJson(res)
      (responseJson \ "error").as[String] must include("Contract not found")
    }

    "return 400 when creating contract with invalid JSON format" in {
      // Arrange
      val invalidJson = Json.obj(
        "employeeId" -> "not-a-number", // should be integer
        "contractType" -> "Permanent",
        "startDate" -> "invalid-date", // invalid format
        "fullTime" -> "not-a-boolean" // should be boolean
      )

      // Act
      val res = route(app, FakeRequest(POST, "/contracts").withJsonBody(invalidJson)).get

      // Assert
      status(res) mustBe BAD_REQUEST
    }

    "return 400 when updating contract with invalid data" in {
      // Arrange - create a valid contract
      val employeeId = createEmployee()
      val createJson = Json.obj(
        "employeeId" -> employeeId,
        "contractType" -> "Permanent",
        "startDate" -> "2024-01-01",
        "endDate" -> JsNull,
        "fullTime" -> true,
        "hoursPerWeek" -> 40
      )
      route(app, FakeRequest(POST, "/contracts").withJsonBody(createJson)).get
      val list = route(app, FakeRequest(GET, "/contracts")).get
      val id = (contentAsJson(list).as[Seq[JsValue]].last \ "id").as[Int]

      // make invalid data update
      val invalidUpdateJson = Json.obj(
        "contractType" -> "", // empty contract type should not work
        "hoursPerWeek" -> -5 // negative hours invalid
      )

      // Act
      val res = route(app, FakeRequest(PATCH, s"/contracts/$id").withJsonBody(invalidUpdateJson)).get

      // Assert
      status(res) mustBe BAD_REQUEST
      val responseJson = contentAsJson(res)
      (responseJson \ "validation_errors").as[JsObject].keys must contain("contractType")
    }

    "return 400 when creating contract with missing required fields" in {
      // Arrange
      val employeeId = createEmployee()
      val incompleteJson = Json.obj(
        "employeeId" -> employeeId,
        "contractType" -> "Permanent"
        // missing startDate and fullTime
      )

      // Act
      val res = route(app, FakeRequest(POST, "/contracts").withJsonBody(incompleteJson)).get

      // Assert
      status(res) mustBe BAD_REQUEST
    }

    // OTHER TESTS:
    "confirm part-time contracts with correct hours per week set" in {
      // Arrange - create employee and part-time contract
      val employeeId = createEmployee()
      val json = Json.obj(
        "employeeId" -> employeeId,
        "contractType" -> "Permanent",
        "startDate" -> "2024-01-01",
        "endDate" -> JsNull,
        "fullTime" -> false,
        "hoursPerWeek" -> 20
      )

      // Act
      val res = route(app, FakeRequest(POST, "/contracts").withJsonBody(json)).get

      // Assert - should be created and correct hours
      status(res) mustBe CREATED
      val responseJson = contentAsJson(res)
      (responseJson \ "fullTime").as[Boolean] mustBe false
      (responseJson \ "hoursPerWeek").as[Int] mustBe 20
    }

    "set up employee contract with correct end date" in {
      // Arrange - create employee and contract
      val employeeId = createEmployee()
      val json = Json.obj(
        "employeeId" -> employeeId,
        "contractType" -> "Contract",
        "startDate" -> "2024-01-01",
        "endDate" -> "2024-12-31",
        "fullTime" -> true,
        "hoursPerWeek" -> 40
      )

      // Act
      val res = route(app, FakeRequest(POST, "/contracts").withJsonBody(json)).get

      // Assert - should create contract with correct end date
      status(res) mustBe CREATED
      val responseJson = contentAsJson(res)
      (responseJson \ "contractType").as[String] mustBe "Contract"
      (responseJson \ "endDate").as[String] mustBe "2024-12-31"
    }
  }
}
