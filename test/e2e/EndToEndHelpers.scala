package e2e

import org.openqa.selenium._
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait, Select}
import scala.jdk.CollectionConverters._
import scala.util.Try

trait EndToEndHelpers {

  protected def driver: WebDriver
  protected def wdWait: WebDriverWait

  // use pause to check visually in browser and to wait for any animations/transitions to complete
  protected def pause(ms: Long): Unit = Thread.sleep(ms)

  protected def clickable(by: By): WebElement =
    wdWait.until(ExpectedConditions.elementToBeClickable(by))

  protected def byTest(id: String): By =
    By.cssSelector(s"[data-test='$id']")

  protected def visible(by: By): WebElement =
    wdWait.until(ExpectedConditions.visibilityOfElementLocated(by))

  protected def visibleIn(el: WebElement, by: By): WebElement = {
    wdWait.until(_ => {
      val found = el.findElements(by).asScala.find(_.isDisplayed)
      found.orNull
    })
  }

  protected def clickByTest(id: String): Unit =
    clickable(byTest(id)).click()

  protected def typeByTest(id: String, value: String): Unit = {
    val el = clickable(byTest(id))
    el.clear()
    el.sendKeys(value)
  }

  protected def setDateByTest(id: String, yyyyMmDd: String): Unit = {
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
  protected def clickHard(el: WebElement): Unit = {
    try el.click()
    catch {
      case _: ElementClickInterceptedException =>
        val js = driver.asInstanceOf[JavascriptExecutor]
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", el)
        js.executeScript("arguments[0].click();", el)
    }
  }

  // Tabs
  protected def goToEmployeesTab(): Unit = clickByTest("tab-employees")
  protected def goToContractsTab(): Unit = clickByTest("tab-contracts")

  // Lists
  protected def employeesList(): WebElement = visible(byTest("employees-list"))
  protected def contractsList(): WebElement = visible(byTest("contracts-list"))

  // Dialogs
  protected def employeeDialog(): WebElement = visible(byTest("employee-dialog"))
  protected def contractDialog(): WebElement = visible(byTest("contract-dialog"))

  protected def selectEmployeeInContractDialog(fullName: String): Unit = {
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

  protected def setContractTypeToContract(): Unit = {
    val dlg = contractDialog()
    val sel = visibleIn(dlg, byTest("contract-type"))
    new Select(sel).selectByVisibleText("Contract")
  }

  // find the <li data-test="contract-item"> for a given employee
  protected def contractRowForEmployee(fullName: String): WebElement = {
    val list = contractsList()
    wdWait.until(_ => {
      val rows = list.findElements(By.cssSelector("[data-test='contract-item']")).asScala
      rows.find { li =>
        Try(li.findElement(byTest("contract-employee-name")).getText.contains(fullName)).getOrElse(false)
      }.orNull
    })
  }

  // XPATH safe quote utility - handles strings containing quotes
  protected def xpathQuote(s: String): String =
    if (!s.contains("'")) s"'$s'"
    else if (!s.contains("\"")) s""""$s""""
    else {
      val parts = s.split("'").map(p => s"'$p'")
      ("concat(" + parts.mkString(", \"'\", ") + ")")
    }

  protected def findDialog(): WebElement =
    visible(By.cssSelector("div[role='dialog']"))
}
