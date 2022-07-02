name := "Http4S-web-novel"

version := "0.1"

scalaVersion := "2.13.8"

lazy val trace4Cats = "0.13.1"
lazy val http4sVersion = "0.23.12"
lazy val DoobieVersion = "1.0.0-RC2"
lazy val FlywayVersion = "8.5.13"
lazy val PureConfigVersion = "0.17.1"
lazy val LogbackVersion = "1.2.3"

val root = (project in file("."))
  .enablePlugins(DockerComposePlugin, DockerPlugin, JavaAgent, JvmPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "io.janstenpickle" %% "trace4cats-core" % trace4Cats,
      "io.janstenpickle" %% "trace4cats-inject" % trace4Cats,
      "io.janstenpickle" %% "trace4cats-avro-exporter" % trace4Cats,
      "io.janstenpickle" %% "trace4cats-opentelemetry-otlp-grpc-exporter" % trace4Cats,
      "io.janstenpickle" %% "trace4cats-opentelemetry-otlp-http-exporter" % trace4Cats,

      "io.opentelemetry.javaagent" % "opentelemetry-javaagent" % "1.11.0" % "runtime",

      "org.scalactic" %% "scalactic" % "3.2.12",
      "org.scalamock" %% "scalamock" % "5.1.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.12" % Test,

      "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
      "org.tpolecat" %% "doobie-postgres" % DoobieVersion,
      "org.tpolecat" %% "doobie-core" % DoobieVersion,
      "org.flywaydb" % "flyway-core" % FlywayVersion,

      "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion,

      "ch.qos.logback" % "logback-classic" % LogbackVersion,

      "io.janstenpickle" %% "trace4cats-http4s-client" % trace4Cats,
      "io.janstenpickle" %% "trace4cats-http4s-server" % trace4Cats,

      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,

      "org.slf4j" % "slf4j-api" % "1.7.36",
      "org.slf4j" % "slf4j-simple" % "1.7.36",

      "org.jsoup" % "jsoup" % "1.15.1",

      "net.kemitix" % "epub-creator" % "1.1.0"
    ),
    javaAgents += "io.opentelemetry.javaagent" % "opentelemetry-javaagent" % "1.11.0",
    javaOptions += "-Dotel.javaagent.debug=true",
    docker / imageNames := Seq(
      // Sets the latest tag
      ImageName(s"${organization.value}/${name.value.toLowerCase}:latest"),

      // Sets a name with a tag that contains the project version
      ImageName(
        namespace = Some(organization.value),
        repository = name.value.toLowerCase,
        tag = Some(version.value)
      )
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", _@_*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    dockerImageCreationTask := docker.value,
    docker / dockerfile := {
      // The assembly task generates a fat JAR file
      val artifact: File = assembly.value
      val artifactTargetPath = s"/app/${artifact.name}"

      new Dockerfile {
        from("openjdk:8-jre")
        add(artifact, artifactTargetPath)
        entryPoint("java", "-jar", artifactTargetPath)
      }
    }
  )