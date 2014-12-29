import Web.Keys._

lazy val root = (project in file("."))
  .settings(
    name := "fuud",
    version := "1.0",
    scalaVersion := "2.11.2",
    libraryDependencies ++= {
      val akkaV = "2.3.+"
      val sprayV = "1.3.+"
      Seq(
        "io.spray" %% "spray-can" % sprayV,
        "io.spray" %% "spray-routing" % sprayV,
        "io.spray" %% "spray-json" % "1.3.+",
        "com.typesafe.akka" %% "akka-actor" % akkaV,
        "org.jsoup" % "jsoup" % "1.8.1",
        "com.squants" %% "squants" % "0.4.2"
      )
    }
  )
  .settings(Web.settings:_*)
  .settings(
    browserifyTransforms += Seq("reactify"),
    browserifyOptions += "-d"
  )
  .settings(Revolver.settings: _*)
