name := """commute-gcm"""
version := "0.1"

lazy val buildSettings = Seq(
  scalaVersion := "2.11.8"
)

lazy val splendidlog = (project in file("modules/splendidlog"))
  .enablePlugins(PlayJava, PlayEbean, PlayEnhancer)
  .settings(buildSettings: _*)

lazy val pushservices = (project in file("modules/pushservices"))
  .enablePlugins(PlayJava, PlayEbean, PlayEnhancer)
  .settings(buildSettings: _*)

lazy val commutegcm = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean, PlayEnhancer)
  .dependsOn(splendidlog)
  .dependsOn(pushservices)
  .aggregate(splendidlog)
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
  "org.postgresql" % "postgresql" % "9.4.1209",
  "org.jsoup" % "jsoup" % "1.7.2",
  "junit" % "junit" % "4.11" % Test,
  "com.google.code.gson" % "gson" % "2.2"
)
dependencyOverrides += "org.avaje.ebeanorm" % "avaje-ebeanorm" % "7.13.1"
dependencyOverrides += "org.avaje.ebeanorm" % "avaje-ebeanorm-agent" % "4.10.1"

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

// Turning this to "true" stops debugging. Aways set it to "false" in debug
fork in run := false
