import sbt.Keys.libraryDependencies

enablePlugins(RiffRaffArtifact, UniversalPlugin, JDebPackaging, DebianPlugin, JavaServerAppPackaging, SystemdPlugin)

scalacOptions ++= Seq("-deprecation")
name := "multimedia-launchdetector-v3"

version := "3.0"

scalaVersion := "2.12.12"

val akkaVersion = "2.8.1"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.5.3"

//AWS
val awsVersion = "1.12.730"
libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-sts" % awsVersion exclude("commons-logging","commons-logging"),
  "com.amazonaws" % "aws-java-sdk-dynamodb" % awsVersion exclude("commons-logging","commons-logging"),
  "com.amazonaws" % "amazon-kinesis-client" % "1.14.10" exclude("commons-logging","commons-logging"),
  "com.gu" %% "scanamo" % "1.0.0-M8" exclude("commons-logging","commons-logging")
)

//logging
libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
  "ch.qos.logback" % "logback-classic" % "1.2.13",
  // https://mvnrepository.com/artifact/ch.qos.logback/logback-core
  "ch.qos.logback" % "logback-core" % "1.2.13",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.25",
  "com.gu" % "kinesis-logback-appender" % "2.1.3",
)

// https://mvnrepository.com/artifact/org.scalatest/scalatest_2.12
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"
libraryDependencies += "org.typelevel" %% "cats-free" % "2.4.0"

//content api
libraryDependencies += "com.gu" %% "content-api-firehose-client" % "0.2.0"
libraryDependencies += "com.gu" %% "content-api-client-default" % "17.24.0"
libraryDependencies += "org.apache.thrift" % "libthrift" % "0.16.0"
libraryDependencies += "com.typesafe" % "config" % "1.4.0"

// http
libraryDependencies ++= Seq(
  "com.softwaremill.sttp" %% "core" % "1.7.2",
  "com.softwaremill.sttp" %% "async-http-client-backend-future" % "1.7.2",
  "org.asynchttpclient" % "async-http-client" % "3.0.0.Beta3",
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

val jacksonVersion = "2.15.2"
//update vulnerable jackson-databind
// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion
libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion
libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % jacksonVersion

// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "32.1.2-jre"
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.13"

debianPackageDependencies := Seq("openjdk-11-jre-headless")
Debian / serverLoading := Some(ServerLoader.Systemd)
Debian / serviceAutostart := false

Debian / name := "launchdetector"

maintainer := "Andy Gallagher <andy.gallagher@theguardian.com>"
packageSummary := "Launch Detector that updates asset management with published data from CAPI"
packageDescription := """Launch Detector that updates asset management with published data from CAPI"""
riffRaffPackageType := (Debian / packageBin).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffPackageName := "launchdetector"
riffRaffManifestProjectName := "multimedia:launchdetector-v3"
