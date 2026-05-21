# scala-faker

ScalaCheck generators backed by datafaker YAML data.

## Installation

```scala
// build.sbt
libraryDependencies += "com.nickrobison" %% "scala-faker" % "@VERSION@"
```

## Usage

### Basic generator

```scala mdoc:reset
import org.scalacheck.Gen
import com.nickrobison.scalafaker.FakerGen

val gen = FakerGen.of("name.male_first_name")
gen.pureApply(Gen.Parameters.default, org.scalacheck.rng.Seed(1))
```

### Composite fields

Fields like `name.first_name` merge `male_first_name` and
`female_first_name` into a single generator:

```scala mdoc:reset
import org.scalacheck.Gen
import com.nickrobison.scalafaker.FakerGen

val gen = FakerGen.of("name.first_name")
gen.pureApply(Gen.Parameters.default, org.scalacheck.rng.Seed(1))
```

### Pattern fields

Fields containing `#` (digit), `?` (letter), or `Ø` (non-zero digit)
are expanded automatically:

```scala mdoc:reset
import org.scalacheck.Gen
import com.nickrobison.scalafaker.FakerGen

val gen = FakerGen.of("address.building_number")
gen.pureApply(Gen.Parameters.default, org.scalacheck.rng.Seed(1))
```

### Syntax extension

```scala mdoc:reset
import org.scalacheck.Gen
import com.nickrobison.scalafaker.syntax._

"name.male_first_name".asGen
  .pureApply(Gen.Parameters.default, org.scalacheck.rng.Seed(1))
```

### Locale support

Pass an implicit `FakerContext` to select locale-specific YAML data.
Fields not overridden by the locale fall back to English.

```scala mdoc:reset
import org.scalacheck.Gen
import com.nickrobison.scalafaker.FakerGen
import com.nickrobison.scalafaker.FakerContext
import java.util.Locale

implicit val ctx: FakerContext = FakerContext(Locale.GERMANY)
FakerGen.of("name.prefix")
  .pureApply(Gen.Parameters.default, org.scalacheck.rng.Seed(1))
```
