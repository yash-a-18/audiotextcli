val scala3Version = "3.7.1"

lazy val root = project
  .in(file("."))
  .enablePlugins(PackPlugin)
  .settings(
    name := "audio-text-cli",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,



    libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test,
    libraryDependencies += "dev.zio" %% "zio-cli" % "0.7.2",
    libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.9.2",
    libraryDependencies += "com.softwaremill.sttp.client4" %% "core" % "4.0.0-M6",
    libraryDependencies += "com.softwaremill.sttp.client4" %% "circe" % "4.0.0-M6",
    libraryDependencies += "io.circe" %% "circe-core" % "0.14.14",
    libraryDependencies += "io.circe" %% "circe-generic" % "0.14.14",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.14.14",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.5.2",
    libraryDependencies += "com.typesafe" % "config" % "1.4.3"
  )
