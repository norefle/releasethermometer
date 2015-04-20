name := "ReleaseThermometer"

version := "1.0"

scalaVersion := "2.11.6"

resolvers ++= Seq(
    "Codehaus Hudson" at "http://maven.jenkins-ci.org/content/repositories/releases/",
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
    "com.offbytwo.jenkins" % "jenkins-client" % "0.3.0",
    "org.json4s" %% "json4s-native" % "3.2.11",
    "org.json4s" %% "json4s-jackson" % "3.2.11"
)

assemblyJarName := "thermometer"

assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case _ => MergeStrategy.first
}
