import mill._
import $ivy.`com.lihaoyi::mill-contrib-playlib:`,  mill.playlib._

object hrportal extends RootModule with PlayModule {

  def scalaVersion = "2.13.16"
  def playVersion = "2.9.8"
  def twirlVersion = "1.6.10"

  object test extends PlayTests
}
