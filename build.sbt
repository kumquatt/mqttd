name := "kumquatt"

version := "1.0"

scalaVersion := "2.11.6"

mainClass in Compile := Some("plantae.citrus.Launcher")

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

//enablePlugins(JavaAppPackaging, JDebPackaging)
//enablePlugins(JavaAppPackaging)

resolvers ++= {
  Seq(
    "repo" at "http://repo.typesafe.com/typesafe/releases/",
  )
}

libraryDependencies ++= {
  val akkaVersion = "2.3.9"
  val sprayVersion = "1.3.3"

  Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-remote" % akkaVersion,
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,

    "ch.qos.logback" % "logback-classic" % "1.1.2",

    "junit" % "junit" % "4.10",
    "org.scodec" % "scodec-core_2.11" % "1.7.1",
    "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
    "com.typesafe" % "config" % "1.2.1",

    "io.spray"            %%  "spray-can"     % sprayVersion,
    "io.spray"            %%  "spray-routing-shapeless2" % sprayVersion,
    "io.spray"            %%  "spray-testkit" % sprayVersion  % "test",
    "io.spray" %% "spray-httpx" % sprayVersion
  )
}

Revolver.settings
