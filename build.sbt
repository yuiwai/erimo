scalaVersion in ThisBuild := "2.12.4"
organization in ThisBuild := "com.yuiwai"
version in ThisBuild := "0.1.0"
crossScalaVersions in ThisBuild := Seq("2.12.4", "2.11.11")

lazy val core = (project in file("core"))
  .settings(
    name := "erimo-core",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-persistence" % "2.5.9",
      "com.typesafe.akka" %% "akka-stream" % "2.5.9"
    ),
    publishTo := Some(Resolver.file("file", file("release")))
  )

lazy val example = (project in file("example"))
  .dependsOn(core)
