name := "scalagraph"

version := "0.1"

scalaVersion := "2.12.5"

libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-json" % "2.6.7",
    "com.typesafe.akka" %% "akka-http"   % "10.1.1",
    "com.typesafe.akka" %% "akka-stream" % "2.5.11",
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1",
    "io.spray" %%  "spray-json" % "1.3.3"
)