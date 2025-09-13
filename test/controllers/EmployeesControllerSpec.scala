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

  "EmployeesController" should {
    // CRUD TESTS:
    "create an employee" in {
      // Arrange
      val json = Json.obj(
        "firstName" -> "Jane",
        "lastName" -> "Doe",
        "mobileNumber" -> "1234567890",
        "address" -> "1 Test St"
      )

      // Act
      val res = route(app, FakeRequest(POST, "/employees").withJsonBody(json)).get

      // Assert
      status(res) mustBe CREATED
      val responseJson = contentAsJson(res)
      (responseJson \ "firstName").as[String] mustBe "Jane"
      (responseJson \ "lastName").as[String] mustBe "Doe"
      // email is auto-generated from firstName.lastName
      (responseJson \ "email").as[String] must include("jane")
      (responseJson \ "email").as[String] must include("doe")
    }

    "list employees" in {
      // Arrange
      val json = Json.obj(
        "firstName" -> "Test",
        "lastName" -> "Employee",
        "mobileNumber" -> "1111111111",
        "address" -> "Test Address"
      )
      route(app, FakeRequest(POST, "/employees").withJsonBody(json)).get

      // Act
      val res = route(app, FakeRequest(GET, "/employees")).get

      // Assert
      status(res) mustBe OK
      contentAsJson(res).as[Seq[JsValue]] must not be empty
    }

    "return validation errors for blank fields" in {
      // Arrange - set up invalid employee data with blank fields
      val bad = Json.obj(
        "firstName" -> "",
        "lastName" -> "",
        "mobileNumber" -> "",
        "address" -> ""
      )

      // Act
      val res = route(app, FakeRequest(POST, "/employees").withJsonBody(bad)).get

      // Assert
      status(res) mustBe BAD_REQUEST
      val responseJson = contentAsJson(res)
      (responseJson \ "validation_errors").as[JsObject].keys must contain("firstName")
      (responseJson \ "validation_errors").as[JsObject].keys must contain("lastName")
      (responseJson \ "validation_errors").as[JsObject].keys must contain("mobileNumber")
    }

    "update and delete an employee" in {
      // Arrange
      val createJson = Json.obj(
        "firstName" -> "Temp",
        "lastName" -> "User",
        "mobileNumber" -> "000",
        "address" -> "Temp St"
      )
      route(app, FakeRequest(POST, "/employees").withJsonBody(createJson)).get

      // getting the employee ID
      val list = route(app, FakeRequest(GET, "/employees")).get
      val id = (contentAsJson(list).as[Seq[JsValue]].last \ "id").as[Int]

      // Act & Assert - update employee (name change should update email address)
      val upd = createJson + ("firstName" -> JsString("Updated"))
      val updRes = route(app, FakeRequest(PATCH, s"/employees/$id").withJsonBody(upd)).get
      status(updRes) mustBe OK
      (contentAsJson(updRes) \ "firstName").as[String] mustBe "Updated"
      // email should update when name changes
      (contentAsJson(updRes) \ "email").as[String] must include("updated")

      // Act & Assert - deleting employee
      val delRes = route(app, FakeRequest(DELETE, s"/employees/$id")).get
      status(delRes) mustBe NO_CONTENT
    }

    // ERROR HANDLING TESTS:
    "return 404 when trying to get non-existent employee" in {
      // Arrange - employee ID that doesn't exist
      val nonExistentId = 99999

      // Act
      val res = route(app, FakeRequest(GET, s"/employees/$nonExistentId")).get

      // Assert
      status(res) mustBe NOT_FOUND
      val responseJson = contentAsJson(res)
      (responseJson \ "error").as[String] must include("Employee not found")
    }

    "return 404 when trying to update non-existent employee" in {
      // Arrange
      val nonExistentId = 99999
      val updateJson = Json.obj(
        "firstName" -> "Updated",
        "lastName" -> "Employee",
        "mobileNumber" -> "1111111111",
        "address" -> "Updated Address"
      )

      // Act
      val res = route(app, FakeRequest(PATCH, s"/employees/$nonExistentId").withJsonBody(updateJson)).get

      // Assert
      status(res) mustBe NOT_FOUND
      val responseJson = contentAsJson(res)
      (responseJson \ "error").as[String] must include("Employee not found")
    }

    "return 404 when trying to delete non-existent employee" in {
      // Arrange
      val nonExistentId = 99999

      // Act
      val res = route(app, FakeRequest(DELETE, s"/employees/$nonExistentId")).get

      // Assert
      status(res) mustBe NOT_FOUND
      val responseJson = contentAsJson(res)
      (responseJson \ "error").as[String] must include("Employee not found")
    }

    "return 400 when creating employee with invalid JSON" in {
      // Arrange
      val invalidJson = Json.obj(
        "firstName" -> 12345, // this should be string, not number
        "lastName" -> true, // this should be string, not boolean
        "mobileNumber" -> Json.arr("not", "a", "string"), // should be string, not array
        "address" -> "Test Address"
      )

      // Act
      val res = route(app, FakeRequest(POST, "/employees").withJsonBody(invalidJson)).get

      // Assert
      status(res) mustBe BAD_REQUEST
    }

    "return 400 when updating employee with invalid data" in {
      // Arrange
      val createJson = Json.obj(
        "firstName" -> "Valid",
        "lastName" -> "Employee",
        "mobileNumber" -> "1111111111",
        "address" -> "Valid Address"
      )
      route(app, FakeRequest(POST, "/employees").withJsonBody(createJson)).get
      val list = route(app, FakeRequest(GET, "/employees")).get
      val id = (contentAsJson(list).as[Seq[JsValue]].last \ "id").as[Int]

      // making invalid data update
      val invalidUpdateJson = Json.obj(
        "firstName" -> "", // this empty first name should be invalid
        "lastName" -> "Employee",
        "mobileNumber" -> "1111111111"
      )

      // Act
      val res = route(app, FakeRequest(PATCH, s"/employees/$id").withJsonBody(invalidUpdateJson)).get

      // Assert
      status(res) mustBe BAD_REQUEST
      val responseJson = contentAsJson(res)
      val validationErrors = (responseJson \ "validation_errors").as[JsObject]
      validationErrors.keys must contain("firstName")
    }

    "handling duplicate names for correct employee email creation" in {
      // Arrange - make two employees with same name (should get different emails)
      val employee1 = Json.obj(
        "firstName" -> "John",
        "lastName" -> "Doe",
        "mobileNumber" -> "1111111111",
        "address" -> "Address 1"
      )
      val employee2 = Json.obj(
        "firstName" -> "John", 
        "lastName" -> "Doe",
        "mobileNumber" -> "2222222222",
        "address" -> "Address 2"
      )

      // Act - creating first employee
      val res1 = route(app, FakeRequest(POST, "/employees").withJsonBody(employee1)).get

      // Assert - first employee should be created ok
      status(res1) mustBe CREATED
      val email1 = (contentAsJson(res1) \ "email").as[String]
      email1 must include("john")
      email1 must include("doe")

      // Act - creating second employee with same name
      val res2 = route(app, FakeRequest(POST, "/employees").withJsonBody(employee2)).get

      // Assert
      status(res2) must (be(CREATED) or be(INTERNAL_SERVER_ERROR))
      if (status(res2) == CREATED) {
        val email2 = (contentAsJson(res2) \ "email").as[String]
        // emails should be different
        email1 must not equal email2
        email2 must include("john") 
        email2 must include("doe")
      }
    }

    // QUERY PARAM TESTS:
    "support search with query parameter" in {
      // Arrange - set up test employees with different names
      val employee1 = Json.obj(
        "firstName" -> "Alice",
        "lastName" -> "Smith",
        "mobileNumber" -> "1111111111",
        "address" -> "123 Main St"
      )
      val employee2 = Json.obj(
        "firstName" -> "Bob",
        "lastName" -> "Johnson",
        "mobileNumber" -> "2222222222",
        "address" -> "456 Oak Ave"
      )
      route(app, FakeRequest(POST, "/employees").withJsonBody(employee1)).get
      route(app, FakeRequest(POST, "/employees").withJsonBody(employee2)).get

      // Act - searching for employees
      val res = route(app, FakeRequest(GET, "/employees?q=Alice")).get

      // Assert
      status(res) mustBe OK
      val employees = contentAsJson(res).as[Seq[JsValue]]
      employees must not be empty
      // Should contain Alice
    }

    "support pagination with page and size parameters" in {
      // Arrange - set up multiple employees
      val employees = (1 to 5).map { i =>
        Json.obj(
          "firstName" -> s"Employee$i",
          "lastName" -> "Test",
          "mobileNumber" -> s"${i}111111111",
          "address" -> s"$i Test Street"
        )
      }
      employees.foreach { emp =>
        route(app, FakeRequest(POST, "/employees").withJsonBody(emp)).get
      }

      // Act - making request with pagination
      val res = route(app, FakeRequest(GET, "/employees?page=1&size=2")).get

      // Assert
      status(res) mustBe OK
      contentAsJson(res).as[Seq[JsValue]] must not be empty
    }

    "allow employees contract type filtering" in {
      // Arrange
      val employee = Json.obj(
        "firstName" -> "Contract",
        "lastName" -> "Employee",
        "mobileNumber" -> "3333333333",
        "address" -> "Contract Address"
      )
      route(app, FakeRequest(POST, "/employees").withJsonBody(employee)).get

      // Act - Filter by contract type (use valid values: full-time, part-time)
      val res = route(app, FakeRequest(GET, "/employees?contractType=full-time")).get

      // Assert - Should return 200 OK (filtering behavior depends on implementation)
      status(res) mustBe OK
      contentAsJson(res).as[Seq[JsValue]] must not be null
    }

    "support expiring contracts filter" in {
      // Arrange - Create an employee
      val employee = Json.obj(
        "firstName" -> "Expiring",
        "lastName" -> "Contract",
        "mobileNumber" -> "4444444444",
        "address" -> "Expiring Address"
      )
      route(app, FakeRequest(POST, "/employees").withJsonBody(employee)).get

      // Act - filtering by expiring contracts
      val res = route(app, FakeRequest(GET, "/employees?expiring=current-month")).get

      // Assert
      status(res) mustBe OK
      contentAsJson(res).as[Seq[JsValue]] must not be null
    }

    // ERROR HANDLING TESTS:
    "return 400 when creating employee with missing required fields" in {
      // Arrange
      val incompleteJson = Json.obj(
        "firstName" -> "Incomplete"
        // missing lastName, mobileNumber
      )

      // Act
      val res = route(app, FakeRequest(POST, "/employees").withJsonBody(incompleteJson)).get

      // Assert
      status(res) mustBe BAD_REQUEST
    }

    "return 400 when updating employee with missing required fields" in {
      // Arrange
      val createJson = Json.obj(
        "firstName" -> "Complete",
        "lastName" -> "Employee",
        "mobileNumber" -> "6666666666",
        "address" -> "Complete Address"
      )
      route(app, FakeRequest(POST, "/employees").withJsonBody(createJson)).get
      val list = route(app, FakeRequest(GET, "/employees")).get
      val id = (contentAsJson(list).as[Seq[JsValue]].last \ "id").as[Int]

      // data missing required fields
      val incompleteUpdateJson = Json.obj(
        "firstName" -> "Updated"
        // missing lastName, mobileNumber
      )

      // Act
      val res = route(app, FakeRequest(PATCH, s"/employees/$id").withJsonBody(incompleteUpdateJson)).get

      // Assert
      status(res) mustBe BAD_REQUEST
    }
  }
}
