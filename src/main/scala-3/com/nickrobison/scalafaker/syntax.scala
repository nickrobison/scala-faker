package com.nickrobison.scalafaker

import org.scalacheck.Gen

object syntax:
  extension (path: String) def asGen: Gen[String] = FakerGen.of(path)
