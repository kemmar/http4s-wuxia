package com.fiirb.controller

import cats.Parallel
import cats.effect.kernel.Concurrent
import com.fiirb.services.ServiceBuilder
import doobie.Transactor
import org.http4s.client.Client

class ControllerBuilder[F[_] : Concurrent: Parallel](val client: Client[F]) extends ServiceBuilder[F] {

  val wuxiaController = new WuxiaController[F](novelService)

}
