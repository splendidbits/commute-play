name := "commutealerts"
version := "1.0"

lazy val buildSettings = Seq(
  scalaVersion := "2.12.6"
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
  "org.scala-lang" % "scala-library" % "2.12.6",
  "com.splendidbits" % "play-pushservices" % "1.2.2",
  "org.postgresql" % "postgresql" % "42.2.5",
  "org.jetbrains" % "annotations" % "16.0.2",
  "com.google.code.gson" % "gson" % "2.8.5",
  "org.jsoup" % "jsoup" % "1.11.3",
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