name := "Http4S-web-novel"

version := "0.1"

scalaVersion := "2.13.8"

val trace4Cats = "0.13.1"
val http4sVersion = "0.23.12"

val root = (project in file("."))
  .enablePlugins(DockerComposePlugin,DockerPlugin,JvmPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "io.janstenpickle" %% "trace4cats-core" % trace4Cats,
      "io.janstenpickle" %% "trace4cats-inject" % trace4Cats,
      "io.janstenpickle" %% "trace4cats-avro-exporter" % trace4Cats,
      "io.janstenpickle" %% "trace4cats-opentelemetry-otlp-grpc-exporter" % trace4Cats,
      "io.janstenpickle" %% "trace4cats-opentelemetry-otlp-http-exporter" % trace4Cats,

      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion
    ),
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
      case PathList("META-INF", _ @ _*) => MergeStrategy.discard
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