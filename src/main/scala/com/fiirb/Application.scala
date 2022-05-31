package com.fiirb

import cats.effect._
import org.http4s._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io._
import org.http4s.implicits._


object Application extends IOApp {

  val service = HttpRoutes.of[IO] {
    case GET -> Root / "test" =>
      Ok("test successful")
  }.orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(service)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
