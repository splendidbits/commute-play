name := """commute-gcm"""
version := "0.1"

lazy val buildSettings = Seq(
  scalaVersion := "2.11.7"
)

lazy val splendidlog = (project in file("modules/splendidlog"))
  .enablePlugins(PlayJava, PlayEbean, PlayEnhancer)
  .settings(buildSettings: _*)

lazy val commutegcm = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean)
  .aggregate(splendidlog)
  .dependsOn(splendidlog)
  .settings(buildSettings: _*)

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  javaCore,
  javaJdbc,
  evolutions,
  cache,
  javaWs,
  javaJpa,
  "org.avaje" % "avaje-agentloader" % "2.1.2",
  "org.postgresql" % "postgresql" % "9.4.1208",
  "junit" % "junit" % "4.11" % Test,
  "com.google.code.gson" % "gson" % "2.2")

val appDependencies = Seq(
  // Add your project dependencies
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-q")
//resolvers += ("Local Maven Repository" at "/Users/daniel/.ivy2/local")

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

playEbeanDebugLevel := 4
playEbeanModels in Compile := Seq(
  "models.alerts.*",
  "models.accounts.*",
  "models.persons.*",
  "models.taskqueue.*",
  "models.registrations.*"
)

// Turning this to "true" stops debugging. Aways set it to "false" in debug
fork in run := false
