package repositories

import models.{EmployeesModel, Tables}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{Future, ExecutionContext}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class EmployeesRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private val employees = Tables.employees

  def create(employee: EmployeesModel): Future[Int] =
    db.run(employees += employee)

  def listAll(): Future[Seq[EmployeesModel]] =
    db.run(employees.result)

  def findById(id: Int): Future[Option[EmployeesModel]] =
    db.run(employees.filter(_.id === id).result.headOption)

  def deleteById(id: Int): Future[Int] =
    db.run(employees.filter(_.id === id).delete)

  def update(id: Int, updated: EmployeesModel): Future[Int] =
    db.run(employees.filter(_.id === id).update(updated))

  def setActiveEmail(id: Int, emailId: Long, address: String): Future[Int] = {
    db.run(
      Tables.employees
        .filter(_.id === id.bind)
        .map(e => (e.emailId, e.email))
        .update((Some(emailId), address))
    )
  }

  def findByEmail(address: String): Future[Option[EmployeesModel]] =
    db.run(Tables.employees.filter(_.email === address).result.headOption)

  // filtering employees by LIKE on first or last name
  def findByNameLike(likeLowerPattern: Option[String]): Future[Seq[EmployeesModel]] = {
    val base = Tables.employees
    val query = likeLowerPattern match {
      case Some(p) =>
        base.filter(e =>
          e.firstName.toLowerCase.like(p.bind) ||
            e.lastName.toLowerCase.like(p.bind)
        )
      case None =>
        base
    }
    db.run(query.result)
  }

}
