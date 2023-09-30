ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "ABTest"
  )

libraryDependencies ++= Seq("io.estatico" %% "newtype" % "0.4.4",
                            "org.typelevel" %% "cats-core" % "2.10.0",
                            "org.typelevel" %% "cats-effect" % "3.5.1")