package com.fiirb.util

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

trait ControllerBase[F[_]] extends Http4sDsl[F] {

  def routes: HttpRoutes[F]

}
