name := "RssTest"

version := "0.1"

scalaVersion := "2.11.8"

val sparkVersion = "2.3.4"


libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "com.github.catalystcode" %% "streaming-rss-html" % "1.0.3",
  "com.rometools" % "rome" % "1.8.0"
)

mainClass in assembly := Some("main")
assemblyJarName in assembly := "rss-test.jar"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}