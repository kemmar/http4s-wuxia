package com.fiirb.util

import cats.data.EitherT
import cats.effect.kernel.Concurrent
import cats.implicits._
import org.http4s.{HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl

trait ControllerBase[F[_]] extends Http4sDsl[F] {

  def routes: HttpRoutes[F]

  def processResponse[T](respIO: F[T])(status: T => F[Response[F]])(implicit concurrent: Concurrent[F]): F[Response[F]] = {
    for {
      respIO <-
        respIO
          .map(status)
          .recover(e => InternalServerError(e.getMessage))
      result <- respIO
    } yield result
  }

}

object ControllerBase {
  type WuxiaResult[F[_], T] = EitherT[F, Exception, T]
}
