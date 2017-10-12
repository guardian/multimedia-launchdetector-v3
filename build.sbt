name := "multimedia-launchdetector-v3"

version := "0.1"

scalaVersion := "2.12.3"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-http_2.12
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.10"

//AWS
libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-sts" % "1.11.208",
  "com.amazonaws" % "amazon-kinesis-client" % "1.8.5"
)

//logging
libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-api" % "2.9.1",
  "org.apache.logging.log4j" % "log4j-core" % "2.9.1",
  "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.7.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.4"
)

// https://mvnrepository.com/artifact/org.scalatest/scalatest_2.12
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

libraryDependencies += "com.gu" %% "content-api-firehose-client" % "0.9"

libraryDependencies += "com.typesafe" % "config" % "1.3.1"