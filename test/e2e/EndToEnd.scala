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

  // use pause to check visually browser as actions taken in test
  private def pause(ms: Long): Unit = Thread.sleep(ms)

  override def beforeAll(): Unit = {
    // val testCfg = ConfigFactory.load("application.test.conf")
    val testCfg = com.typesafe.config.ConfigFactory.parseResources("application.test.conf")

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

  // Adding a new employee
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

  // Adding a new contract employee

  // Form validation and error handling

  // Edit an employees details

  // Edit a contract

  // Delete an employee



}

