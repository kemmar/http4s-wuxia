package com.fiirb.db.repository

import cats.effect.MonadCancelThrow
import cats.implicits.toFunctorOps
import com.fiirb.domain.ChapterInfo
import doobie.Transactor
import doobie.implicits._

class NovelChapterRepo[F[_]: MonadCancelThrow](transactor: Transactor[F]) {

  def insertChapterInfo(chapterInfo: ChapterInfo): F[ChapterInfo] =
    sql"""
    DO $$ DECLARE
        BEGIN
           IF EXISTS (SELECT id FROM chapter_info WHERE link = ${chapterInfo.link}) THEN
              update chapter_info set title = ${chapterInfo.title} where link = ${chapterInfo.link};
           ELSE
               insert into chapter_info (title, link) values (${chapterInfo.title}, ${chapterInfo.link});
           END IF;
    END $$;
       """
      .update
      .withUniqueGeneratedKeys[Long]("id")
      .transact(transactor).map { id =>
      chapterInfo.withId(id)
    }
      .attemptSql.map {
      case Left(e) =>
        println(e.getSQLState)
        chapterInfo
      case Right(result) => result
    }
}
