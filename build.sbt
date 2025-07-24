name := """hr-portal"""
organization := "com.hrportal"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.16"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "6.0.2" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.hrportal.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.hrportal.binders._"
