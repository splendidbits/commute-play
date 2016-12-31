name := """commutealerts"""
version := "0.2"

lazy val buildSettings = Seq(
  scalaVersion := "2.11.8"
)

lazy val fluffylog = (project in file("modules/fluffylog"))
  .enablePlugins(PlayJava, PlayEbean, PlayEnhancer)
  .settings(buildSettings: _*)

lazy val pushservices = (project in file("modules/pushservices"))
  .enablePlugins(PlayJava, PlayEbean, PlayEnhancer)
  .settings(buildSettings: _*)

lazy val commutealerts = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean, PlayEnhancer)
  .dependsOn(fluffylog)
  .dependsOn(pushservices)
  .aggregate(fluffylog)
  .aggregate(pushservices)
  .settings(buildSettings: _*)

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  javaCore,
  javaJdbc,
  cache,
  javaWs,
  javaJpa,
  "org.avaje" % "avaje-agentloader" % "2.1.2",
  "org.postgresql" % "postgresql" % "9.4.1212",
  "org.jsoup" % "jsoup" % "1.10.1",
  "com.google.code.gson" % "gson" % "2.8.0",
  "junit" % "junit" % "4.12" % Test,
  "org.mockito" % "mockito-all" % "2.0.2-beta" % Test
)

dependencyOverrides += "org.avaje.ebeanorm" % "avaje-ebeanorm" % "8.1.1"
dependencyOverrides += "org.avaje.ebeanorm" % "avaje-ebeanorm-agent" % "8.1.1"

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-q")
//resolvers += ("Local Maven Repository" at "/Users/daniel/.ivy2/local")

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

playEbeanDebugLevel := 4
playEbeanModels in Compile := Seq(
  "models.alerts.*",
  "models.accounts.*",
  "models.devices.*"
)

// If set to True, Play will run in a different JVM than SBT
// Turning this to "true" stops debugging. Aways set it to "false" in debug
fork in run := false
