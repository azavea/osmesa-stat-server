package osmesa.server.model

import io.circe._


trait OsmStatError

case class UnknownError(message: String) extends OsmStatError {
  override def toString = s"Unknown error: $message"
}

case class IdNotFoundError[ID](recordType: String, id: ID) extends OsmStatError {
  override def toString = s"Unable to retrieve ${recordType} record at ${id}"
}

