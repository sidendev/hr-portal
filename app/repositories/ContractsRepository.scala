package repositories

import models.{ContractsModel, Tables}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{Future, ExecutionContext}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class ContractsRepository @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private val contracts = Tables.contracts

  def create(contract: ContractsModel): Future[Int] =
    db.run(contracts += contract)

  def listAll(): Future[Seq[ContractsModel]] =
    db.run(contracts.result)

  def findById(id: Int): Future[Option[ContractsModel]] =
    db.run(contracts.filter(_.id === id).result.headOption)

  def findByEmployeeId(employeeId: Int): Future[Seq[ContractsModel]] =
    db.run(contracts.filter(_.employeeId === employeeId).result)

  def deleteById(id: Int): Future[Int] =
    db.run(contracts.filter(_.id === id).delete)

  def update(id: Int, updated: ContractsModel): Future[Int] =
    db.run(contracts.filter(_.id === id).update(updated))
}

