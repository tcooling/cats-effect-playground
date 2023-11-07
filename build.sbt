import scala.sys.process._

ThisBuild / scalaVersion := "3.3.1"
ThisBuild / version := "git rev-parse --short HEAD".!!.trim
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
Global / onChangedBuildSource := ReloadOnSourceChanges
Global / excludeLintKeys += mainClass

lazy val projects: Seq[ProjectReference] = Seq(catsEffectPlayground)

val root = Project(id = "cats-effect-playground", base = file("."))
  .aggregate(projects: _*)
  .settings(Aliases.root)

lazy val catsEffectPlayground = Project("playground", file("playground"))
  .settings(Dependencies.catsEffectPlayground)
  .settings(CompilerOptions.ScalaCompileOptions)
