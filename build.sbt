ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "ABTest"
  )

val Http4sVersion = "1.0.0-M21"
val CirceVersion = "0.14.0-M5"
val LogVersion = "2.5.0"

libraryDependencies ++= Seq("io.estatico" %% "newtype" % "0.4.4",
                            "org.typelevel" %% "cats-core" % "2.10.0",
                            "org.typelevel" %% "cats-effect" % "3.5.1",
                            "eu.timepit" %% "refined" % "0.11.0",
                            "com.datastax.cassandra" % "cassandra-driver-core" % "4.0.0",
                            "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
                            "org.http4s" %% "http4s-circe" % Http4sVersion,
                            "org.http4s" %% "http4s-dsl" % Http4sVersion,
                            "io.circe" %% "circe-generic" % CirceVersion,
                            "org.typelevel" %% "log4cats-core" % LogVersion,
                            "org.typelevel" %% "log4cats-slf4j" % LogVersion,
                            "org.scalatest" %% "scalatest" % "3.2.15" % "test")