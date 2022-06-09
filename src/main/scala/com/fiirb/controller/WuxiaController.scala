package com.fiirb.controller

import cats.effect.kernel.Concurrent
import cats.implicits._
import com.fiirb.services.NovelService
import com.fiirb.util.ControllerBase
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.jsonOf

trait ExternalService[F[_]]

class WuxiaController[F[_] : Concurrent](service: NovelService[F]) extends ControllerBase[F] {

 case class UriReq(url: String)

  implicit val decoder = jsonOf[F, UriReq]

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "novels" =>
      for {
        uri <- req.as[UriReq]
        result <- processResponse(service.listNovels(uri.url))(resp => Ok(s"$resp"))
      } yield result
  }

}
