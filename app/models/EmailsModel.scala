package models

import java.sql.Timestamp

case class EmailsModel(
  id: Option[Long] = None,
  employeeId: Option[Int],
  address: String,
  isActive: Boolean = true,
  createdAt: Timestamp = new Timestamp(System.currentTimeMillis()),
  deactivatedAt: Option[Timestamp] = None
)
