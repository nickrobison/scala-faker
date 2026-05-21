package com.nickrobison.scalafaker

import java.util.Locale
import org.scalacheck.{Gen, Prop, Test}
import org.scalacheck.Prop._

object FakerGenSpec {
  private val seed = org.scalacheck.rng.Seed(42)

  def main(args: Array[String]): Unit = {
    val results = Seq(
      "name.male_first_name" -> testSimple("name.male_first_name"),
      "name.female_first_name" -> testSimple("name.female_first_name"),
      "name.first_name" -> testFirstName,
      "name.last_name" -> testSimple("name.last_name"),
      "name.name" -> testName,
      "name.prefix" -> testSimple("name.prefix"),
      "name.suffix" -> testSimple("name.suffix"),
      "address.country" -> testSimple("address.country"),
      "address.city" -> testSimple("address.city"),
      "address.street_name" -> testSimple("address.street_name"),
      "address.building_number" -> testPattern("address.building_number"),
      "address.postcode" -> testPattern("address.postcode"),
      "address.secondary_address" -> testPattern("address.secondary_address"),
      "address.full_address" -> testFullAddress,
      "address.zip_code" -> testPattern("address.zip_code"),
      "address.full_address (fr)" -> testFrenchAddress,
      "book.title" -> testSimple("book.title"),
      "book.author" -> testSimple("book.author"),
      "company.name" -> testSimple("company.name"),
      "internet.free_email" -> testSimple("internet.free_email"),
      "internet.domain_suffix" -> testSimple("internet.domain_suffix"),
      "lorem.words" -> testSimple("lorem.words"),
      "movie.name" -> testSimple("movie.name"),
      "syntax extension" -> testSyntax,
      "determinism" -> testDeterminism,
      "german locale" -> testGerman,
      "fallback to english" -> testFallbackToEnglish,
      "explicit context" -> testExplicitContext
    )

    results.foreach { case (name, result) =>
      println(s"[$name] ${if (result) "PASS" else "FAIL"}")
    }

    val failures = results.count(!_._2)
    println(s"\n${results.size - failures}/${results.size} passed")
    if (failures > 0) sys.exit(1)
  }

  private def sample(gen: Gen[String], samples: Int = 20): Seq[String] =
    Gen.listOfN(samples, gen).pureApply(Gen.Parameters.default, seed, 100)

  private def testSimple(path: String): Boolean = {
    val gen = FakerGen.of(path)
    val values = sample(gen)
    val allNonEmpty = values.forall(_.nonEmpty)
    println(s"  samples: ${values.take(5).mkString(", ")}")
    allNonEmpty
  }

  private def testFirstName: Boolean = {
    val gen = FakerGen.of("name.first_name")
    val values = sample(gen, 100)
    val allNonEmpty = values.forall(_.nonEmpty)
    val hasShortNames = values.exists(_.length <= 4)
    val hasLongNames = values.exists(_.length >= 8)
    println(s"  samples: ${values.distinct.take(8).mkString(", ")}")
    println(s"  total distinct: ${values.distinct.size}")
    allNonEmpty && hasShortNames && hasLongNames
  }

  private def testName: Boolean = {
    val gen = FakerGen.of("name.name")
    val values = sample(gen)
    val allHaveSpace = values.forall(_.contains(' '))
    println(s"  samples: ${values.take(5).mkString(", ")}")
    allHaveSpace
  }

  private def testPattern(path: String): Boolean = {
    val gen = FakerGen.of(path)
    val values = sample(gen)
    val noHashLeft = values.forall(v => !v.contains('#') && !v.contains('?'))
    println(s"  samples: ${values.take(5).mkString(", ")}")
    noHashLeft
  }

  private def testFullAddress: Boolean = {
    val gen = FakerGen.of("address.full_address")
    val values = sample(gen)
    val allNonEmpty = values.forall(_.nonEmpty)
    val hasComma = values.exists(_.contains(","))
    println(s"  samples: ${values.take(5).mkString(", ")}")
    allNonEmpty && hasComma
  }

  private def testFrenchAddress: Boolean = {
    implicit val ctx: FakerContext = FakerContext(Locale.FRANCE)
    val gen = FakerGen.of("address.full_address")
    val values = sample(gen)
    val allNonEmpty = values.forall(_.nonEmpty)
    val hasDigit = values.exists(_.exists(_.isDigit))
    println(s"  samples: ${values.take(5).mkString(", ")}")
    allNonEmpty && hasDigit
  }

  private def testSyntax: Boolean = {
    import syntax._
    val gen = "name.male_first_name".asGen
    val values = sample(gen)
    val ok = values.forall(_.nonEmpty)
    println(s"  samples: ${values.take(5).mkString(", ")}")
    ok
  }

  private def testDeterminism: Boolean = {
    val gen = FakerGen.of("name.male_first_name")
    val s1 = Gen.listOfN(10, gen).pureApply(Gen.Parameters.default, org.scalacheck.rng.Seed(12345), 100)
    val s2 = Gen.listOfN(10, gen).pureApply(Gen.Parameters.default, org.scalacheck.rng.Seed(12345), 100)
    val s3 = Gen.listOfN(10, gen).pureApply(Gen.Parameters.default, org.scalacheck.rng.Seed(99999), 100)
    println(s"  seed=12345: ${s1.take(3).mkString(", ")}")
    println(s"  seed=12345: ${s2.take(3).mkString(", ")}")
    println(s"  seed=99999: ${s3.take(3).mkString(", ")}")
    s1 == s2 && s1 != s3
  }

  private def testGerman: Boolean = {
    implicit val ctx: FakerContext = FakerContext(Locale.GERMANY)
    val gen = FakerGen.of("name.prefix")
    val values = sample(gen)
    val hasGermanPrefix = values.exists(v => v == "Hr." || v == "Fr." || v == "Dipl.-Ing.")
    println(s"  samples: ${values.distinct.take(8).mkString(", ")}")
    hasGermanPrefix
  }

  private def testFallbackToEnglish: Boolean = {
    implicit val ctx: FakerContext = FakerContext(Locale.GERMANY)
    val gen = FakerGen.of("movie.name")
    val values = sample(gen)
    val allNonEmpty = values.forall(_.nonEmpty)
    println(s"  samples: ${values.take(5).mkString(", ")}")
    allNonEmpty
  }

  private def testExplicitContext: Boolean = {
    val ctx = FakerContext(Locale.GERMANY)
    val gen = FakerGen.of("name.prefix")(ctx)
    val values = sample(gen)
    val hasGermanPrefix = values.exists(v => v == "Hr." || v == "Fr.")
    println(s"  samples: ${values.distinct.take(8).mkString(", ")}")
    hasGermanPrefix
  }
}
