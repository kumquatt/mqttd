import sbt._

object MyBuild extends Build {

  lazy val root = Project(id = "mqttd", base = file(".")) dependsOn (mqttcProject)
  //https://github.com/kumquatt/mqttpacket.git
  lazy val mqttcProject = RootProject(uri("git://github.com/kumquatt/mqttpacket.git"))
}