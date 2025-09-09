package repositories

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.sql.Timestamp

import models.EmailsModel
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

@Singleton
class EmailsRepository @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private class EmailsTable(tag: Tag) extends Table[EmailsModel](tag, "emails") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def employeeId = column[Option[Int]]("employee_id")
    def address = column[String]("address")
    def isActive = column[Boolean]("is_active")
    def createdAt = column[Timestamp]("created_at")
    def deactivatedAt = column[Option[Timestamp]]("deactivated_at")

    def * = (id.?, employeeId, address, isActive, createdAt, deactivatedAt) <> (EmailsModel.tupled, EmailsModel.unapply)
  }

  private val Emails = TableQuery[EmailsTable]

  def addressExists(address: String): Future[Boolean] =
    db.run(Emails.filter(_.address === address).exists.result)

  def findActiveByEmployeeId(employeeId: Int): Future[Option[EmailsModel]] =
    db.run(Emails.filter(e => e.employeeId === employeeId.bind && e.isActive).result.headOption)

  def listByEmployee(employeeId: Int): Future[Seq[EmailsModel]] =
    db.run(Emails.filter(_.employeeId === employeeId.bind).sortBy(_.createdAt.desc).result)

  def deactivateActive(employeeId: Int): Future[Int] =
    db.run(
      Emails.filter(e => e.employeeId === employeeId.bind && e.isActive)
        .map(e => (e.isActive, e.deactivatedAt))
        .update((false, Some(new Timestamp(System.currentTimeMillis()))))
    )

  def insert(employeeId: Int, address: String, active: Boolean = true): Future[EmailsModel] = {
    val now = new Timestamp(System.currentTimeMillis())
    val row = EmailsModel(None, Some(employeeId), address, active, now, None)
    val insertQ = (Emails returning Emails.map(_.id) into ((r, id) => r.copy(id = Some(id)))) += row
    db.run(insertQ)
  }
}

