ThisBuild / version := "0.2.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.8.0"

val circeVersion = "0.14.13"
maintainer := "maarten.vandercammen@vub.be"

resolvers +=
  "Gradle" at "https://repo.gradle.org/ui/native/libs-releases"
resolvers += Resolver.sbtPluginRepo("releases")

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "os-lib" % "0.11.3",
  "io.joern" %% "pysrc2cpg" % "2.0.250",
  "com.github.scopt" %% "scopt" % "4.1.0",
  "com.github.pathikrit" %% "better-files" % "3.9.2",
  "org.jsoup" % "jsoup" % "1.22.2",
  "org.scalatest" %% "scalatest" % "3.2.20" % Test,
  guice
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "SBOM_augmentor"
  )

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case _                        => MergeStrategy.first
}
mainClass in assembly := Some("app.Main")
