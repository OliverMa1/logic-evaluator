name := "logic-evaluator"

version := "0.1"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

// for debugging:
javaOptions in reStart += "-agentlib:jdwp=transport=dt_socket,server=y,address=5005,suspend=n"
fork in run := true
cancelable in Global := true


//libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % Test
libraryDependencies += "junit" % "junit" % "4.12" % "test"

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v")