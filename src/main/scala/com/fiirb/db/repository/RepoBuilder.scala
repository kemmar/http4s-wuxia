package com.fiirb.db.repository

import cats.effect.MonadCancelThrow
import doobie.Transactor

abstract class RepoBuilder[F[_]: MonadCancelThrow] {

//  val transactor: Transactor[F]

//  val novelChapterRepo: NovelChapterRepo[F] = new NovelChapterRepo(transactor)

}
