name := "http4s-mongo"

version := "0.1"

scalaVersion := "2.13.2"

val mongoVersion = "4.2.2"
val http4sVersion = "0.21.15"
val circeVersion = "0.13.0"
val slf4jVersion = "1.7.30"
val cfgVersion = "1.4.1"
val ctVersion = "0.12"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-tagless-macros" % ctVersion,
  "org.mongodb.scala" %% "mongo-scala-driver" % mongoVersion,
  //"org.lyranthe" %% "fs2-mongodb" % "0.5.0",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-yaml" % circeVersion,
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "slf4j-simple" % slf4jVersion,
  "com.typesafe" % "config" % cfgVersion
)
