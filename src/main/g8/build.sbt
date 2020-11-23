import Common._
import Dependencies._

lazy val root = (project in file("."))
  .commonSettings("$name$")
  .settings(
    libraryDependencies ++= Dependencies.compileDeps ++ Dependencies.testDeps,
    publishTravisSettings
  )
