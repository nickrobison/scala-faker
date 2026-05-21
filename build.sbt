ThisBuild / scalaVersion := "3.3.7"
ThisBuild / crossScalaVersions := Seq("3.3.7", "2.13.16")

lazy val root = (project in file("."))
  .settings(
    name := "scala-faker",
    idePackagePrefix := Some("com.nickrobison.scalafaker"),
    libraryDependencies ++= Seq(
      "net.datafaker" % "datafaker" % "2.5.4",
      "org.scalacheck" %% "scalacheck" % "1.19.0",
      "org.yaml" % "snakeyaml" % "2.6"
    )
  )
