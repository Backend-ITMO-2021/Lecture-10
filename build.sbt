name := "Lecture-10"

version := "0.1"

scalaVersion := "2.13.5"

libraryDependencies +=  "com.lihaoyi" %% "scalatags" % "0.9.4"
libraryDependencies +=  "com.lihaoyi" %% "cask" % "0.7.10"

libraryDependencies +=  "io.getquill" %% "quill-jdbc" % "3.7.1"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.20"

libraryDependencies += "com.opentable.components" % "otj-pg-embedded" % "0.13.3"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.30"
libraryDependencies += "org.asynchttpclient" % "async-http-client" % "2.12.3"


libraryDependencies +=  "com.lihaoyi" %% "utest" % "0.7.9"
testFrameworks += new TestFramework("utest.runner.Framework")
libraryDependencies +=  "com.lihaoyi" %% "requests" % "0.6.8"

Global / concurrentRestrictions += Tags.limit(Tags.Test, 1)