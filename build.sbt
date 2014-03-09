name := "instapuzzle"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

resolvers += "justwrote" at "http://repo.justwrote.it/releases/"

libraryDependencies += "it.justwrote" %% "scala-faker" % "0.2"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.2.2" % "test"

play.Project.playScalaSettings
