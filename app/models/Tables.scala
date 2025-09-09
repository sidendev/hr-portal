package models

import slick.jdbc.MySQLProfile.api._
import java.time.LocalDate
import java.sql.Timestamp

class Emails(tag: Tag) extends Table[EmailsModel](tag, "emails") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def employeeId = column[Option[Int]]("employee_id")
  def address = column[String]("address")
  def isActive = column[Boolean]("is_active")
  def createdAt = column[Timestamp]("created_at")
  def deactivatedAt = column[Option[Timestamp]]("deactivated_at")

  def employeeFk = foreignKey(
    "fk_emails_employee",
    employeeId,
    TableQuery[Employees]
  )(_.id.?, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.SetNull)

  def * =
    (id.?, employeeId, address, isActive, createdAt, deactivatedAt)
      .<>(EmailsModel.tupled, EmailsModel.unapply)
}

class Employees(tag: Tag) extends Table[EmployeesModel](tag, "employees") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def email = column[String]("email", O.Unique)
  def mobileNumber = column[String]("mobile_number")
  def address = column[Option[String]]("address")
  def emailId = column[Option[Long]]("email_id")

  def activeEmailFk = foreignKey(
    "fk_employees_email",
    emailId,
    TableQuery[Emails]
  )(_.id.?, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.SetNull)

  def * =
    (id.?, firstName, lastName, email, mobileNumber, address)
      .<>(EmployeesModel.tupled, EmployeesModel.unapply)
}

class Contracts(tag: Tag) extends Table[ContractsModel](tag, "contracts") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def employeeId = column[Int]("employee_id")
  def contractType = column[String]("contract_type")
  def startDate = column[LocalDate]("start_date")
  def endDate = column[Option[LocalDate]]("end_date")
  def fullTime = column[Boolean]("full_time")
  def hoursPerWeek = column[Option[Int]]("hours_per_week")

  def employeeFk = foreignKey("fk_employee", employeeId, TableQuery[Employees])(_.id, onDelete = ForeignKeyAction.Cascade)

  def * = (id.?, employeeId, contractType, startDate, endDate, fullTime, hoursPerWeek) <> (ContractsModel.tupled, ContractsModel.unapply)
}

object Tables {
  val emails = TableQuery[Emails]
  val employees = TableQuery[Employees]
  val contracts = TableQuery[Contracts]
}
