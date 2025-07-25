name := """hr-portal"""
organization := "com.hrportal"
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.16"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "6.0.2" % Test
libraryDependencies += "com.mysql" % "mysql-connector-j" % "8.0.31"
libraryDependencies += "com.typesafe.play" %% "play-slick" % "5.1.0"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0"
