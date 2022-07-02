package com.fiirb.services

import cats.{Applicative, ApplicativeError}
import cats.effect.Async
import cats.implicits._
import com.fiirb.controller.ExternalService
import com.fiirb.domain.{NovelChapter, NovelInfo}
import org.http4s.Uri
import org.http4s.client.Client
import org.jsoup.Jsoup

import java.io.File
import scala.jdk.CollectionConverters.CollectionHasAsScala

class NovelService[F[_] : Async : Applicative](client: Client[F], epubService: EpubService[F]) extends ExternalService[F] {

  private def readPage(uri: Uri)(implicit F: ApplicativeError[F, Throwable]): F[String] = {
    client
      .expect[String](uri)
  }

  private def stringToUri(strUri: String): Option[Uri] =
    Option(strUri)
      .filter(_.nonEmpty)
      .flatMap(Uri.fromString(_).toOption)

  private def readTitlePage(page: String): F[NovelInfo] = {
    for {
      delayed <- Async[F].delay {
        val html = Jsoup.parse(page)

        val title = html.select("h3.title").text()

        val firstChapter = html
          .select("ul.list-chapter li a")
          .asScala
          .headOption.flatMap(s => stringToUri(s.attr("href")))

        (title, firstChapter)
      }

      (title, firstChapter) = delayed
      chapters <- buildNovel(firstChapter)
    } yield NovelInfo(title, chapters = chapters)
  }

  private def buildNovel(link: Option[Uri]): F[Seq[NovelChapter]] = {
    link match {
      case Some(l) =>
        for {
          page <- readPage(l)
          novelChap <- parsePageToChapter(page)
          nextChapters <- buildNovel(novelChap.nextChapter)
        } yield novelChap +: nextChapters
      case None => Async[F].pure(Seq.empty[NovelChapter])
    }
  }

  private def parsePageToChapter(page: String): F[NovelChapter] = {
    Async[F].delay {
      val html = Jsoup.parse(page)

      val nextChapter = stringToUri(html.getElementById("next_chap").attr("href").trim)
      val content = html.getElementById("chr-content").select("p").asScala.map(_.html()).mkString("<html><p>", "</p><p>", "</p></html>")
      val title = html.select("a.chr-title").text()

      NovelChapter(title, nextChapter = nextChapter, content = content)
    }
  }

  def listNovels(novelUrl: String): F[File] = for {
    page <- readPage(stringToUri(s"$novelUrl#tab-chapters-title").get)
    novelInfo <- readTitlePage(page)
    file <- epubService.buildBook(novelInfo)
  } yield file
}
