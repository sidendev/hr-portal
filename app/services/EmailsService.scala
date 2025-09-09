package services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.sql.Timestamp

import repositories.{EmployeesRepository, EmailsRepository}
import utils.EmailGen

@Singleton
class EmailsService @Inject()(
  employeesRepo: EmployeesRepository,
  emailsRepo: EmailsRepository
)(implicit ec: ExecutionContext) {

  // generates a unique email address never used before
  def generateAddress(first: String, last: String): Future[String] = {
    val baseLocal = EmailGen.baseLocal(first, last)

    def tryN(n: Int): Future[String] = {
      val cand = EmailGen.candidate(baseLocal, n)
      emailsRepo.addressExists(cand).flatMap {
        case false => Future.successful(cand)
        case true => tryN(n + 1)
      }
    }

    tryN(0)
  }

  // creates a new active email address for that employee,
  // updates employees.email_id and employees.email columns.
  def registerActive(employeeId: Int, address: String): Future[Unit] = {
    for {
      _ <- emailsRepo.deactivateActive(employeeId)
      inserted <- emailsRepo.insert(employeeId, address, active = true)
      _ <- employeesRepo.setActiveEmail(employeeId, inserted.id.get, address)
    } yield ()
  }

  // changes active email when name gets changed
  def rotateActiveEmail(employeeId: Int, newFirst: String, newLast: String): Future[String] = {
    generateAddress(newFirst, newLast).flatMap { newAddr =>
      registerActive(employeeId, newAddr).map(_ => newAddr)
    }
  }
}
