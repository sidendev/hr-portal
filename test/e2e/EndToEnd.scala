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

  private def visible(by: By): WebElement =
    wdWait.until(ExpectedConditions.visibilityOfElementLocated(by))

  private def clickButtonByText(txt: String): WebElement =
    clickable(By.xpath(s"//button[normalize-space(.)='$txt']"))

  private def fillByLabel(labelText: String, value: String): Unit = {
    val label = visible(
      By.xpath(s"//label[translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='${labelText.toLowerCase}']")
    )
    val input: WebElement =
      Option(label.getAttribute("for"))
        .flatMap(id => Try(driver.findElement(By.id(id))).toOption)
        .getOrElse {
          val cands = label.findElements(By.xpath("(following::input | following::textarea)[1]"))
          if (cands.isEmpty) throw new NoSuchElementException(s"No input for label '$labelText'")
          cands.get(0)
        }
    input.clear()
    input.sendKeys(value)
  }

  private def setDateByName(name: String, yyyyMmDd: String): Unit = {
    val input = clickable(By.cssSelector(s"input[name='$name']"))
    // first try by typing
    input.clear()
    input.sendKeys(yyyyMmDd)
    pause(120)
    val current = Option(input.getAttribute("value")).getOrElse("")
    if (current != yyyyMmDd) {
      val js = driver.asInstanceOf[JavascriptExecutor]
      js.executeScript(
        "arguments[0].value = arguments[1];" +
          "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
          "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
        input, yyyyMmDd
      )
      pause(120)
    }
  }

  // using shadcn select for Employee - data-slot based
  private def selectEmployeeFromDropdown(fullName: String): Unit = {
    clickable(By.cssSelector("[data-slot='select-trigger']")).click()
    visible(By.cssSelector("[data-slot='select-content']"))
    clickable(By.xpath(s"//div[@data-slot='select-item' and contains(normalize-space(.), '$fullName')]")).click()
  }

  // set contractType = Contract
  private def setContractTypeToContract(): Unit = {
    val setViaSelect = Try {
      val selectEl = visible(By.cssSelector("select[name='contractType']"))
      new Select(selectEl).selectByVisibleText("Contract")
      true
    }.getOrElse(false)

    if (!setViaSelect) {
      // fallback
      clickHard(clickable(By.xpath("//label[normalize-space(.)='Contract']")))
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

  private def goToContractsTab(): Unit = {
    val selectors: Seq[By] = Seq(
      By.cssSelector("button[value='contracts']"),
      By.cssSelector("[role='tab'][data-value='contracts']"),
      By.xpath("//button[@role='tab' and contains(normalize-space(.), 'Contracts')]"),
      By.xpath("//*[(@role='tab' or self::button or self::a) and normalize-space(.)='Contracts']")
    )
    val tab = selectors.iterator.flatMap(sel => Try(clickable(sel)).toOption).toSeq.headOption
      .getOrElse(throw new NoSuchElementException("Could not find any Contracts tab selector"))

    clickHard(tab)
    pause(150)
  }

  private def findDialog(): WebElement =
    visible(By.cssSelector("div[role='dialog']"))

  private def selectEmployeeFromDropdownInDialog(fullName: String): Unit = {
    val dialog = findDialog()
    val trigger = dialog.findElement(By.cssSelector("[data-slot='select-trigger']"))
    clickHard(trigger)
    visible(By.cssSelector("[data-slot='select-content']"))
    val option = clickable(By.xpath(s"//div[@data-slot='select-item' and contains(normalize-space(.), '$fullName')]"))
    clickHard(option)
  }

  // to find the visible list container using tailwind list divider
  private def visibleDivideY(): WebElement = {
    val lists = driver.findElements(By.cssSelector("[class*='divide-y']")).asScala
    lists.find(_.isDisplayed).getOrElse(visible(By.cssSelector("[class*='divide-y']")))
  }


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

  // TEST: Viewing the list of employees
  test("Employees list shows records (Alice, Bob)") {
    driver.get(frontendBase)

    val listContainer = wdWait.until(
      ExpectedConditions.presenceOfElementLocated(By.cssSelector("[class*='divide-y']"))
    )

    wdWait.until { _ =>
      listContainer.findElements(By.tagName("li")).size() >= 2
    }

    val items = listContainer.findElements(By.tagName("li")).asScala.map(_.getText).mkString(" | ")

    assert(items.contains("Alice") && items.contains("Smith"), "Expected Alice Smith in the list")
    assert(items.contains("Bob")   && items.contains("Johnson"), "Expected Bob Johnson in the list")

  }

  // TEST: Adding a new employee
  test("Add a new employee and see it in the list") {
    driver.get(frontendBase)

    // Open the dialog component
    val addEmployeeBtn = wdWait.until(
      ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space(.)='Add employee']"))
    )
    addEmployeeBtn.click()

    def fillByLabel(labelText: String, value: String): Unit = {
      val label = wdWait.until(
        ExpectedConditions.presenceOfElementLocated(
          By.xpath(s"//label[translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = '${labelText.toLowerCase}']")
        )
      )
      val forAttr = Option(label.getAttribute("for"))
      val input: WebElement =
        forAttr.flatMap { id =>
          val byId = By.id(id)
          Try(driver.findElement(byId)).toOption
        }.getOrElse {
          val candidates = label.findElements(By.xpath("(following::input | following::textarea)[1]"))
          if (candidates.isEmpty) throw new NoSuchElementException(s"No input found for label '$labelText'")
          candidates.get(0)
        }
      input.clear()
      input.sendKeys(value)
    }

    // adding in test employee data
    val now = System.currentTimeMillis()
    val first = s"Test$now"
    val last  = "Employee"
    val email = s"test$now@example.com"
    val mobile = "07000111222"
    val address = "10 Downing St, London"

    // filling in the form
    fillByLabel("First name", first)
    fillByLabel("Last name",  last)
    fillByLabel("Email",      email)
    fillByLabel("Mobile number", mobile)
    fillByLabel("Address",    address)

    // saving the new employee
    val saveBtn = wdWait.until(
      ExpectedConditions.elementToBeClickable(
        By.xpath("//button[normalize-space(.)='Save' or @type='submit']")
      )
    )
    saveBtn.click()

    // pause for visual check
    pause(1000)

    // after save the dialog should close and the list should contain the new employee details
    val listContainer = wdWait.until(
      ExpectedConditions.presenceOfElementLocated(By.cssSelector("[class*='divide-y']"))
    )

    // pause for visual check
    pause(1000)

    // waiting till a li contains the new name OR email
    wdWait.until { _ =>
      val lis = listContainer.findElements(By.tagName("li")).asScala
      lis.exists(li => li.getText.contains(first) && li.getText.contains(last)) ||
        lis.exists(li => li.getText.contains(email))
    }

    // pause for visual check
    pause(1000)

    // final check
    val combined = listContainer.findElements(By.tagName("li")).asScala.map(_.getText).mkString(" | ")
    assert(combined.contains(first) && combined.contains(last), s"Expected new employee $first $last in list, got: $combined")
  }

  // TEST: Adding an employee and a new contract type to that employee
  test("Add a new employee, then add a Contract for them and see it in Contracts list") {
    driver.get(frontendBase)
    pause(400)

    val now = System.currentTimeMillis()
    val first = s"E2E$now"
    val last  = "Contractee"
    val email = s"e2e$now@example.com"
    val mobile = "07123456789"
    val address = "221B Baker Street, London"
    val fullName = s"$first $last"

    clickButtonByText("Add employee").click()
    pause(200)

    fillByLabel("First name", first)
    fillByLabel("Last name", last)
    fillByLabel("Email", email)
    fillByLabel("Mobile number", mobile)
    fillByLabel("Address", address)

    clickable(By.xpath("//button[normalize-space(.)='Save' or @type='submit']")).click()
    pause(500)

    val employeesList = visible(By.cssSelector("[class*='divide-y']"))
    wdWait.until { _ => employeesList.getText.contains(first) && employeesList.getText.contains(last) }
    pause(200)

    goToContractsTab()
    pause(250)

    clickButtonByText("Add contract").click()
    visible(By.cssSelector("div[role='dialog']"))
    pause(200)

    selectEmployeeFromDropdownInDialog(fullName)
    pause(150)

    // set contract type = Contract
    setContractTypeToContract()
    pause(120)

    // setting dates: start = today, end = today + 7
    val today = LocalDate.now()
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    setDateByName("startDate", today.format(fmt))
    setDateByName("endDate", today.plusDays(7).format(fmt))

    // saving
    clickable(By.xpath("//button[normalize-space(.)='Save' or @type='submit']")).click()
    pause(600)

    // verify contract is listed (name + "Contract" is somewhere in the row)
    val contractsList = visibleDivideY()
    wdWait.until { _ =>
      val t = contractsList.getText
      t.contains(fullName) && (t.contains("Contract") || t.contains("contract"))
    }
  }





  // Form validation and error handling

  // Edit an employees details

  // Edit a contract

  // Delete an employee



}

