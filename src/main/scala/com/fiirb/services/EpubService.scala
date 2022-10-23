package com.fiirb.services


import cats.Monad
import cats.effect.MonadCancelThrow
import com.fiirb.domain.NovelInfo
import coza.opencollab.epub.creator.EpubConstants
import coza.opencollab.epub.creator.model.{Content, EpubBook}

import java.io.{File, FileOutputStream}
import java.text.MessageFormat
import java.util.UUID
import scala.util.Try

class EpubService[F[_]: MonadCancelThrow] {

  def buildBook(info: NovelInfo): F[File] = {
    println(s"Building book base: ${info.title}")
    val book = new EpubBook("en", info.title, info.title, "Fiirb")
    //      book.addCoverImage(info.image,
    //        "image/jpeg", "images/coverImage.jpg")

    MonadCancelThrow[F].fromTry {
      Try {
        info.chapters.map { chapter =>
          val title = chapter.title
          println(s"writing chapter: $title")
          val formattedContent = {
            MessageFormat
              .format(EpubConstants.HTML_WRAPPER, chapter.nextChapter,
                chapter.content)
          }

          val content =
            new Content("application/xhtml+xml",
              s"html/${book.getTitle}/${UUID.randomUUID()}.htm",
              formattedContent.getBytes())

          content.setId(title)
          content.setToc(true)
          content.setSpine(true)

          book.addContent(content)
        }

        val file = new File(s"${book.getTitle}.epub")

        println(s"writing novel to file: ${file.getAbsolutePath}")
        val fileStream = new FileOutputStream(file)

        book.writeToStream(fileStream)

        fileStream.close()
        println(s"file saved")

        file
      }
    }
  }

}
