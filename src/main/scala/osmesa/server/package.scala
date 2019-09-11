package osmesa

import cats.implicits._
import doobie.util.meta.Meta
import io.circe.Json
import io.circe.generic.extras.Configuration
import io.circe.parser.parse
import org.postgresql.util.PGobject

package object server {
  trait Implicits {
    implicit val CustomConfig: Configuration =
      Configuration.default.withSnakeCaseMemberNames.withDefaults

  }

  implicit final val JsonMeta: Meta[Json] =
    Meta
      .other[PGobject]("json")
      .xmap[Json](
        a => parse(a.getValue).leftMap[Json](e => throw e).merge,
        a => {
          val o = new PGobject
          o.setType("json")
          o.setValue(a.noSpaces)
          o
        }
      )
}
