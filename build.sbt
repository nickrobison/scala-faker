ThisBuild / scalaVersion := "3.3.7"

lazy val root = (project in file("."))
  .settings(
    name := "scala-faker",
    idePackagePrefix := Some("com.nickrobison.scalafaker")
  )
