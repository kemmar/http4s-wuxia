package com.fiirb

import cats.effect._
import com.fiirb.controller.{ExternalService, WuxiaController}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._


object Application extends IOApp {

  val stub: ExternalService[IO] = new ExternalService[IO] {
    override def testStub: IO[Int] = IO.apply(2)
  }

  val wuxiaController = new WuxiaController[IO](stub)

  val service = wuxiaController.routes.orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    {
      println("Starting server")

      BlazeServerBuilder[IO]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(service)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
}
