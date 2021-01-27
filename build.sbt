import sbt.Keys.libraryDependencies

enablePlugins(RiffRaffArtifact, UniversalPlugin, JDebPackaging, DebianPlugin, JavaServerAppPackaging, SystemdPlugin)

name := "multimedia-launchdetector-v3"

version := "3.0"

scalaVersion := "2.12.12"

val akkaVersion = "2.6.9"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.2.0"

//AWS
val awsVersion = "1.11.941"
libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-sts" % awsVersion exclude("commons-logging","commons-logging"),
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsVersion exclude("commons-logging","commons-logging"),
  "com.amazonaws" % "amazon-kinesis-client" % "1.14.0" exclude("commons-logging","commons-logging"),
  "com.gu" %% "scanamo" % "1.0.0-M8" exclude("commons-logging","commons-logging")
)

//logging
libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  // https://mvnrepository.com/artifact/ch.qos.logback/logback-core
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.25",
  "com.gu" % "kinesis-logback-appender" % "1.4.4",
)

// https://mvnrepository.com/artifact/org.scalatest/scalatest_2.12
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

//content api
libraryDependencies += "com.gu" %% "content-api-firehose-client" % "0.2.0"
libraryDependencies += "com.gu" %% "content-api-client-default" % "17.1"
libraryDependencies += "org.apache.thrift" % "libthrift" % "0.13.0"
libraryDependencies += "com.typesafe" % "config" % "1.4.0"

// http
libraryDependencies ++= Seq(
  "com.softwaremill.sttp" %% "core" % "1.7.2",
  "com.softwaremill.sttp" %% "async-http-client-backend-future" % "1.7.2",
  "org.asynchttpclient" % "async-http-client" % "2.12.2",
  "com.softwaremill.sttp" %% "akka-http-backend" % "1.7.2",
  "org.scala-lang.modules" %% "scala-xml" % "1.3.0"
)

//json
val circeVersion = "0.13.0"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
)

// testing
// https://mvnrepository.com/artifact/org.mockito/mockito-all
libraryDependencies += "org.mockito" % "mockito-all" % "2.0.2-beta" % "test"

val jacksonVersion = "2.11.0"
//update vulnerable jackson-databind
// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion
libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion
libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % jacksonVersion

// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "30.0-jre"

debianPackageDependencies := Seq("openjdk-8-jre-headless")
serverLoading in Debian := Some(ServerLoader.Systemd)
serviceAutostart in Debian := false

name in Debian := "launchdetector"

maintainer := "Andy Gallagher <andy.gallagher@theguardian.com>"
packageSummary := "Launch Detector that updates asset management with published data from CAPI"
packageDescription := """Launch Detector that updates asset management with published data from CAPI"""
riffRaffPackageType := (packageBin in Debian).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffPackageName := "launchdetector"
riffRaffManifestProjectName := "multimedia:launchdetector-v3"
