package com.fiirb.domain

case class ChapterInfo(id: Option[Long] = None, title: String, link: String) {

  def withId(id: Long): ChapterInfo = copy(id = Some(id))

}
