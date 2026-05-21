package com.nickrobison.scalafaker

import org.yaml.snakeyaml.Yaml
import scala.jdk.CollectionConverters._
import scala.collection.concurrent.TrieMap
import scala.util.Using

class YamlRegistry private (locale: String) {
  import YamlRegistry._

  private val cache = TrieMap.empty[String, Map[String, Any]]

  def getProvider(name: String): Option[Map[String, Any]] = {
    cache.get(name) match {
      case s @ Some(_) => s
      case None => loadAndCache(name)
    }
  }

  def getField(provider: String, field: String): Option[Any] =
    getProvider(provider).flatMap(_.get(field))

  private def loadAndCache(name: String): Option[Map[String, Any]] = {
    val stream = classLoader.getResourceAsStream(s"$locale/$name.yml")
    if (stream == null) None
    else {
      Using.resource(stream) { s =>
        try {
          val yaml = new Yaml()
          val raw = yaml.load[java.util.Map[String, Any]](s)
          extractProvider(raw, name).map { data =>
            cache.put(name, data)
            data
          }
        } catch {
          case _: Exception => None
        }
      }
    }
  }

  private def extractProvider(raw: Any, name: String): Option[Map[String, Any]] = {
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

object YamlRegistry {
  private val instances = TrieMap.empty[String, YamlRegistry]
  private val classLoader = classOf[YamlRegistry].getClassLoader

  def apply(): YamlRegistry = apply("en")

  def apply(locale: String): YamlRegistry =
    instances.getOrElseUpdate(locale, new YamlRegistry(locale))
}
