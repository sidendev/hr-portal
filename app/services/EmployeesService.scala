package services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import dtos.CreateEmployeeDto
import models.EmployeesModel
import repositories.EmployeesRepository
import repositories.ContractsRepository
import utils.ApiError
import validators.EmployeesValidator
import services.EmailsService
import java.time.{LocalDate, YearMonth}

@Singleton
class EmployeesService @Inject()(
  employeesRepo: EmployeesRepository,
  contractsRepo: ContractsRepository,
  emailsService: EmailsService
)(implicit ec: ExecutionContext) {

  def create(dto: CreateEmployeeDto): Future[Either[ApiError, EmployeesModel]] = {
    val validationErrors = EmployeesValidator.validateCreate(dto)
    if (validationErrors.nonEmpty)
      Future.successful(Left(ApiError.ValidationError(validationErrors)))
    else {
      emailsService.generateAddress(dto.firstName, dto.lastName).flatMap { addr =>
        val employee = EmployeesModel(
          firstName = dto.firstName,
          lastName = dto.lastName,
          email = addr,
          mobileNumber = dto.mobileNumber,
          address = dto.address
        )
        employeesRepo.create(employee).flatMap { _ =>
          employeesRepo.findByEmail(addr).flatMap {
            case Some(saved) =>
              emailsService.registerActive(saved.id.get, addr).map(_ => Right(saved))
            case None =>
              Future.successful(Left(ApiError.InternalServerError("Employee insert verification failed")))
          }
        }.recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }
      }
    }
  }

  def listAll(): Future[Either[ApiError, Seq[EmployeesModel]]] =
    employeesRepo.listAll()
      .map(Right(_))
      .recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }

  def findById(id: Int): Future[Either[ApiError, EmployeesModel]] =
    employeesRepo.findById(id)
      .map {
        case Some(e) => Right(e)
        case None => Left(ApiError.NotFound("Employee not found"))
      }
      .recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }

  def delete(id: Int): Future[Either[ApiError, Unit]] =
    employeesRepo.deleteById(id)
      .map {
        case 0 => Left(ApiError.NotFound("Employee not found"))
        case _ => Right(())
      }
      .recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }

  def update(id: Int, dto: CreateEmployeeDto): Future[Either[ApiError, EmployeesModel]] = {
    val validationErrors = EmployeesValidator.validateCreate(dto)
    if (validationErrors.nonEmpty)
      Future.successful(Left(ApiError.ValidationError(validationErrors)))
    else {
      employeesRepo.findById(id).flatMap {
        case None => Future.successful(Left(ApiError.NotFound("Employee not found")))
        case Some(existing) =>
          val nameChanged =
            existing.firstName != dto.firstName || existing.lastName != dto.lastName

          val emailFut: Future[String] =
            if (!nameChanged) Future.successful(existing.email)
            else emailsService.rotateActiveEmail(id, dto.firstName, dto.lastName)

          emailFut.flatMap { newAddr =>
            val updatedEmployee = EmployeesModel(
              id = Some(id),
              firstName = dto.firstName,
              lastName = dto.lastName,
              email = newAddr,
              mobileNumber = dto.mobileNumber,
              address = dto.address
            )
            employeesRepo.update(id, updatedEmployee)
              .map {
                case 0 => Left(ApiError.NotFound("Employee not found"))
                case _ => Right(updatedEmployee)
              }
              .recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }
          }
      }
    }
  }

  def search(
    contractType: Option[String],
    q: Option[String],
    expiring: Option[String],
    page: Option[Int],
    size: Option[Int]
  ): Future[Either[ApiError, Seq[EmployeesModel]]] = {
    val fullTimeOpt: Option[Boolean] = contractType.map(_.toLowerCase) match {
      case Some("full-time") | Some("fulltime") => Some(true)
      case Some("part-time") | Some("parttime") => Some(false)
      case Some(other) => return Future.successful(Left(ApiError.ValidationError(Map("contractType" -> s"Unsupported: $other"))))
      case None => None
    }

    // building name LIKE pattern
    val likePattern: Option[String] = q.map(_.trim).filter(_.nonEmpty).map(s => s"%${s.toLowerCase}%")

    // expiring current month
    import java.time.YearMonth
    val wantsExpiring = expiring.exists(_.equalsIgnoreCase("current-month"))
    val ym = YearMonth.now()
    val start = ym.atDay(1)
    val end   = ym.plusMonths(1).atDay(1)

    // pagination defaults - 20 per page
    val pageNum  = page.getOrElse(1)
    val pageSize = size.getOrElse(20)
    if (pageNum < 1)  return Future.successful(Left(ApiError.ValidationError(Map("page" -> "must be >= 1"))))
    if (pageSize < 1) return Future.successful(Left(ApiError.ValidationError(Map("size" -> "must be >= 1"))))
    if (pageSize > 100) return Future.successful(Left(ApiError.ValidationError(Map("size" -> "must be <= 100"))))

    val employeesF = employeesRepo.findByNameLike(likePattern)
    val contractsF = contractsRepo.listAll()

    (for {
      employees <- employeesF
      contracts <- contractsF
    } yield {
      // grouping contracts by employeeId
      val contractsByEmp: Map[Int, Seq[models.ContractsModel]] = contracts.groupBy(_.employeeId)

      val filtered = employees.filter { e =>
        val okType = fullTimeOpt match {
          case None => true
          case Some(ft) =>
            e.id.exists(id => contractsByEmp.getOrElse(id, Nil).exists(_.fullTime == ft))
        }
        val okExpiry =
          if (!wantsExpiring) true
          else e.id.exists { id =>
            contractsByEmp.getOrElse(id, Nil).exists { c =>
              c.endDate.exists(d => !d.isBefore(start) && d.isBefore(end))
            }
          }
        okType && okExpiry
      }

      // pagination
      val from = (pageNum - 1) * pageSize
      val paged =
        if (from >= filtered.length) Seq.empty
        else filtered.slice(from, math.min(from + pageSize, filtered.length))

      Right(paged)
    }).recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }
  }
}
