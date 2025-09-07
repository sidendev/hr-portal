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
  test("Homepage renders header and employees content") {
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
  test("Add a new employee, then add a Contract for them and see it in contracts list") {
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

  // TEST: Adding an employee and a new permanent contract for them
  test("Add a new employee, then add a permanent contract for them and see it in contracts list") {
    driver.get(frontendBase)

    clickByTest("add-employee")
    employeeDialog()

    val now = System.currentTimeMillis()
    val first = s"E2E$now"
    val last  = "Permy"
    val email = s"e2e$now@example.com"
    val fullName = s"$first $last"

    typeByTest("firstName", first)
    typeByTest("lastName", last)
    typeByTest("email", email)
    typeByTest("mobileNumber", "07123456789")
    typeByTest("address", "10 Downing St, London")

    clickByTest("save-employee")
    pause(250)
    wdWait.until(_ => employeesList().getText.contains(fullName))

    goToContractsTab()
    clickByTest("add-contract")
    contractDialog()

    // Select the employee
    selectEmployeeInContractDialog(fullName)

    // setting start date
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val start = LocalDate.now().plusDays(7)
    setDateByTest("start-date", start.format(fmt))

    clickByTest("save-contract")
    pause(400)

    // final check to verify employee is in list
    val row = contractRowForEmployee(fullName)
    val typeText = visibleIn(row, byTest("contract-type-text")).getText
    assert(typeText.toLowerCase.contains("permanent"), s"Expected contract type to include 'Permanent', got: $typeText")
  }

  // TEST: Frontend validation - cannot save employee without first name added
  test("Add employee validation: shows error when first name is missing and stays in dialog") {
    driver.get(frontendBase)

    clickByTest("add-employee")
    employeeDialog()

    // leaving firstName blank
    typeByTest("firstName", "")
    typeByTest("lastName", "Geller")
    typeByTest("email", "jo@example.com")
    typeByTest("mobileNumber", "07000000000")
    typeByTest("address", "123 Test St")

    // try to save
    clickByTest("save-employee")

    // check FE error to be visible
    val err = visible(byTest("error-firstName"))
    val msg = err.getText.trim
    assert(
      msg.equalsIgnoreCase("First name is required"),
      s"Expected 'First name is required', got: '$msg'"
    )

    assert(employeeDialog().isDisplayed)
  }

  // TEST: Edit an employee's first name and verify the change
  test("Add a new employee, then edit their first name and see the update in the list") {
    driver.get(frontendBase)

    clickByTest("add-employee")
    employeeDialog()

    val now = System.currentTimeMillis()
    val firstBefore = s"EditSrc$now"
    val firstAfter = s"Edited$now"
    val last = "Person"
    val email = s"editsrc$now@example.com"
    val fullBefore = s"$firstBefore $last"
    val fullAfter = s"$firstAfter $last"

    typeByTest("firstName", firstBefore)
    typeByTest("lastName", last)
    typeByTest("email", email)
    typeByTest("mobileNumber", "07000999000")
    typeByTest("address", "9 Edit Lane")

    clickByTest("save-employee")
    wdWait.until(_ => employeesList().getText.contains(fullBefore))

    // click on edit button for that employee
    val row = employeeRowForName(fullBefore)
    val editBtn = row.findElement(By.xpath(".//button[normalize-space(.)='Edit']"))
    editBtn.click()
    employeeDialog()

    // change first name and save
    typeByTest("firstName", firstAfter)
    clickByTest("save-employee")

    // check the list updated
    wdWait.until { _ =>
      val t = employeesList().getText
      t.contains(fullAfter) && !t.contains(fullBefore)
    }
  }

  // TEST: Add employee, add a permanent contract, then edit contract start to +14d and confirm
  test("Add employee and permanent contract, edit contract start date to + 14d") {
    driver.get(frontendBase)

    clickByTest("add-employee")
    employeeDialog()

    val now = System.currentTimeMillis()
    val first = s"ContractEdit$now"
    val last = "Person"
    val email = s"cedit$now@example.com"
    val fullName = s"$first $last"

    typeByTest("firstName", first)
    typeByTest("lastName", last)
    typeByTest("email", email)
    typeByTest("mobileNumber", "07000777777")
    typeByTest("address", "77 Contract Ave")
    clickByTest("save-employee")

    wdWait.until(_ => employeesList().getText.contains(fullName))

    goToContractsTab()
    clickByTest("add-contract")
    contractDialog()

    selectEmployeeInContractDialog(fullName)

    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val start = LocalDate.now().plusDays(7)
    setDateByTest("start-date", start.format(fmt))

    clickByTest("save-contract")
    pause(400)

    var row = contractRowForEmployee(fullName)
    wdWait.until(_ => row.getText.contains(start.format(fmt)))

    // edit the contract - change start date to + 14
    Try(row.findElement(byTest("contract-edit")).click())
      .getOrElse {
        // fallback
        row.findElement(By.xpath(".//button[normalize-space(.)='Edit']")).click()
      }

    contractDialog()

    val newStart = LocalDate.now().plusDays(14)
    setDateByTest("start-date", newStart.format(fmt))
    clickByTest("save-contract")
    pause(400)

    // checking the row shows the new start date
    row = contractRowForEmployee(fullName)
    wdWait.until(_ => row.getText.contains(newStart.format(fmt)))
    assert(
      !row.getText.contains(start.format(fmt)),
      s"Old start date ${start.format(fmt)} still present in row text: ${row.getText}"
    )
  }

  // TEST: Add a new employee, delete, and verify they are removed
  test("Add a new employee, confirm delete, and verify they are removed") {
    driver.get(frontendBase)

    // creating employee
    clickByTest("add-employee")
    employeeDialog()

    val now = System.currentTimeMillis()
    val first = s"Del$now"
    val last = "Target"
    val email = s"del$now@example.com"
    val full = s"$first $last"

    typeByTest("firstName", first)
    typeByTest("lastName", last)
    typeByTest("email", email)
    typeByTest("mobileNumber", "07000111111")
    typeByTest("address", "1 Delete Rd")
    clickByTest("save-employee")

    wdWait.until(_ => employeesList().getText.contains(full))

    val row = employeeRowForName(full)
    row.findElement(byTest("employee-remove")).click()

    visible(byTest("delete-confirmation-dialog"))
    clickByTest("confirm-delete")

    // confirm the employee is not on list
    wdWait.until(_ => !employeesList().getText.contains(full))
  }

}

