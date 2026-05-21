package com.nickrobison.scalafaker

import org.yaml.snakeyaml.Yaml
import scala.jdk.CollectionConverters._
import scala.collection.concurrent.TrieMap
import scala.util.Using

object YamlRegistry {
  private val cache = TrieMap.empty[String, Map[String, Any]]
  private val classLoader = classOf[YamlRegistry.type].getClassLoader

  def getProvider(name: String, locale: String): Option[Map[String, Any]] = {
    val candidates = localeCandidates(locale).distinct
    val merged = candidates.reverse
      .flatMap { loc =>
        cache.get(s"$loc/$name").orElse { loadAndCache(name, loc) }
      }
      .foldLeft(Map.empty[String, Any]) { (acc, data) =>
        acc ++ data
      }
    if (merged.nonEmpty) Some(merged) else None
  }

  def getField(provider: String, field: String, locale: String): Option[Any] =
    getProvider(provider, locale).flatMap(_.get(field))

  private def localeCandidates(locale: String): Seq[String] = {
    val base = locale.split("-").head
    if (locale == "en") Seq("en")
    else Seq(locale, base, "en")
  }

  private def loadAndCache(name: String, locale: String): Option[Map[String, Any]] = {
    loadFromDir(name, locale).orElse(loadFromFile(name, locale)).map { data =>
      cache.put(s"$locale/$name", data)
      data
    }
  }

  private def loadFromDir(name: String, locale: String): Option[Map[String, Any]] = {
    val stream = classLoader.getResourceAsStream(s"$locale/$name.yml")
    if (stream == null) None
    else {
      Using.resource(stream) { s =>
        try {
          val yaml = new Yaml()
          val raw = yaml.load[java.util.Map[String, Any]](s)
          extractProvider(raw, locale, name)
        } catch {
          case _: Exception => None
        }
      }
    }
  }

  private def loadFromFile(name: String, locale: String): Option[Map[String, Any]] = {
    val stream = classLoader.getResourceAsStream(s"$locale.yml")
    if (stream == null) None
    else {
      Using.resource(stream) { s =>
        try {
          val yaml = new Yaml()
          val raw = yaml.load[java.util.Map[String, Any]](s)
          extractProvider(raw, locale, name)
        } catch {
          case _: Exception => None
        }
      }
    }
  }

  private def extractProvider(raw: Any, locale: String, name: String): Option[Map[String, Any]] = {
    try {
      val root = raw.asInstanceOf[java.util.Map[String, Any]].asScala
      val localeData = root(locale).asInstanceOf[java.util.Map[String, Any]].asScala
      val faker = localeData("faker").asInstanceOf[java.util.Map[String, Any]].asScala
      faker.get(name).map { v =>
        v.asInstanceOf[java.util.Map[String, Any]].asScala.toMap
      }
    } catch {
      case _: Exception => None
    }
  }
}
