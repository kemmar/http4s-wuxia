package com.fiirb.services

import cats.Parallel
import cats.effect.MonadCancelThrow
import cats.effect.kernel.Concurrent
import com.fiirb.db.repository.RepoBuilder
import doobie.Transactor
import org.http4s.client.Client

abstract class ServiceBuilder[F[_]: MonadCancelThrow: Concurrent: Parallel]() extends RepoBuilder[F] {

  val client: Client[F]

//  val transactor: Transactor[F]

  val epubService: EpubService[F] = new EpubService[F]()

  val novelService: NovelService[F] = new NovelService[F](client, epubService)

}
