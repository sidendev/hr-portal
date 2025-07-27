package models

case class EmployeesModel (
  id: Option[Int] = None,
  firstName: String,
  lastName: String,
  email: String,
  mobileNumber: String,
  address: Option[String]
)
