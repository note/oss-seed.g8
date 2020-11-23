import Common._
import Dependencies._

lazy val root = (project in file("."))
  .commonSettings("$projectName$")
  .settings(
    libraryDependencies ++= Dependencies.compileDeps ++ Dependencies.testDeps,
    publishTravisSettings
  )
