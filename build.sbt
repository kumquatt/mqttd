name := "kumquatt"

version := "1.0"

scalaVersion := "2.11.6"

resolvers ++= {
  Seq(
    "repo" at "http://repo.typesafe.com/typesafe/releases/",
    "Paho MQTT Client" at "https://repo.eclipse.org/content/repositories/paho-releases/"
  )
}

libraryDependencies ++= {
  val akkaVersion = "2.3.9"

  Seq(
    "com.typesafe.akka" % "akka-testkit_2.11" % akkaVersion,
    "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion,
    "com.typesafe.akka" % "akka-remote_2.11" % akkaVersion,
    "com.typesafe.akka" % "akka-contrib_2.11" % akkaVersion,
    "com.typesafe.akka" % "akka-slf4j_2.11" % akkaVersion,
//    "com.typesafe.scala-logging" % "scala-logging-slf4j_2.11" % "2.1.2",
    "ch.qos.logback" % "logback-classic" % "1.1.2",

    "junit" % "junit" % "4.10",
    "org.typelevel" % "scodec-core_2.11" % "1.6.0",
    "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.0.2",
    "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
  )
}

