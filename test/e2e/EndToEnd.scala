package e2e

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll
import org.openqa.selenium._
import org.openqa.selenium.chrome._
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import java.time.Duration

import com.typesafe.config.ConfigFactory
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.TestServer

import scala.jdk.CollectionConverters._
import scala.util.Try

final class EndToEnd extends AnyFunSuite with BeforeAndAfterAll {

  // configure this if changes:
  private val frontendBase = "http://localhost:5173"
  private val backendPort  = 9000

  private var app: Application = _
  private var server: TestServer = _
  private var driver: WebDriver = _
  private var wdWait: WebDriverWait = _

  override def beforeAll(): Unit = {
    val testCfg = ConfigFactory.load("application.test.conf")

    app = new GuiceApplicationBuilder()
      .configure(
        testCfg.entrySet().asScala.map(e => e.getKey -> e.getValue.unwrapped().asInstanceOf[AnyRef]).toMap
      )
      .build()

    server = TestServer(port = backendPort, application = app)
    server.start()

    val options = new ChromeOptions()
    // options.addArguments("--headless=new")
    options.addArguments("--window-size=1280,900")

    driver = new ChromeDriver(options)
    wdWait = new WebDriverWait(driver, Duration.ofSeconds(10))
  }

  override def afterAll(): Unit = {
    Try(driver.quit())
    Try(server.stop())
  }

  // Initial test to check
  test("Homepage renders header and Employees content") {
    driver.get(frontendBase)

    val h1 = wdWait.until(
      ExpectedConditions.presenceOfElementLocated(By.xpath("//h1[contains(normalize-space(.), 'HR Portal')]"))
    )
    assert(h1.isDisplayed)

    val addEmployeeBtn = wdWait.until(
      ExpectedConditions.presenceOfElementLocated(By.xpath("//button[normalize-space(.)='Add employee']"))
    )
    assert(addEmployeeBtn.isDisplayed)

    val lists = driver.findElements(By.cssSelector("[class*='divide-y']")).asScala
    assert(lists.nonEmpty, "Expected an employees list container on the page")
  }

  // Viewing the list of employees

  // Adding a new permanent employee

  // Adding a new contract employee

  // Form validation and error handling

  // Edit an employees details

  // Edit a contract

  // Delete an employee



}

