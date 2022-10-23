package com.fiirb.services

import cats.{Applicative, Parallel}
import cats.effect.kernel.Concurrent
import cats.effect.{MonadCancelThrow, Ref}
import cats.implicits._
import com.fiirb.db.repository.NovelChapterRepo
import com.fiirb.domain.{ChapterInfo, NovelChapter, NovelInfo}
import org.http4s.Uri
import org.http4s.client.Client
import org.jsoup.Jsoup

import java.io.File
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try
import scala.util.control.NonFatal

class NovelService[F[_] : MonadCancelThrow: Concurrent: Parallel](client: Client[F],
                                                                     epubService: EpubService[F]) extends ExternalService[F] {

  private def readPage(uri: Uri): F[String] = {
    client
      .expect[String](uri).recoverWith {
        case NonFatal(error) =>
          for {
            _ <- Applicative[F].pure(println(s"Error: ${error.getMessage}"))
            _ <- Applicative[F].pure(Thread.sleep(1000))
            retry <- readPage(uri)
          } yield retry
      }
  }

  private def stringToUri(strUri: String): Option[Uri] =
    Option(strUri)
      .filter(_.nonEmpty)
      .flatMap(Uri.fromString(_).toOption)

  private def readTitlePage(page: String): F[NovelInfo] = {
    for {
      delayed <- Concurrent[F].fromTry {
        Try {
          val html = Jsoup.parse(page)

          val title = html.select("h3.title").text()

          val firstChapter = html
            .select("ul.list-chapter li a")
            .asScala
            .headOption.flatMap(s => stringToUri(s.attr("href")))

          (title, firstChapter)
        }
      }

      (title, firstChapter) = delayed
      chapters <- buildNovel(firstChapter)
    } yield NovelInfo(title, chapters = chapters)
  }

  private def listFromPage(page: String): F[Seq[ChapterInfo]] = {
    for {
      delayed <- Concurrent[F].fromTry {
        Try {
          val html = Jsoup.parse(page)

          html.select("select.chr-jump option").asScala.flatMap { elm =>
            Try {
              val title = elm.text()
              val link = elm.`val`()

              ChapterInfo(None, title, link)
            }.toOption
          }.toSeq
        }
      }
    } yield delayed
  }

  private def buildNovel(link: Option[Uri]): F[Seq[NovelChapter]] = {
    link match {
      case Some(l) =>
        for {
          _ <- Concurrent[F].pure(())
          page <- readPage(l)
          novelChap <- parsePageToChapter(page)
          nextChapters <- buildNovel(novelChap.nextChapter)
        } yield novelChap +: nextChapters
      case None => Concurrent[F].pure(Seq.empty[NovelChapter])
    }
  }

  def f(chapter: (ChapterInfo, Int), ref: Ref[F, Seq[(Int, NovelChapter)]]) = {
    for {
      page <- readPage(stringToUri(chapter._1.link).get)
      novelChap <- parsePageToChapter(page)
      update <- ref.update(_ :+ (chapter._2, novelChap))
    } yield update
  }

  private def buildNovelFromChapterInfoRef(chapterInfo: Seq[ChapterInfo]): F[Seq[NovelChapter]] = {

    val ref = Ref[F].of(Seq.empty[(Int, NovelChapter)])

    for {
      r <- ref
      _ <- chapterInfo.zipWithIndex.grouped(2).map(_.map(f(_, r)).parSequence).toList.sequence
      result <- r.get
    } yield result.sortBy(_._1).map(_._2)
  }

  private def parsePageToChapter(page: String): F[NovelChapter] = {
    Concurrent[F].fromTry {
      Try {
        val html = Jsoup.parse(page)

        val nextChapter = stringToUri(html.getElementById("next_chap").attr("href").trim)
        val content = html.getElementById("chr-content").select("p").asScala.map(_.html()).mkString("<html><p>", "</p><p>", "</p></html>")
        val title = html.select("a.chr-title").text()

        NovelChapter(title, nextChapter = nextChapter, content = content)
      }
    }
  }

  def listNovels(novelUrl: String): F[File] = for {
    page <- readPage(stringToUri(s"$novelUrl#tab-chapters-title").get)
    novelInfo <- readTitlePage(page)
    file <- epubService.buildBook(novelInfo)
  } yield file

  def findNovelByName(novelName: String) = for {
    page <- readPage(stringToUri(s"https://novelbin.com/ajax/chapter-option?novelId=$novelName").get)
    chapterInfo <- listFromPage(page)
    chapters <- buildNovelFromChapterInfoRef(chapterInfo)
    novel <- epubService.buildBook(NovelInfo(novelName, chapters))
  } yield novel

}
