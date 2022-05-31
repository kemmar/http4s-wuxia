package com.fiirb.controller

import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

trait ExternalService[F[_]] {
  def testStub: F[Int]
}

class WuxiaController[F[_]: Async](service: ExternalService[F]) extends Http4sDsl[F] {


def route: HttpRoutes[F] = HttpRoutes.of[F] {
  case GET -> Root / "test" =>
    Ok("test successful")
}


}
