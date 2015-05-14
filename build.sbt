name := "Autok"

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "spray.io Repository" at "http://repo.spray.io",
  "scalaz-bintray"      at "http://dl.bintray.com/scalaz/releases",
  "sonatype releases"   at "http://oss.sonatype.org/content/repositories/releases/",
  "sonatype snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++=
  Seq(
    "com.typesafe" % "config" % "1.3.0",
    "io.spray" %% "spray-json" % "1.3.2",
    "com.github.tomakehurst" % "wiremock" % "1.55",
    "io.spray" %% "spray-testkit" % "1.3.3" % "test",
    "org.scalaj" %% "scalaj-http" % "1.1.4",
    "org.specs2" %% "specs2-core" % "3.5" % "test"
  )

scalacOptions ++= Seq("-unchecked", "-deprecation")

scalacOptions in Test ++= Seq("-Yrangepos")
