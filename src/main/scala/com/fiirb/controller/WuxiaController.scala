package com.fiirb.controller

import cats.effect.kernel.Concurrent
import cats.implicits._
import com.fiirb.services.NovelService
import com.fiirb.util.ControllerBase
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.jsonOf

class WuxiaController[F[_] : Concurrent](service: NovelService[F]) extends ControllerBase[F] {

 case class NameReq(name: String)

  implicit val decoder = jsonOf[F, NameReq]

  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "novels" =>
      for {
        nameReq <- req.as[NameReq]
        result <- processResponse(service.findNovelByName(nameReq.name))(resp => Ok(resp.getName))
      } yield result
//    case req@POST -> Root / "novels" / "persist" =>
//      for {
//        nameReq <- req.as[NameReq]
//        result <- processResponse(service.loadChapterInfo(nameReq.name))(resp => Ok(resp.toList))
//      } yield result
  }

}
