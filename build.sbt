name := """hr-portal"""
organization := "com.hrportal"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.16"

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "6.0.2" % Test,
  "org.scalatestplus" %% "selenium-4-21" % "3.2.19.0" % Test,
  "com.h2database" % "h2" % "2.3.232" % Test,
  "com.mysql" % "mysql-connector-j" % "8.0.31",
  "com.typesafe.play" %% "play-slick" % "5.1.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0"
)
