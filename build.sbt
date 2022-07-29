
name := "tessellation-demo"

version := "0.1"

scalaVersion := "2.13.6"

val http4sVersion = "0.23.10"
val constellationVersion = "0.11.2"
val pureConfigVersion = "0.17.1"

ThisBuild / resolvers ++= List(
  Resolver.mavenLocal,
  Resolver.sonatypeRepo("snapshots"),
  Resolver.githubPackages("abankowski", "http-request-signer")
)

ThisBuild / assemblyMergeStrategy := {
  case "logback.xml"                                       => MergeStrategy.first
  case x if x.contains("io.netty.versions.properties")     => MergeStrategy.discard
  case PathList(xs @ _*) if xs.last == "module-info.class" => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

libraryDependencies ++= Seq(
  "org.http4s"             %% "http4s-dsl"              % http4sVersion,
  "org.http4s"             %% "http4s-ember-client"     % http4sVersion,
  "org.http4s"             %% "http4s-ember-server"     % http4sVersion,
  "org.http4s"             %% "http4s-circe"            % http4sVersion,
  "org.constellation"      %% "tessellation-dag-shared" % constellationVersion,
  "org.constellation"      %% "tessellation-kernel"     % constellationVersion,
  "org.constellation"      %% "tessellation-keytool"    % constellationVersion,
  "org.constellation"      %% "tessellation-sdk"        % constellationVersion,
  "com.github.pureconfig"  %% "pureconfig"              % pureConfigVersion,
  "com.github.pureconfig"  %% "pureconfig-cats-effect"  % pureConfigVersion,
  "org.scalatest"          %% "scalatest"               % "3.2.11" % Test,
  "com.github.tomakehurst" %  "wiremock-jre8"           % "2.33.2" % Test
)

scalacOptions ++= List("-Ymacro-annotations")