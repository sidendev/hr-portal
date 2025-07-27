package models

import java.time.LocalDate

case class ContractsModel(
  id: Option[Int] = None,
  employeeId: Int,
  contractType: String,
  startDate: LocalDate,
  endDate: Option[LocalDate],
  fullTime: Boolean,
  hoursPerWeek: Option[Int]
)
