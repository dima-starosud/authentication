name := "Autok"

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "spray.io Repository" at "http://repo.spray.io",
  "scalaz-bintray"      at "http://dl.bintray.com/scalaz/releases",
  "sonatype releases"   at "http://oss.sonatype.org/content/repositories/releases/",
  "sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.2.1",
  "io.spray" %% "spray-json" % "1.3.2",
  "io.spray" % "spray-http" % "1.3.1",
  "com.github.tomakehurst" % "wiremock" % "1.55",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "org.slf4j" % "slf4j-simple" % "1.7.12",
  "org.slf4j" % "slf4j-log4j12" % "1.7.12",
  "org.scalaj" %% "scalaj-http" % "1.1.4",
  "org.specs2" %% "specs2-core" % "3.5" % "test"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")

scalacOptions in Test ++= Seq("-Yrangepos")

javacOptions in Compile ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation")
