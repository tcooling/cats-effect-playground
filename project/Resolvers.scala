import sbt.Keys.resolvers
import sbt._

object Resolvers {

  val Resolvers = resolvers ++= Seq(
    DefaultMavenRepository,
    Resolver.jcenterRepo,
    "confluent-release" at "https://packages.confluent.io/maven/",
    "jitpack" at "https://jitpack.io"
  )

}
