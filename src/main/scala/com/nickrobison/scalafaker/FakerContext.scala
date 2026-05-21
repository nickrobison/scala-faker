package com.nickrobison.scalafaker

import java.util.Locale

/** Controls locale and future generation parameters.
  *
  * An implicit `FakerContext` is required by [[FakerGen.of]] and
  * `syntax.asGen`. The companion provides a default using
  * `Locale.getDefault()`.
  *
  * @param locale
  *   the locale used to resolve YAML data (language part only)
  */
case class FakerContext(locale: Locale) {

  /** ISO language code (e.g. "en", "de"). */
  def language: String = locale.getLanguage
}

object FakerContext {

  /** Default context using the JVM's system locale. */
  implicit val default: FakerContext = FakerContext(Locale.getDefault())
}
