package com.nickrobison.scalafaker

import org.scalacheck.Gen
import scala.jdk.CollectionConverters._

object FakerGen {
  private val registry = YamlRegistry
  private val camelToSnakeCache = scala.collection.concurrent.TrieMap.empty[String, String]

  /** Create a ScalaCheck `Gen[String]` from a datafaker YAML path.
    *
    * The path is a dot-separated provider and field, e.g.
    * `"name.male_first_name"`. Composite fields like `"name.first_name"`
    * merge data from multiple YAML keys.
    *
    * @param path
    *   dot-separated provider name and field name
    * @param ctx
    *   implicit generation context that determines locale
    */
  def of(path: String)(implicit ctx: FakerContext): Gen[String] = {
    val dot = path.indexOf('.')
    if (dot == -1) {
      throw new IllegalArgumentException(
        s"Path must be in format 'provider.field', got: $path"
      )
    }
    val provider = path.substring(0, dot)
    val field = path.substring(dot + 1)
    buildGen(provider, field)
  }

  private def buildGen(provider: String, field: String)(implicit ctx: FakerContext): Gen[String] = {
    compositeGen(provider, field).getOrElse {
      registry.getField(provider, field, ctx.language) match {
        case Some(list: java.util.List[_]) =>
          val strings = list.asScala.toSeq.map(_.toString)
          genFromValues(provider, strings)
        case Some(s: String) =>
          resolveExpression(provider, s)
        case Some(map: java.util.Map[_, _]) =>
          val strings = map.values.asScala.toSeq.map(_.toString)
          genFromValues(provider, strings)
        case Some(other) =>
          resolveExpression(provider, other.toString)
        case None =>
          Gen.fail
      }
    }
  }

  private def genFromValues(provider: String, values: Seq[String])(implicit ctx: FakerContext): Gen[String] = {
    if (values.isEmpty) Gen.const("")
    else if (values.size == 1) resolveExpression(provider, values.head)
    else Gen.oneOf(values).flatMap(v => resolveExpression(provider, v))
  }

  private def resolveExpression(provider: String, s: String)(implicit ctx: FakerContext): Gen[String] = {
    if (!s.contains("#{")) {
      patternify(s)
    } else {
      val parts = splitExpr(s)
      val gens = parts.map {
        case Left(static) => patternify(static)
        case Right(expr) => resolveReference(provider, expr)
      }
      gens.foldLeft(Gen.const("")) { (acc, gen) =>
        for {
          prefix <- acc
          next <- gen
        } yield prefix + next
      }
    }
  }

  private def splitExpr(s: String): Seq[Either[String, String]] = {
    val result = Seq.newBuilder[Either[String, String]]
    val sb = new StringBuilder()
    var i = 0
    while (i < s.length) {
      if (i < s.length - 2 && s.charAt(i) == '#' && s.charAt(i + 1) == '{') {
        if (sb.nonEmpty) {
          result += Left(sb.toString)
          sb.clear()
        }
        i += 2
        val exprStart = i
        while (i < s.length && s.charAt(i) != '}') {
          i += 1
        }
        if (i < s.length) {
          result += Right(s.substring(exprStart, i))
          i += 1
        } else {
          sb.append(s.substring(exprStart - 2))
        }
      } else {
        sb += s.charAt(i)
        i += 1
      }
    }
    if (sb.nonEmpty) {
      result += Left(sb.toString)
    }
    result.result()
  }

  private def resolveReference(provider: String, expr: String)(implicit ctx: FakerContext): Gen[String] = {
    val dot = expr.indexOf('.')
    if (dot == -1) {
      buildGen(provider, expr)
    } else {
      val providerRef = expr.substring(0, dot)
      val fieldRef = expr.substring(dot + 1)
      val yamlProvider = camelToSnake(providerRef)
      buildGen(yamlProvider, fieldRef)
    }
  }

  private def patternify(s: String): Gen[String] = {
    if (!s.exists(c => c == '#' || c == '?' || c == 'Ø')) {
      Gen.const(s)
    } else {
      val charGens: Seq[Gen[Char]] = s.map {
        case '#' => Gen.choose('0', '9')
        case 'Ø' => Gen.choose('1', '9')
        case '?' => Gen.alphaLowerChar
        case c => Gen.const(c)
      }
      charGens.foldLeft(Gen.const("")) { (acc, gen) =>
        for {
          prefix <- acc
          next <- gen
        } yield prefix + next
      }
    }
  }

  private def compositeGen(provider: String, field: String)(implicit ctx: FakerContext): Option[Gen[String]] = {
    (provider, field) match {
      case ("name", "first_name") =>
        Some(
          Gen.oneOf(
            buildGen("name", "male_first_name"),
            buildGen("name", "female_first_name")
          )
        )
      case _ => None
    }
  }

  private def camelToSnake(s: String): String =
    camelToSnakeCache.getOrElseUpdate(
      s, {
        s.flatMap { c =>
          if (c.isUpper) "_" + c.toLower
          else c.toLower.toString
        }.stripPrefix("_")
      }
    )
}
