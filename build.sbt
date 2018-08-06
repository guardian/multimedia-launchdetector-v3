import sbt.Keys.libraryDependencies

enablePlugins(RiffRaffArtifact, UniversalPlugin, JDebPackaging, DebianPlugin, JavaServerAppPackaging, SystemdPlugin)

name := "multimedia-launchdetector-v3"

version := "3.0"

scalaVersion := "2.12.3"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-testkit_2.11
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.6" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.6"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.6"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.5.6"
// https://mvnrepository.com/artifact/com.typesafe.akka/akka-http_2.12
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.10"

//AWS
libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-sts" % "1.11.378" exclude("commons-logging","commons-logging"),
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.378" exclude("commons-logging","commons-logging"),
  "com.amazonaws" % "amazon-kinesis-client" % "1.9.1" exclude("commons-logging","commons-logging"),
  "com.gu" %% "scanamo" % "1.0.0-M2" exclude("commons-logging","commons-logging")
)

//logging
libraryDependencies ++= Seq(
  // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  // https://mvnrepository.com/artifact/ch.qos.logback/logback-core
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.25"

)

// https://mvnrepository.com/artifact/org.scalatest/scalatest_2.12
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

//content api
libraryDependencies += "com.gu" %% "content-api-firehose-client" % "0.10"
libraryDependencies += "com.gu" %% "content-api-client" % "11.43"

libraryDependencies += "com.typesafe" % "config" % "1.3.1"

// http
libraryDependencies ++= Seq(
  "com.softwaremill.sttp" %% "core" % "0.0.20",
  "com.softwaremill.sttp" %% "async-http-client-backend-future" % "0.0.20",
  "org.asynchttpclient" % "async-http-client" % "2.0.37",
  "com.softwaremill.sttp" %% "akka-http-backend" % "0.0.20",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
)


// testing
// https://mvnrepository.com/artifact/org.mockito/mockito-all
libraryDependencies += "org.mockito" % "mockito-all" % "2.0.2-beta" % "test"

val jacksonVersion = "2.9.6"
//update vulnerable jackson-databind
// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
// https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion
libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion
libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % jacksonVersion

// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "25.1-jre"

debianPackageDependencies := Seq("openjdk-8-jre-headless")
serverLoading in Debian := Some(ServerLoader.Systemd)
serviceAutostart in Debian := false

version in Debian := s"${version.value}-${sys.env.getOrElse("CIRCLE_BUILD_NUM","SNAPSHOT")}"
name in Debian := "launchdetector"

maintainer := "Andy Gallagher <andy.gallagher@theguardian.com>"
packageSummary := "Launch Detector that updates asset management with published data from CAPI"
packageDescription := """Launch Detector that updates asset management with published data from CAPI"""
riffRaffPackageType := (packageBin in Debian).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffManifestBranch := sys.env.getOrElse("CIRCLE_BRANCH","unknown")
riffRaffManifestRevision := sys.env.getOrElse("CIRCLE_BUILD_NUM","SNAPSHOT")
riffRaffManifestVcsUrl := sys.env.getOrElse("CIRCLE_BUILD_URL", "")
riffRaffBuildIdentifier := sys.env.getOrElse("CIRCLE_BUILD_NUM", "SNAPSHOT")
riffRaffPackageName := "launchdetector"
riffRaffManifestProjectName := "multimedia:launchdetector-v3"
