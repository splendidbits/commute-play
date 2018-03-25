name := "commutealerts"
version := "0.3"

lazy val buildSettings = Seq(
  scalaVersion := "2.11.8"
)

lazy val commutealerts = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean, PlayEnhancer)
  .settings(buildSettings: _*)

resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
resolvers += "Bintray jCenter" at "http://jcenter.bintray.com"

libraryDependencies ++= Seq(
  javaCore,
  javaJdbc,
  cacheApi,
  ehcache,
  javaJpa,
  javaWs,
  guice,
  "com.splendidbits" % "play-pushservices" % "1.1.2",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.0.1",
  "org.postgresql" % "postgresql" % "42.2.2",
  "org.jetbrains" % "annotations" % "13.0",
  "com.google.code.gson" % "gson" % "2.8.2",
  "org.jsoup" % "jsoup" % "1.10.1",
  "junit" % "junit" % "4.12" % Test
)

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