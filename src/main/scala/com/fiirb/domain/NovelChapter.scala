package com.fiirb.domain

import org.http4s.Uri

case class NovelInfo(title: String, chapters: Seq[NovelChapter])
case class NovelChapter(title: String, nextChapter: Option[Uri], content: String)
