name := "spark-utils-demos"

organization := "org.tupol"

scalaVersion := "2.11.12"

val sparkUtilsVersion = "0.4.1"

val sparkVersion = "2.4.3"
val sparkXmlVersion = "0.4.1"
val sparkAvroVersion = "4.0.0"

// ------------------------------
// DEPENDENCIES AND RESOLVERS

updateOptions := updateOptions.value.withCachedResolution(true)
resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"


lazy val providedDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion force(),
  "org.apache.spark" %% "spark-sql" % sparkVersion force(),
  "org.apache.spark" %% "spark-mllib" % sparkVersion force(),
  "org.apache.spark" %% "spark-streaming" % sparkVersion force(),
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion
)

libraryDependencies ++= providedDependencies.map(_ % "provided")

// Jackson dependencies over Spark and Kafka Versions can be tricky; for Spark 2.4.x we need this override
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7"

libraryDependencies ++= Seq(
  "org.tupol" %% "spark-utils" % sparkUtilsVersion,
  "org.tupol" %% "spark-utils" % sparkUtilsVersion % "test" classifier "tests",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.0" % "test"
)
// ------------------------------
// ASSEMBLY
assemblyJarName in assembly := s"${name.value}-assembly.jar"

// Add exclusions, provided...
assemblyMergeStrategy in assembly := {
  {
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
}

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in(Compile, assembly), assembly)

// Skip test in `assembly` and encompassing publish(Local) tasks.
test in assembly := {}

// ------------------------------
