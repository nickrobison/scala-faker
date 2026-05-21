package com.nickrobison.scalafaker

import org.scalacheck.Gen

object syntax {
  implicit class FakerGenOps(private val path: String) extends AnyVal {

    /** Convenience syntax: `"name.male_first_name".asGen`.
      *
      * Equivalent to `FakerGen.of("name.male_first_name")`.
      */
    def asGen(implicit ctx: FakerContext): Gen[String] = FakerGen.of(path)
  }
}
