import sbt.ExclusionRule
import sbt.Keys.scalaVersion
import sbtassembly.MergeStrategy

lazy val commonSettings = Seq(
  organization := "com.kongming.mxnet",
  name := "mxnet-on-spark",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.11",
  javaOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
  resolvers += Resolver.mavenLocal,
  resolvers += "velvia maven" at "http://dl.bintray.com/velvia/maven",
  resolvers += Resolver.url("bintray-sbt-plugin-releases", url("https://dl.bintray.com/en-japan/sbt"))(Resolver.ivyStylePatterns),
  resolvers += "En Japan" at "https://raw.github.com/en-japan/repository/master/releases",
  updateOptions := updateOptions.value.withCachedResolution(true),
  publishMavenStyle := true,
  exportJars := true,
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
  updateOptions := updateOptions.value.withLatestSnapshots(true),
  conflictManager := ConflictManager.default,
  assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
  assemblyExcludedJars in assembly := {
    val cp = (fullClasspath in assembly).value
    cp filter { tempPath =>
      tempPath.data.getName.startsWith("akka") || tempPath.data.getName.startsWith("commons-lang3")
    }
  },
  assemblyMergeStrategy in assembly := {
    case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
    case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
    case PathList(ps@_*) if ps.last endsWith ".properties" =>
      MergeStrategy.discard
    case "application.conf" => MergeStrategy.concat
    case "unwanted.txt" => MergeStrategy.discard
    case x if Assembly.isConfigFile(x) =>
      MergeStrategy.concat
    case PathList(ps@_*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
      MergeStrategy.rename
    case PathList("META-INF", xs@_*) =>
      xs.map(_.toLowerCase) match {
        case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
          MergeStrategy.discard
        case ps@(x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
          MergeStrategy.discard
        case "plexus" :: xs => MergeStrategy.discard
        case "services" :: xs => MergeStrategy.filterDistinctLines
        case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
          MergeStrategy.filterDistinctLines
        case _ => MergeStrategy.last
      }
    case _ => MergeStrategy.last
  },
  test in assembly := {}
)


lazy val mllib = (project in file("mllib"))
  .settings(commonSettings: _*)
  .settings(
    name := "mllib",
    assemblyJarName in assembly := s"${name.value}.jar",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % "2.11.11" % scope,
      "org.apache.avro" % "avro" % avroVersion % scope,
      "org.apache.avro" % "avro-compiler" % avroVersion % scope,
      "org.scalatest" %% "scalatest" % "2.2.6" % scope,
      "org.apache.spark" %% "spark-core" % sparkVersion % scope excludeAll (ExclusionRule("org.apache.commons", "commons-lang3")),
      "org.apache.spark" %% "spark-sql" % sparkVersion % scope,
      "org.apache.spark" %% "spark-mllib" % sparkMllibVersion % scope,
      "org.apache.mxnet" % "mxnet-full_2.11-linux-x86_64-cpu" % "1.2.0.1-SNAPSHOT" % scope,
      "args4j" % "args4j" % "2.0.29" % scope
    )
  )

lazy val utils = (project in file("utils"))
  .settings(commonSettings: _*)
  .settings(
    name := "utils",
    assemblyJarName in assembly := s"${name.value}.jar",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % "2.11.11" % scope,
      "org.apache.avro" % "avro" % avroVersion % scope,
      "org.apache.avro" % "avro-compiler" % avroVersion % scope,
      "org.scalatest" %% "scalatest" % "2.2.6" % scope,
      "org.apache.spark" %% "spark-core" % sparkVersion % scope excludeAll (ExclusionRule("org.apache.commons", "commons-lang3")),
      "org.apache.spark" %% "spark-sql" % sparkVersion % scope,
      "org.apache.spark" %% "spark-mllib" % sparkMllibVersion % scope,
      "org.apache.mxnet" % "mxnet-full_2.11-linux-x86_64-cpu" % "1.2.0.1-SNAPSHOT" % scope,
      "args4j" % "args4j" % "2.0.29" % scope
    )
)

lazy val sparkVersion = "2.3.0"
lazy val sparkMllibVersion = "2.3.0.1"
lazy val sparkJobServerVersion = "0.7.0"
lazy val alluxioVersion = "1.5.0"
lazy val ansjVersion = "3.7.1"
lazy val hadoopVersion = "2.7.1"
lazy val typesafeVersion = "1.3.0"
lazy val sparkKnnVersion = "0.2.0.2"
lazy val protobufVersion = "3.2.0"
lazy val breezeVersion = "0.12"
lazy val avroVersion = "1.8.1"
lazy val scope = "compile" //provided