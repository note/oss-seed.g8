import com.softwaremill.Publish.ossPublishSettings
import com.softwaremill.SbtSoftwareMillCommon.autoImport.commonSmlBuildSettings
import com.typesafe.sbt.SbtPgp.autoImportImpl.PgpKeys
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys.{developers, name, organization, scalaVersion, testFrameworks, version}
import sbt.{Project, TestFramework}
import xerial.sbt.Sonatype.autoImport.{sonatypeProfileName, sonatypeProjectHosting}
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport.releasePublishArtifactsAction
import xerial.sbt.Sonatype.GitHubHosting

object Common {
  implicit class ProjectFrom(project: Project) {
    def commonSettings(nameArg: String): Project = project.settings(
      name := nameArg,
      organization := "$organization$",

      scalaVersion := "2.13.3",
      scalafmtOnCompile := true,
      releasePublishArtifactsAction := PgpKeys.publishSigned.value,

      commonSmlBuildSettings,
      scalacOptions += "-Ymacro-annotations",
      testFrameworks += new TestFramework("munit.Framework"),
      ossPublishSettings ++ Seq(
        sonatypeProfileName := "$organization$",
        organizationHomepage := Some(url("$organizationHomepage$")),
        homepage := Some(url("$projectHomepage$")),
        sonatypeProjectHosting := Some(
          GitHubHosting("$githubLogin$", name.value, "$githubEmail$")
        ),
        licenses := Seq("$licenseName$" -> url("$licenseUrl$")),
        developers := List(
          Developer(
            id = "$githubLogin$",
            name = "$developerName$",
            email = "$githubEmail$",
            url = new URL("$githubDeveloperUrl$")
          )
        )
      )
    )
  }
}
