package services

import org.scalatestplus.play._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.{any, eq, anyInt}
import org.mockito.Mockito.{when, verify}
import scala.concurrent.{ExecutionContext, Future}
import repositories.ContractsRepository
import models.ContractsModel
import dtos.{CreateContractDto, UpdateContractDto}
import utils.ApiError
import java.time.LocalDate

class ContractsServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "ContractsService.create" should {
    // TEST: the create method properly validates the input and returns validation errors
    "return validation error when dto is invalid" in {
      // Arrange - setting up mocks and invalid test data
      val mockContractsRepo = mock[ContractsRepository]
      val service = new ContractsService(mockContractsRepo)
      val invalidDto = CreateContractDto(
        employeeId = 1,
        contractType = "", // this is invalid, contract type cannot be empty
        startDate = LocalDate.now,
        endDate = None,
        fullTime = true,
        hoursPerWeek = Some(40)
      )

      // Act
      val result = service.create(invalidDto).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.ValidationError]
    }

    // TEST: the create method creates contract when given valid data
    "create contract successfully with valid dto" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      val dto = CreateContractDto(
        employeeId = 1,
        contractType = "permanent",
        startDate = LocalDate.now,
        endDate = None,
        fullTime = true,
        hoursPerWeek = Some(40)
      )
      when(mockContractsRepo.create(any[ContractsModel]))
        .thenReturn(Future.successful(1))
      val service = new ContractsService(mockContractsRepo)

      // Act
      val result = service.create(dto).futureValue

      // Assert
      result.isRight mustBe true
      val contract = result.value
      contract.employeeId mustBe 1
      contract.contractType mustBe "permanent"
      contract.fullTime mustBe true
      contract.hoursPerWeek mustBe Some(40)
      verify(mockContractsRepo).create(any[ContractsModel])
    }

    // TEST: the create method handles repository errors
    "return error when repository fails" in {
      // Arrange - setup and mock failure
      val mockContractsRepo = mock[ContractsRepository]
      val dto = CreateContractDto(
        employeeId = 1,
        contractType = "temporary",
        startDate = LocalDate.now,
        endDate = Some(LocalDate.now.plusMonths(6)),
        fullTime = false,
        hoursPerWeek = Some(20)
      )
      when(mockContractsRepo.create(any[ContractsModel]))
        .thenReturn(Future.failed(new RuntimeException("Database error")))
      val service = new ContractsService(mockContractsRepo)

      // Act
      val result = service.create(dto).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.InternalServerError]
    }
  }

  "ContractsService.listAll" should {
    // TEST: listAll returns all contracts from repository
    "return all contracts from repository" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      val contracts = Seq(
        ContractsModel(Some(1), 1, "permanent",
          LocalDate.now, None, fullTime = true, Some(40)),
        ContractsModel(Some(2), 2, "temporary",
          LocalDate.now, Some(LocalDate.now.plusMonths(6)), fullTime = false, Some(20))
      )
      when(mockContractsRepo.listAll())
        .thenReturn(Future.successful(contracts))
      val service = new ContractsService(mockContractsRepo)

      // Act
      val result = service.listAll().futureValue

      // Assert
      result.isRight mustBe true
      result.value mustBe contracts
      verify(mockContractsRepo).listAll()
    }

    // TEST: listAll method handles repository errors
    "return error when repository fails" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      when(mockContractsRepo.listAll())
        .thenReturn(Future.failed(new RuntimeException("Database error")))
      val service = new ContractsService(mockContractsRepo)

      // Act - call the service with repository failure
      val result = service.listAll().futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.InternalServerError]
    }
  }

  "ContractsService.findById" should {
    // TEST: findById returns a contract when it exists
    "return contract when found" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      val contract = ContractsModel(Some(1), 1, "permanent",
        LocalDate.now, None, fullTime = true, Some(40))
      when(mockContractsRepo.findById(1))
        .thenReturn(Future.successful(Some(contract)))
      val service = new ContractsService(mockContractsRepo)

      // Act
      val result = service.findById(1).futureValue

      // Assert
      result.isRight mustBe true
      result.value mustBe contract
      verify(mockContractsRepo).findById(1)
    }

    // TEST: findById returns error when contract doesn't exist
    "return NotFound when contract doesn't exist" in {
      // Arrange - setup mocks
      val mockContractsRepo = mock[ContractsRepository]
      when(mockContractsRepo.findById(999))
        .thenReturn(Future.successful(None))
      val service = new ContractsService(mockContractsRepo)

      // Act
      val result = service.findById(999).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe ApiError.NotFound("Contract not found")
    }

    // TEST: findById handles repository errors
    "return error when repository fails" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      when(mockContractsRepo.findById(1))
        .thenReturn(Future.failed(new RuntimeException("Database error")))
      val service = new ContractsService(mockContractsRepo)

      // Act
      val result = service.findById(1).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.InternalServerError]
    }
  }

  "ContractsService.delete" should {
    // TEST: the delete method removes a contract correctly
    "delete contract successfully" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      when(mockContractsRepo.deleteById(1))
        .thenReturn(Future.successful(1))
      val service = new ContractsService(mockContractsRepo)

      // Act
      val result = service.delete(1).futureValue

      // Assert
      result.isRight mustBe true
      result.value mustBe ()
      verify(mockContractsRepo).deleteById(1)
    }

    // TEST: delete method returns an error when trying to delete a non-existent contract
    "return NotFound when contract doesn't exist" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      when(mockContractsRepo.deleteById(999))
        .thenReturn(Future.successful(0))
      val service = new ContractsService(mockContractsRepo)

      // Act - id 999 doesn't exist
      val result = service.delete(999).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe ApiError.NotFound("Contract not found")
    }

    // TEST: delete method handles repository errors
    "return error when repository fails" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      when(mockContractsRepo.deleteById(1))
        .thenReturn(Future.failed(new RuntimeException("Database error")))
      val service = new ContractsService(mockContractsRepo)

      // Act
      val result = service.delete(1).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.InternalServerError]
    }
  }

  "ContractsService.update" should {
    // TEST: the update method returns validation errors for invalid DTOs
    "return validation error when dto is invalid" in {
      // Arrange - setup mocks including invalid test data
      val mockContractsRepo = mock[ContractsRepository]
      val service = new ContractsService(mockContractsRepo)
      val invalidDto = UpdateContractDto(
        contractType = Some(""), // invalid empty contract type
        startDate = None,
        endDate = None,
        fullTime = None,
        hoursPerWeek = None
      )

      // Act
      val result = service.update(1, invalidDto).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.ValidationError]
    }

    // TEST: update method updates a contract correctly
    "update contract successfully with partial data" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      val existingContract = ContractsModel(Some(1), 1, "temporary",
        LocalDate.now, None, fullTime = false, Some(20))
      val updateDto = UpdateContractDto(
        contractType = Some("permanent"),
        startDate = None,
        endDate = None,
        fullTime = Some(true),
        hoursPerWeek = Some(40)
      )
      when(mockContractsRepo.findById(1))
        .thenReturn(Future.successful(Some(existingContract)))
      when(mockContractsRepo.update(anyInt(), any[ContractsModel]))
        .thenReturn(Future.successful(1))
      val service = new ContractsService(mockContractsRepo)

      // Act
      val result = service.update(1, updateDto).futureValue

      // Assert - verify update and correct changes
      result.isRight mustBe true
      val updated = result.value
      updated.contractType mustBe "permanent"
      updated.fullTime mustBe true
      updated.hoursPerWeek mustBe Some(40)
      updated.employeeId mustBe 1
      verify(mockContractsRepo).findById(1)
      verify(mockContractsRepo).update(anyInt(), any[ContractsModel])
    }

    // TEST: update method returns error when updating a non-existent contract
    "return NotFound when contract doesn't exist" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      val updateDto = UpdateContractDto(
        contractType = Some("permanent"),
        startDate = None,
        endDate = None,
        fullTime = None,
        hoursPerWeek = None
      )
      when(mockContractsRepo.findById(999))
        .thenReturn(Future.successful(None))
      val service = new ContractsService(mockContractsRepo)

      // Act - call service with non-existent ID
      val result = service.update(999, updateDto).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe ApiError.NotFound("Contract not found")
    }

    // TEST: update method handles repository update errors
    "return error when repository update fails" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      val existingContract = ContractsModel(Some(1), 1, "temporary",
        LocalDate.now, None, fullTime = false, Some(20))
      val updateDto = UpdateContractDto(
        contractType = Some("permanent"),
        startDate = None,
        endDate = None,
        fullTime = None,
        hoursPerWeek = None
      )
      when(mockContractsRepo.findById(1))
        .thenReturn(Future.successful(Some(existingContract)))
      when(mockContractsRepo.update(anyInt(), any[ContractsModel]))
        .thenReturn(Future.failed(new RuntimeException("Database error")))
      val service = new ContractsService(mockContractsRepo)

      // Act
      val result = service.update(1, updateDto).futureValue

      // Assert
      result.isLeft mustBe true
      result.left.value mustBe a[ApiError.InternalServerError]
    }
  }

  "ContractsService.search" should {
    "return paginated contracts" in {
      // Arrange
      val mockContractsRepo = mock[ContractsRepository]
      val allContracts = (1 to 16).map { i =>
        ContractsModel(
          id = Some(i),
          employeeId = i,
          contractType = if (i % 2 == 0) "permanent" else "contract",
          startDate = LocalDate.now().minusMonths(i),
          endDate = if (i % 2 == 0) None else Some(LocalDate.now().plusMonths(i)),
          fullTime = i % 2 == 0,
          hoursPerWeek = if (i % 2 == 0) Some(40) else Some(20)
        )
      }

      when(mockContractsRepo.listAll())
        .thenReturn(Future.successful(allContracts))

      val service = new ContractsService(mockContractsRepo)

      // Act
      val result = service.search(
        contractType = None,
        q = None,
        expiring = None,
        page = Some(1),
        size = Some(5)
      ).futureValue

      // Assert
      result.isRight mustBe true
      val page1 = result.value
      page1.size mustBe 5
      page1.head.id mustBe Some(1)

      verify(mockContractsRepo).listAll()
    }
  }
}
