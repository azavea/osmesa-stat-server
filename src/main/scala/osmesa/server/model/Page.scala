package osmesa.server.model

import io.circe._
import io.circe.generic.semiauto._


case class ResultPage[RESULT](
  results: List[RESULT],
  page: Int
)

object ResultPage {
  implicit def resultPageDecoder[RESULT: Decoder]: Decoder[ResultPage[RESULT]] = deriveDecoder
  implicit def resultPageEncoder[RESULT: Encoder]: Encoder[ResultPage[RESULT]] = deriveEncoder
}
