package com.fiirb

import cats.effect.IO._
import cats.effect._
import com.fiirb.config.Config
import com.fiirb.controller.ControllerBuilder
import com.fiirb.db.Database
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import io.janstenpickle.trace4cats.`export`.CompleterConfig
import io.janstenpickle.trace4cats.avro.AvroSpanCompleter
import io.janstenpickle.trace4cats.inject.EntryPoint
import io.janstenpickle.trace4cats.kernel.SpanSampler
import io.janstenpickle.trace4cats.model.TraceProcess
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.client.Client
import org.http4s.implicits._

import scala.concurrent.duration.DurationInt

object HttpServer {
  def create(configFile: String = "application.conf"): IO[ExitCode] = {
    resources(configFile).use(create)
  }

  def entryPoint[F[_] : Async](process: TraceProcess): Resource[F, EntryPoint[F]] = {
        for {
          completer <- AvroSpanCompleter.udp[F](process, config = CompleterConfig(batchTimeout = 50.millis))
        } yield EntryPoint[F](SpanSampler.always[F], completer)
      }

  private def resources(configFile: String): Resource[IO, Resources] = {
    for {
      config <- Config.load(configFile)
      ec <- ExecutionContexts.fixedThreadPool[IO](config.database.threadPoolSize)
      transactor <- Database.transactor(config.database, ec)
      ep <- entryPoint[IO](TraceProcess("web-novel-app"))
      client <- BlazeClientBuilder[IO]
        .withMaxWaitQueueLimit(1024)
        .resource
    } yield Resources(client, transactor, config, ep)
  }

  private def create(resources: Resources): IO[ExitCode] = {
//      _ <- Database.initialize(resources.transactor)
    for {
      repository <- IO.pure(new ControllerBuilder(resources.client))
      exitCode <- BlazeServerBuilder[IO]
        .bindHttp(resources.config.server.port, resources.config.server.host)
        .withIdleTimeout(10.minutes)
        .withResponseHeaderTimeout(10.minutes)
        .withHttpApp(repository.wuxiaController.routes.orNotFound).serve.compile.lastOrError
    } yield exitCode
  }

  case class Resources(client: Client[IO],transactor: HikariTransactor[IO], config: Config, entryPoint: EntryPoint[IO])
}
