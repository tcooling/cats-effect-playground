import sbt.{Def, _}

object Aliases {

  lazy val root: Seq[Def.Setting[State => State]] = scalaFmtAliases ++ scalaFixAliases

  private lazy val scalaFixAliases =
    addCommandAlias(
      "checkFix",
      "Compile / scalafix --check; Test / scalafix --check;"
    ) ++
      addCommandAlias(
        "runFix",
        "Compile / scalafix; Test / scalafix;"
      )

  private lazy val scalaFmtAliases =
    addCommandAlias(
      "checkFmt",
      "scalafmtCheckAll; scalafmtSbtCheck"
    ) ++
      addCommandAlias(
        "runFmt",
        "scalafmtAll; scalafmtSbt"
      )

}
