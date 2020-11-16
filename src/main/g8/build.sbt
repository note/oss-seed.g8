import Common._

lazy val root = (project in file("."))
  .commonSettings("$name$", "$version$")
  .settings(
    libraryDependencies ++= Dependencies.compileDeps ++ Dependencies.testDeps
  )
