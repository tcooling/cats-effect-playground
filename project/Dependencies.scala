import sbt.Keys.libraryDependencies
import sbt.*

object Dependencies {

  object Versions {
    val ScalaTest     = "3.2.17"
    val ScalaTestPlus = "3.2.11.0"
    val CatsEffect    = "3.5.2"
  }

  object Libraries {
    val ScalaTest     = "org.scalatest"     %% "scalatest"       % Versions.ScalaTest
    val ScalaTestPlus = "org.scalatestplus" %% "scalacheck-1-15" % Versions.ScalaTestPlus
    val CatsEffect    = "org.typelevel"     %% "cats-effect"     % Versions.CatsEffect
  }

  lazy val catsEffectPlayground = libraryDependencies ++= Seq(
    Libraries.CatsEffect,
    Libraries.ScalaTest     % Test,
    Libraries.ScalaTestPlus % Test
  )

}
