package services

import org.scalatestplus.play._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.{any, eq, anyInt}
import org.mockito.Mockito.{when, verify, verifyNoInteractions}
import scala.concurrent.{ExecutionContext, Future}
import repositories.{EmployeesRepository, ContractsRepository}
import models.{EmployeesModel, ContractsModel}
import dtos.CreateEmployeeDto
import utils.ApiError
import java.time.LocalDate

class EmployeesServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.global
  
  // TEST: checks employee creation with DTO validation
  "EmployeesService.create" should {
    "return validation error when dto is invalid" in {
      // Arrange - setup mocks and invalid first name
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)
      val invalidDto = CreateEmployeeDto("", "Smith", "123456789", Some("Address"))

      // Act
      val result = service.create(invalidDto).futureValue

      // Assert - verify validation error
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.ValidationError]
    }

    // TEST: checks employee creation with valid DTO
    "create employee successfully with generated email" in {
      // Arrange
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      val dto = CreateEmployeeDto("John", "Doe", "123456789", Some("123 Main St"))
      val generatedEmail = "john.doe@example.com"
      val savedEmployee = EmployeesModel(Some(1), "John", "Doe", generatedEmail, "123456789", Some("123 Main St"))
      when(mockEmailsService.generateAddress("John", "Doe"))
        .thenReturn(Future.successful(generatedEmail))
      when(mockEmployeesRepo.create(any[EmployeesModel]))
        .thenReturn(Future.successful(1))
      when(mockEmployeesRepo.findByEmail(generatedEmail))
        .thenReturn(Future.successful(Some(savedEmployee)))
      when(mockEmailsService.registerActive(1, generatedEmail))
        .thenReturn(Future.successful(()))
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.create(dto).futureValue

      // Assert - successful employee creation
      result.isRight mustBe true
      val employee = result.value
      employee.firstName mustBe "John"
      employee.lastName mustBe "Doe"
      employee.email mustBe generatedEmail
      verify(mockEmailsService).generateAddress("John", "Doe")
      verify(mockEmployeesRepo).create(any[EmployeesModel])
      verify(mockEmployeesRepo).findByEmail(generatedEmail)
      verify(mockEmailsService).registerActive(1, generatedEmail)
    }

    // TEST: checks employee creation with insert verification failure
    "return error when employee insert verification fails" in {
      // Arrange
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      val dto = CreateEmployeeDto("Jane", "Smith", "987654321", None)
      val generatedEmail = "jane.smith@example.com"
      when(mockEmailsService.generateAddress("Jane", "Smith"))
        .thenReturn(Future.successful(generatedEmail))
      when(mockEmployeesRepo.create(any[EmployeesModel]))
        .thenReturn(Future.successful(1))
      when(mockEmployeesRepo.findByEmail(generatedEmail))
        .thenReturn(Future.successful(None)) // verification fails - employee not found after insert
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.create(dto).futureValue

      // Assert - verify error
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.InternalServerError]
    }
  }
  
  // TEST: checks we get all employees from repository
  "EmployeesService.listAll" should {
    "return all employees from repository" in {
      // Arrange
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      val employees = Seq(
        EmployeesModel(Some(1), "John", "Doe", "john.doe@example.com", "123", None),
        EmployeesModel(Some(2), "Jane", "Smith", "jane.smith@example.com", "456", None)
      )
      when(mockEmployeesRepo.listAll())
        .thenReturn(Future.successful(employees))
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.listAll().futureValue

      // Assert
      result.isRight mustBe true
      result.value mustBe employees
      verify(mockEmployeesRepo).listAll()
    }

    // TEST: checks we get error when repository fails
    "return error when repository fails" in {
      // Arrange
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      when(mockEmployeesRepo.listAll())
        .thenReturn(Future.failed(new RuntimeException("Database error")))
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.listAll().futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.InternalServerError]
    }
  }
  
  // TEST: Tests finding a specific employee by ID
  "EmployeesService.findById" should {
    "return employee when found" in {
      // Arrange
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      val employee = EmployeesModel(Some(1), "John", "Doe", "john.doe@example.com", "123", None)
      when(mockEmployeesRepo.findById(1))
        .thenReturn(Future.successful(Some(employee)))
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.findById(1).futureValue

      // Assert
      result.isRight mustBe true
      result.value mustBe employee
      verify(mockEmployeesRepo).findById(1)
    }

    // TEST: checks we get error when employee doesn't exist
    "return NotFound when employee doesn't exist" in {
      // Arrange - setup for non-existent employee
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      when(mockEmployeesRepo.findById(999))
        .thenReturn(Future.successful(None))
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.findById(999).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe ApiError.NotFound("Employee not found")
    }
  }
  
  // TEST: Validates employee deletion by ID
  "EmployeesService.delete" should {
    "delete employee successfully" in {
      // Arrange
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      when(mockEmployeesRepo.deleteById(1))
        .thenReturn(Future.successful(1))
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.delete(1).futureValue

      // Assert
      result.isRight mustBe true
      result.value mustBe ()
      verify(mockEmployeesRepo).deleteById(1)
    }

    // TEST: checks we get error when employee doesn't exist on delete
    "return NotFound when employee doesn't exist" in {
      // Arrange
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      when(mockEmployeesRepo.deleteById(999))
        .thenReturn(Future.successful(0))
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.delete(999).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe ApiError.NotFound("Employee not found")
    }
  }
  
  // TEST: Tests employee updates without name changes
  "EmployeesService.update" should {
    "update employee without name change" in {
      // Arrange - setup mocks
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      val existingEmployee = EmployeesModel(Some(1), "John", "Doe", "john.doe@example.com", "123", None)
      val dto = CreateEmployeeDto("John", "Doe", "456", Some("New Address"))
      when(mockEmployeesRepo.findById(1))
        .thenReturn(Future.successful(Some(existingEmployee)))
      when(mockEmployeesRepo.update(anyInt(), any[EmployeesModel]))
        .thenReturn(Future.successful(1))
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.update(1, dto).futureValue

      // Assert - verify successful update - no email change
      result.isRight mustBe true
      val updated = result.value
      updated.firstName mustBe "John"
      updated.lastName mustBe "Doe"
      updated.email mustBe "john.doe@example.com"
      updated.mobileNumber mustBe "456"
      updated.address mustBe Some("New Address")
      verify(mockEmployeesRepo).findById(1)
      verify(mockEmployeesRepo).update(anyInt(), any[EmployeesModel])
      verifyNoInteractions(mockEmailsService)
    }

    // TEST: Tests employee updates with name changes and email change
    "update employee with name change and rotate email" in {
      // Arrange - setup mocks
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      val existingEmployee = EmployeesModel(Some(1), "John", "Doe", "john.doe@example.com", "123", None)
      val dto = CreateEmployeeDto("Jane", "Smith", "456", Some("New Address"))
      val newEmail = "jane.smith@example.com"
      when(mockEmployeesRepo.findById(1))
        .thenReturn(Future.successful(Some(existingEmployee)))
      when(mockEmailsService.rotateActiveEmail(1, "Jane", "Smith"))
        .thenReturn(Future.successful(newEmail))
      when(mockEmployeesRepo.update(anyInt(), any[EmployeesModel]))
        .thenReturn(Future.successful(1))
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.update(1, dto).futureValue

      // Assert
      result.isRight mustBe true
      val updated = result.value
      updated.firstName mustBe "Jane"
      updated.lastName mustBe "Smith"
      updated.email mustBe newEmail
      verify(mockEmployeesRepo).findById(1)
      verify(mockEmailsService).rotateActiveEmail(1, "Jane", "Smith")
      verify(mockEmployeesRepo).update(anyInt(), any[EmployeesModel])
    }

    // TEST: checks we get error when employee doesn't exist on update
    "return NotFound when employee doesn't exist" in {
      // Arrange
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      val dto = CreateEmployeeDto("John", "Doe", "123", None)
      when(mockEmployeesRepo.findById(999))
        .thenReturn(Future.successful(None))
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act - call the service method with non-existent ID
      val result = service.update(999, dto).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe ApiError.NotFound("Employee not found")
    }
  }
  
  // TEST: checks employee search with invalid contract type
  "EmployeesService.search" should {
    "return validation error for invalid contractType" in {
      // Arrange
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act - call the service method with invalid contract type
      val result = service.search(Some("invalid-type"), None, None, None, None).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.ValidationError]
    }

    // TEST: checks employee search with invalid pagination parameters
    "return validation error for invalid pagination parameters" in {
      // Arrange - setup mocks and service with invalid pagination (page 0)
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.search(None, None, None, Some(0), None).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.ValidationError]
    }

    // TEST: checks employee search with name
    "filter employees by name query" in {
      // Arrange
      val mockEmployeesRepo = mock[EmployeesRepository]
      val mockContractsRepo = mock[ContractsRepository]
      val mockEmailsService = mock[EmailsService]
      val employees = Seq(
        EmployeesModel(Some(1), "John", "Doe", "john.doe@example.com", "123", None),
        EmployeesModel(Some(2), "Jane", "Smith", "jane.smith@example.com", "456", None)
      )
      when(mockEmployeesRepo.findByNameLike(Some("%john%")))
        .thenReturn(Future.successful(employees))
      when(mockContractsRepo.listAll())
        .thenReturn(Future.successful(Seq.empty))
      val service = new EmployeesService(mockEmployeesRepo, mockContractsRepo, mockEmailsService)

      // Act
      val result = service.search(None, Some("john"), None, None, None).futureValue

      // Assert
      result.isRight mustBe true
      verify(mockEmployeesRepo).findByNameLike(Some("%john%"))
      verify(mockContractsRepo).listAll()
    }
  }
}
