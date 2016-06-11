import sbt.Def._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.packager.docker._
import scalariform.formatter.preferences._

resolvers ++= Seq(
  "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"
)

val catsVers = "0.6.0"
val algebraVers = "0.4.0"
val monocleVers = "1.2.2"

val specsVers = "3.8.2"

val scalaVers = "2.11.8"
val scalaBinaryVers = scalaVers

// Dependencies
val compilerPlugins = Seq(
  compilerPlugin("org.spire-math" %% "kind-projector"  % "0.8.0"),
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

val rootDependencies = Seq(
  "org.typelevel" %% "cats" % catsVers,
  "com.github.julien-truffaut"  %%  "monocle-core"    % monocleVers,
  "com.github.julien-truffaut"  %%  "monocle-generic" % monocleVers,
  "com.github.julien-truffaut"  %%  "monocle-macro"   % monocleVers,
  "com.github.julien-truffaut"  %%  "monocle-state"   % monocleVers,
  "com.github.julien-truffaut"  %%  "monocle-refined" % monocleVers
)

val testDependencies = Seq(
  "org.specs2" %% "specs2-core" % specsVers % "test",
  "org.specs2" %% "specs2-scalacheck" % specsVers % "test"
)

val dependencies = compilerPlugins ++ rootDependencies ++ testDependencies

val compileSettings = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:_",
  "-target:jvm-1.8",
  "-unchecked",
  "-Ybackend:GenBCode",
  "-Ydelambdafy:method",
  "-Xfuture",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard"
)

val forkedJvmOption = Seq(
  "-server",
  "-Dfile.encoding=UTF8",
  "-Duser.timezone=GMT",
  "-Xss1m",
  "-Xms2048m",
  "-Xmx2048m",
  "-XX:+CMSClassUnloadingEnabled",
  "-XX:ReservedCodeCacheSize=256m",
  "-XX:+DoEscapeAnalysis",
  "-XX:+UseConcMarkSweepGC",
  "-XX:+UseParNewGC",
  "-XX:+UseCodeCacheFlushing",
  "-XX:+UseCompressedOops"
)

val settings = Seq(
  name := "recommendation",
  organization := "org.patricknoir",
  version := "1.0.0-SNAPSHOT",
  scalaVersion := scalaVers,
  libraryDependencies ++= dependencies,
  fork in run := true,
  fork in Test := true,
  fork in testOnly := true,
  connectInput in run := true,
  javaOptions in run ++= forkedJvmOption,
  javaOptions in Test ++= forkedJvmOption,
  scalacOptions := compileSettings,
  initialCommands in console :=
    """
      |import org.patricknoir.recommendation._
      |
    """.stripMargin,
  ScalariformKeys.preferences := PreferencesImporterExporter.loadPreferences((file(".") / "formatter.preferences").getPath)
  //mainClass in (Compile, run) := Option("org.patricknoir.recommendation.Main")
)

val dockerSettings = Seq(
  defaultLinuxInstallLocation in Docker := "opt/recommendation",
  dockerCommands := Seq(
    Cmd("FROM", "alpine:3.3"),
    Cmd("RUN apk upgrade --update && apk add --update openjdk-jre && rm -rf /var/cache/apk/*"),
    Cmd("ADD", "opt /opt"),
    ExecCmd("RUN", "mkdir", "-p", "/var/log/recommendation"),
    ExecCmd("ENTRYPOINT", "/opt/recommendation/bin/recommendation")
  ),
  version in Docker := version.value
)

lazy val main = 
  project
    .in(file("."))
    .settings(dockerSettings ++ settings:_*)
    .enablePlugins(AshScriptPlugin,JavaServerAppPackaging, UniversalDeployPlugin)
