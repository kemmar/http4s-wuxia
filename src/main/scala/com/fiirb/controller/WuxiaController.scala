package com.fiirb.controller

import cats.effect._
import cats.implicits._
import com.fiirb.util.ControllerBase
import org.http4s.HttpRoutes

trait ExternalService[F[_]] {
  def testStub: F[Int]
}

class WuxiaController[F[_] : Async](service: ExternalService[F]) extends ControllerBase[F] {

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "test" =>
      for {
        i <- service.testStub
        resp <- Ok(s"test successful $i")
      } yield resp
  }

}
