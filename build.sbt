name := "instapuzzle"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.typesafe.slick" %% "slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick" % "0.6.0.1",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"
)

resolvers += "justwrote" at "http://repo.justwrote.it/releases/"

libraryDependencies += "it.justwrote" %% "scala-faker" % "0.2"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.2.2" % "test"

play.Project.playScalaSettings
