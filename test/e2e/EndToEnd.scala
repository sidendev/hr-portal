package e2e

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll

import org.openqa.selenium._
import org.openqa.selenium.chrome._
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait, Select}

import java.time.{Duration, LocalDate}
import java.time.format.DateTimeFormatter

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.TestServer

import scala.jdk.CollectionConverters._
import scala.util.Try

final class EndToEnd
  extends AnyFunSuite
    with BeforeAndAfterAll
    with EndToEndHelpers {

  // configure this if changes:
  private val frontendBase = "http://localhost:5173"
  private val backendPort  = 9000

  private var app: Application = _
  private var server: TestServer = _

  protected var driver: WebDriver = _
  protected var wdWait: WebDriverWait = _

  override def beforeAll(): Unit = {
    val testCfg = com.typesafe.config.ConfigFactory.parseResources("application.test.conf")

    app = new GuiceApplicationBuilder()
      .configure(
        testCfg.entrySet().asScala.map(e => e.getKey -> e.getValue.unwrapped().asInstanceOf[AnyRef]).toMap
      )
      .build()

    server = TestServer(port = backendPort, application = app)
    server.start()

    val options = new ChromeOptions()
    options.addArguments("--window-size=1280,900")

    driver = new ChromeDriver(options)
    wdWait = new WebDriverWait(driver, Duration.ofSeconds(10))
  }

  override def afterAll(): Unit = {
    Try(driver.quit())
    Try(server.stop())
  }

  // TEST: Initial general test to check header, button and list are displayed
  test("Homepage renders header and Employees content") {
    driver.get(frontendBase)

    assert(visible(byTest("app-title")).isDisplayed)
    assert(clickable(byTest("add-employee")).isDisplayed)
    assert(employeesList().isDisplayed)
  }

  // TEST: Viewing the list of employees
  test("Employees list shows records (Alice, Bob)") {
    driver.get(frontendBase)
    val list = employeesList()

    // wait until at least 2 items exist
    wdWait.until(_ => list.findElements(By.cssSelector("[data-test='employee-item']")).size() >= 2)

    val textBlob = list.getText
    assert(textBlob.contains("Alice") && textBlob.contains("Smith"), "Expected Alice Smith in the list")
    assert(textBlob.contains("Bob") && textBlob.contains("Johnson"), "Expected Bob Johnson in the list")
  }

  // TEST: Adding a new employee
  test("Add a new employee and see it in the list") {
    driver.get(frontendBase)
    clickByTest("add-employee")
    employeeDialog()

    val now = System.currentTimeMillis()
    val first = s"Test$now"
    val last = "Employee"
    val email = s"test$now@example.com"

    typeByTest("firstName", first)
    typeByTest("lastName", last)
    typeByTest("email", email)
    typeByTest("mobileNumber", "07000111222")
    typeByTest("address", "10 Downing St, London")

    clickByTest("save-employee")
    pause(250)

    val list = employeesList()
    wdWait.until(_ => list.getText.contains(first) && list.getText.contains(last))
  }

  // TEST: Adding an employee and a new contract type to that employee
  test("Add a new employee, then add a Contract for them and see it in Contracts list") {
    driver.get(frontendBase)

    clickByTest("add-employee")
    employeeDialog()

    val now = System.currentTimeMillis()
    val first = s"E2E$now"
    val last = "Contractee"
    val email = s"e2e$now@example.com"
    val fullName = s"$first $last"

    typeByTest("firstName", first)
    typeByTest("lastName", last)
    typeByTest("email", email)
    typeByTest("mobileNumber", "07123456789")
    typeByTest("address", "221B Baker Street, London")

    clickByTest("save-employee")
    pause(250)
    wdWait.until(_ => employeesList().getText.contains(fullName))

    goToContractsTab()
    clickByTest("add-contract")
    contractDialog()

    // choose the employee from the shadcn select
    selectEmployeeInContractDialog(fullName)

    // setting contract type to Contract
    setContractTypeToContract()

    // dates: start today, end today + 7
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    setDateByTest("start-date", today.format(fmt))
    setDateByTest("end-date", today.plusDays(7).format(fmt))

    clickByTest("save-contract")
    pause(400)

    // check row shows Contract in the type text
    val row = contractRowForEmployee(fullName)
    val typeText = visibleIn(row, byTest("contract-type-text")).getText
    assert(typeText.toLowerCase.contains("contract"), s"Expected contract type to include 'Contract', got: $typeText")

  }





  // Form validation and error handling

  // Edit an employees details

  // Edit a contract

  // Delete an employee



}

