package utils

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import play.api.inject.ApplicationLifecycle

@Singleton
class Startup @Inject()(
  dataSeeder: DataSeeder,
  lifecycle: ApplicationLifecycle
)(implicit ec: ExecutionContext) {

  println("Running app startup and seeding data")

  dataSeeder.seed().map { _ =>
    println("Database seeding complete")
  }.recover {
    case ex => println(s"Database seeding failed: ${ex.getMessage}")
  }

  lifecycle.addStopHook { () =>
    println("Application shutting down...")
    scala.concurrent.Future.successful(())
  }
}

