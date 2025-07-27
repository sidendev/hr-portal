package utils

import slick.jdbc.JdbcProfile
import play.api.db.slick.DatabaseConfigProvider
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import models._
import models.Tables._

import java.time.LocalDate

@Singleton
class DataSeeder @Inject() (
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  def seed(): Future[Unit] = {
    val initialEmployees = Seq(
      EmployeesModel(None, "Alice", "Smith", "alice@example.com", "1234567890", Some("123 Main St, NY, USA, 001001")),
      EmployeesModel(None, "Bob", "Johnson", "bob@example.com", "0987654321", Some("456 High St, NY, USA, 001001"))
    )

    val action = for {
      existing <- employees.exists.result
      _ <- if (!existing) {
        for {
          inserted <- (employees returning employees.map(_.id)) ++= initialEmployees
          _ <- contracts ++= Seq(
            ContractsModel(None, inserted.head, "Permanent", LocalDate.parse("2023-01-01"), None, fullTime = true, Some(40)),
            ContractsModel(None, inserted(1), "Contract", LocalDate.parse("2024-05-01"), Some(LocalDate.parse("2025-05-01")), fullTime = false, Some(20))
          )
        } yield ()
      } else DBIO.successful(())
    } yield ()

    db.run(action.transactionally)
  }
}

