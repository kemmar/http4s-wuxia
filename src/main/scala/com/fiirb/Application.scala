package com.fiirb

import cats.data.Kleisli
import com.fiirb.controller.WuxiaController
import com.fiirb.services.{EpubService, NovelService}
import io.janstenpickle.trace4cats.`export`.CompleterConfig
import io.janstenpickle.trace4cats.avro.AvroSpanCompleter
import io.janstenpickle.trace4cats.http4s.common.Http4sRequestFilter
import io.janstenpickle.trace4cats.http4s.server.syntax._
import io.janstenpickle.trace4cats.inject.EntryPoint
import io.janstenpickle.trace4cats.inject.Trace.Implicits.noop
import io.janstenpickle.trace4cats.kernel.SpanSampler
import io.janstenpickle.trace4cats.model.TraceProcess
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import cats.effect.IO._
import cats.effect._
import cats.effect.std.Console
import io.janstenpickle.trace4cats.Span
import io.janstenpickle.trace4cats.http4s.client.syntax.TracedClient
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client

import scala.concurrent.duration.DurationInt

object Application extends ResourceApp.Forever {

  def novelService[F[_]: Async : Console](client: Client[F]) = new NovelService[F](client, new EpubService[F])

  def entryPoint[F[_] : Async](process: TraceProcess): Resource[F, EntryPoint[F]] = {
    for {
      completer <- AvroSpanCompleter.udp[F](process, config = CompleterConfig(batchTimeout = 50.millis))
    } yield EntryPoint[F](SpanSampler.always[F], completer)
  }

  def run(args: List[String]) = {
    type F[x] = IO[x]
    type G[x] = Kleisli[F, Span[F], x]

    for {
      ep <- entryPoint[F](TraceProcess("trace4cats"))

      client <- BlazeClientBuilder[F].resource

      routes = new WuxiaController[G](novelService(client.liftTrace())).routes

      _ <- BlazeServerBuilder[F]
        .bindHttp(8082, "0.0.0.0")
        .withIdleTimeout(10.minutes)
        .withResponseHeaderTimeout(10.minutes)
        .withHttpApp(
          routes.inject(ep, requestFilter = Http4sRequestFilter.allowAll).orNotFound
        )
        .resource
    } yield ()
  }
}
