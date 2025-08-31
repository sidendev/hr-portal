package utils

import slick.jdbc.JdbcProfile
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Configuration, Logger}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import models._
import models.Tables._
import java.time.LocalDate

@Singleton
class DataSeeder @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  config: Configuration
)(implicit ec: ExecutionContext) {

  private val log = Logger(this.getClass)

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private val shouldSeed: Boolean =
    config.getOptional[Boolean]("app.seed").getOrElse(true)

  def seed(): Future[Unit] = {
    if (!shouldSeed) {
      log.info("DataSeeder: skipping seeding (app.seed = false)")
      Future.unit
    } else {
      log.info("DataSeeder: seeding initial data if empty...")

      val initialEmployees = Seq(
        EmployeesModel(None, "Alice", "Smith", "alice@example.com", "1234567890", Some("123 Main St, NY, USA, 001001")),
        EmployeesModel(None, "Bob",   "Johnson", "bob@example.com",   "0987654321", Some("456 High St, NY, USA, 001001"))
      )

      val action: DBIO[Unit] = for {
        existing <- employees.exists.result
        _ <- if (!existing) {
          for {
            insertedIds <- (employees returning employees.map(_.id)) ++= initialEmployees
            _ <- contracts ++= Seq(
              ContractsModel(None, insertedIds.head, "Permanent", LocalDate.parse("2023-01-01"), None, fullTime = true,  Some(40)),
              ContractsModel(None, insertedIds(1),   "Contract",  LocalDate.parse("2024-05-01"), Some(LocalDate.parse("2025-05-01")), fullTime = false, Some(20))
            )
          } yield ()
        } else DBIO.successful(())
      } yield ()

      db.run(action.transactionally).map { _ =>
        log.info("DataSeeder: seeding complete (or data already present).")
      }.recover { case ex =>
        log.error("DataSeeder: seeding failed", ex)
        throw ex
      }
    }
  }
}
