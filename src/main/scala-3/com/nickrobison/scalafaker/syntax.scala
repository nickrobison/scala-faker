package com.nickrobison.scalafaker

import org.scalacheck.Gen

object syntax:
  extension (path: String)
    /** Convenience syntax: `"name.male_first_name".asGen`.
      *
      * Equivalent to `FakerGen.of("name.male_first_name")`.
      */
    def asGen(implicit ctx: FakerContext): Gen[String] = FakerGen.of(path)
