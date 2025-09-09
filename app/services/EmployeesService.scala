package services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import dtos.CreateEmployeeDto
import models.EmployeesModel
import repositories.EmployeesRepository
import utils.ApiError
import validators.EmployeesValidator
import services.EmailsService

@Singleton
class EmployeesService @Inject()(
  employeesRepo: EmployeesRepository,
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
}
