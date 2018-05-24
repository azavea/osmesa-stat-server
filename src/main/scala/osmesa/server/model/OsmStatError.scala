package osmesa.server.model

import io.circe._


sealed trait OsmStatError
case class IdNotFoundError[ID](recordType: String, id: ID) extends OsmStatError {
  override def toString = s"Unable to retrieve ${recordType} record at ${id}"
}

