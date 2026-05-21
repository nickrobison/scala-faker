package com.nickrobison.scalafaker

import org.scalacheck.Gen

object syntax {
  implicit class FakerGenOps(private val path: String) extends AnyVal {
    def asGen: Gen[String] = FakerGen.of(path)
  }
}
