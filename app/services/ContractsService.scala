package services

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.{LocalDate, YearMonth}

import dtos.{CreateContractDto, UpdateContractDto}
import models.{ContractsModel, EmployeesModel}
import repositories.{ContractsRepository, EmployeesRepository}
import utils.ApiError
import validators.ContractsValidator

@Singleton
class ContractsService @Inject()(
  contractsRepo: ContractsRepository,
  employeesRepo: EmployeesRepository
)(implicit ec: ExecutionContext) {

  def create(dto: CreateContractDto): Future[Either[ApiError, ContractsModel]] = {
    val validationErrors = ContractsValidator.validateCreate(dto)
    if (validationErrors.nonEmpty)
      Future.successful(Left(ApiError.ValidationError(validationErrors)))
    else {
      val contract = ContractsModel(
        employeeId = dto.employeeId,
        contractType = dto.contractType,
        startDate = dto.startDate,
        endDate = dto.endDate,
        fullTime = dto.fullTime,
        hoursPerWeek = dto.hoursPerWeek
      )
      contractsRepo.create(contract)
        .map(_ => Right(contract))
        .recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }
    }
  }

  def listAll(): Future[Either[ApiError, Seq[ContractsModel]]] =
    contractsRepo.listAll()
      .map(Right(_))
      .recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }
      
  def search(
    contractType: Option[String],
    q: Option[String],
    expiring: Option[String],
    page: Option[Int],
    size: Option[Int]
  ): Future[Either[ApiError, Seq[ContractsModel]]] = {
    val pageNum = page.getOrElse(1)
    val pageSize = size.getOrElse(20)
    
    if (pageNum < 1) return Future.successful(
      Left(ApiError.ValidationError(Map("page" -> "must be >= 1")))
    )
    if (pageSize < 1) return Future.successful(
      Left(ApiError.ValidationError(Map("size" -> "must be >= 1")))
    )
    if (pageSize > 100) return Future.successful(
      Left(ApiError.ValidationError(Map("size" -> "must be <= 100")))
    )

    contractsRepo.listAll().map { contracts =>
      val from = (pageNum - 1) * pageSize
      val paged = 
        if (from >= contracts.length) Seq.empty
        else contracts.slice(from, math.min(from + pageSize, contracts.length))
      Right(paged)
    }.recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }
  }

  def findById(id: Int): Future[Either[ApiError, ContractsModel]] =
    contractsRepo.findById(id)
      .map {
        case Some(c) => Right(c)
        case None => Left(ApiError.NotFound("Contract not found"))
      }
      .recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }

  def delete(id: Int): Future[Either[ApiError, Unit]] =
    contractsRepo.deleteById(id)
      .map {
        case 0 => Left(ApiError.NotFound("Contract not found"))
        case _ => Right(())
      }
      .recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }

  def update(id: Int, dto: UpdateContractDto): Future[Either[ApiError, ContractsModel]] = {
    val validationErrors = ContractsValidator.validateUpdate(dto)
    if (validationErrors.nonEmpty)
      Future.successful(Left(ApiError.ValidationError(validationErrors)))
    else {
      contractsRepo.findById(id).flatMap {
        case None => Future.successful(Left(ApiError.NotFound("Contract not found")))
        case Some(existing) =>
          val merged = existing.copy(
            contractType = dto.contractType.getOrElse(existing.contractType),
            startDate = dto.startDate.getOrElse(existing.startDate),
            endDate = dto.endDate.orElse(existing.endDate),
            fullTime = dto.fullTime.getOrElse(existing.fullTime),
            hoursPerWeek = dto.hoursPerWeek.orElse(existing.hoursPerWeek)
          )
          contractsRepo.update(id, merged)
            .map(_ => Right(merged))
            .recover { case ex => Left(ApiError.InternalServerError(ex.getMessage)) }
      }
    }
  }
}

