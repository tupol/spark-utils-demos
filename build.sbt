name := "spark-utils-demos"

organization := "org.tupol"

scalaVersion := "2.11.12"

val sparkUtilsVersion = "0.3.1"

val sparkVersion = "2.3.2"
val sparkXmlVersion = "0.4.1"
val sparkAvroVersion = "4.0.0"

// ------------------------------
// DEPENDENCIES AND RESOLVERS

updateOptions := updateOptions.value.withCachedResolution(true)
resolvers += "Sonatype OSS Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases"
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"


lazy val providedDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion force(),
  "org.apache.spark" %% "spark-sql" % sparkVersion force(),
  "org.apache.spark" %% "spark-mllib" % sparkVersion force(),
  "org.apache.spark" %% "spark-streaming" % sparkVersion force()
)

libraryDependencies ++= providedDependencies.map(_ % "provided")

libraryDependencies ++= Seq(
  "org.tupol" %% "spark-utils" % sparkUtilsVersion,
  "com.databricks" %% "spark-xml" % sparkXmlVersion,
  "com.databricks" %% "spark-avro" % sparkAvroVersion
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
