name := "instapuzzle"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.typesafe.slick" %% "slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick" % "0.5.0.8"
)

resolvers += "justwrote" at "http://repo.justwrote.it/releases/"

libraryDependencies += "it.justwrote" %% "scala-faker" % "0.2"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.2.2" % "test"

play.Project.playScalaSettings
