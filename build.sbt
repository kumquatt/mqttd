name := "kumquatt"

version := "1.0"

scalaVersion := "2.11.6"

mainClass in Compile := Some("plantae.citrus.Launcher")

//enablePlugins(JavaAppPackaging, JDebPackaging)
enablePlugins(JavaAppPackaging)

resolvers ++= {
  Seq(
    "repo" at "http://repo.typesafe.com/typesafe/releases/",
    "Paho MQTT Client" at "https://repo.eclipse.org/content/repositories/paho-releases/",
    "wasted.io/repo" at "http://repo.wasted.io/mvn"
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
    "ch.qos.logback" % "logback-classic" % "1.1.2",

    "junit" % "junit" % "4.10",
    "org.scodec" % "scodec-core_2.11" % "1.7.1",
    "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.0.2",
    "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
    "com.typesafe" % "config" % "1.2.1",

    "io.wasted" % "wasted-util" % "0.9.0"
  )
}
