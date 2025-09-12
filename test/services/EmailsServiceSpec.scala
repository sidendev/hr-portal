package services

import org.scalatestplus.play._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.{when, verify, doNothing}
import scala.concurrent.{ExecutionContext, Future}
import repositories.{EmployeesRepository, EmailsRepository}
import models.EmailsModel
import java.sql.Timestamp

class EmailsServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.global

  // TEST: checks generateAddress creates correct email format
  "EmailsService.generateAddress" should {
    "generate base email address when no conflicts exist" in {
      // Arrange
      val mockEmailsRepo = mock[EmailsRepository]
      val mockEmployeesRepo = mock[EmployeesRepository]
      when(mockEmailsRepo.addressExists("john.doe@example.com"))
        .thenReturn(Future.successful(false))
      val service = new EmailsService(mockEmployeesRepo, mockEmailsRepo)

      // Act
      val result = service.generateAddress("John", "Doe").futureValue

      // Assert
      result mustBe "john.doe@example.com"
      verify(mockEmailsRepo).addressExists("john.doe@example.com")
    }

    // TEST: generateAddress appends a number (1) to the email when the address already exists
    "generate numbered email address when base address exists" in {
      // Arrange
      val mockEmailsRepo = mock[EmailsRepository]
      val mockEmployeesRepo = mock[EmployeesRepository]
      when(mockEmailsRepo.addressExists("john.doe@example.com"))
        .thenReturn(Future.successful(true))
      when(mockEmailsRepo.addressExists("john.doe1@example.com"))
        .thenReturn(Future.successful(false))
      val service = new EmailsService(mockEmployeesRepo, mockEmailsRepo)

      // Act
      val result = service.generateAddress("John", "Doe").futureValue

      // Assert
      result mustBe "john.doe1@example.com"
      verify(mockEmailsRepo).addressExists("john.doe@example.com")
      verify(mockEmailsRepo).addressExists("john.doe1@example.com")
    }

    // TEST: Confirms generateAddress can handle multiple same emails created
    // by incrementing numbers until finding an available address to make unique email
    "handle multiple conflicts and find available numbered address" in {
      // Arrange
      val mockEmailsRepo = mock[EmailsRepository]
      val mockEmployeesRepo = mock[EmployeesRepository]
      when(mockEmailsRepo.addressExists("jane.smith@example.com"))
        .thenReturn(Future.successful(true))
      when(mockEmailsRepo.addressExists("jane.smith1@example.com"))
        .thenReturn(Future.successful(true)) // first numbered address exists
      when(mockEmailsRepo.addressExists("jane.smith2@example.com"))
        .thenReturn(Future.successful(false)) // second numbered address is available
      val service = new EmailsService(mockEmployeesRepo, mockEmailsRepo)

      // Act
      val result = service.generateAddress("Jane", "Smith").futureValue

      // Assert
      result mustBe "jane.smith2@example.com"
    }
  }

  // TEST: checks that registerActive deactivates old emails and sets up a new active email
  "EmailsService.registerActive" should {
    "deactivate existing emails and register new active email" in {
      // Arrange
      val mockEmailsRepo = mock[EmailsRepository]
      val mockEmployeesRepo = mock[EmployeesRepository]
      val newEmail = EmailsModel(Some(123L), Some(1), "test@example.com", true, new Timestamp(System.currentTimeMillis()))
      when(mockEmailsRepo.deactivateActive(1))
        .thenReturn(Future.successful(1))
      when(mockEmailsRepo.insert(1, "test@example.com", true))
        .thenReturn(Future.successful(newEmail))
      when(mockEmployeesRepo.setActiveEmail(1, 123L, "test@example.com"))
        .thenReturn(Future.successful(1))
      val service = new EmailsService(mockEmployeesRepo, mockEmailsRepo)

      // Act
      val result = service.registerActive(1, "test@example.com").futureValue

      // Assert
      result mustBe (())
      verify(mockEmailsRepo).deactivateActive(1)
      verify(mockEmailsRepo).insert(1, "test@example.com", true)
      verify(mockEmployeesRepo).setActiveEmail(1, 123L, "test@example.com")
    }
  }
}
