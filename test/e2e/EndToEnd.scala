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

final class EndToEnd extends AnyFunSuite with BeforeAndAfterAll {

  // configure this if changes:
  private val frontendBase = "http://localhost:5173"
  private val backendPort  = 9000

  private var app: Application = _
  private var server: TestServer = _
  private var driver: WebDriver = _
  private var wdWait: WebDriverWait = _

  // ---------------------------------------------------------------------------
  // HELPERS:

  // use pause to check visually in browser and to wait for any animations/transitions to complete
  private def pause(ms: Long): Unit = Thread.sleep(ms)

  private def clickable(by: By): WebElement =
    wdWait.until(ExpectedConditions.elementToBeClickable(by))

  private def byTest(id: String): By =
    By.cssSelector(s"[data-test='$id']")

  private def visible(by: By): WebElement =
    wdWait.until(ExpectedConditions.visibilityOfElementLocated(by))

  private def visibleIn(el: WebElement, by: By): WebElement = {
    wdWait.until(_ => {
      val found = el.findElements(by).asScala.find(_.isDisplayed)
      found.orNull
    })
  }

  private def clickByTest(id: String): Unit =
    clickable(byTest(id)).click()

  private def typeByTest(id: String, value: String): Unit = {
    val el = clickable(byTest(id))
    el.clear()
    el.sendKeys(value)
  }

  private def setDateByTest(id: String, yyyyMmDd: String): Unit = {
    val input = clickable(byTest(id))
    input.clear()
    input.sendKeys(yyyyMmDd)
    pause(80)
    val current = Option(input.getAttribute("value")).getOrElse("")
    if (current != yyyyMmDd) {
      val js = driver.asInstanceOf[JavascriptExecutor]
      js.executeScript(
        "arguments[0].value = arguments[1];" +
          "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
          "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
        input, yyyyMmDd
      )
    }
  }

  // try to click, and if something is overlaying it, click via JS.
  private def clickHard(el: WebElement): Unit = {
    try el.click()
    catch {
      case _: ElementClickInterceptedException =>
        val js = driver.asInstanceOf[JavascriptExecutor]
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", el)
        js.executeScript("arguments[0].click();", el)
    }
  }

  // Tabs
  private def goToEmployeesTab(): Unit = clickByTest("tab-employees")
  private def goToContractsTab(): Unit = clickByTest("tab-contracts")

  // Lists
  private def employeesList(): WebElement = visible(byTest("employees-list"))
  private def contractsList(): WebElement = visible(byTest("contracts-list"))

  // Dialogs
  private def employeeDialog(): WebElement = visible(byTest("employee-dialog"))
  private def contractDialog(): WebElement = visible(byTest("contract-dialog"))

  private def selectEmployeeInContractDialog(fullName: String): Unit = {
    val dlg = contractDialog()
    val trigger = visibleIn(dlg, byTest("employee-select-trigger"))
    clickHard(trigger)
    visible(byTest("employee-select-content"))
    val exact = By.cssSelector(s"""[data-test='employee-option'][data-value="$fullName"]""")
    val opt = Try(clickable(exact)).getOrElse {
      clickable(By.xpath(s"//*[@data-test='employee-option' and contains(@data-value, ${xpathQuote(fullName)})]"))
    }
    clickHard(opt)
  }

  private def setContractTypeToContract(): Unit = {
    val dlg = contractDialog()
    val sel = visibleIn(dlg, byTest("contract-type"))
    new Select(sel).selectByVisibleText("Contract")
  }

  private def contractRowForEmployee(fullName: String): WebElement = {
    val list = contractsList()
    wdWait.until(_ => {
      val rows = list.findElements(By.cssSelector("[data-test='contract-item']")).asScala
      rows.find { li =>
        Try(li.findElement(byTest("contract-employee-name")).getText.contains(fullName)).getOrElse(false)
      }.orNull
    })
  }

  // XPATH safe quote utility - handles strings containing quotes
  private def xpathQuote(s: String): String =
    if (!s.contains("'")) s"'$s'"
    else if (!s.contains("\"")) s""""$s""""
    else {
      val parts = s.split("'").map(p => s"'$p'")
      ("concat(" + parts.mkString(", \"'\", ") + ")")
    }

  private def findDialog(): WebElement =
    visible(By.cssSelector("div[role='dialog']"))

  // ---------------------------------------------------------------------------

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

