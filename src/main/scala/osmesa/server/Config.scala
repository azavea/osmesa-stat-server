package osmesa.server

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.error.ConfigReaderException

case class Config(server: Config.Server,
                  database: Config.Database,
                  tiles: Config.Tiles)

object Config {
  def load(configFile: String = "application.conf"): IO[Config] = {
    IO {
      loadConfig[Config](ConfigFactory.load(configFile))
    }.flatMap {
      case Left(e) =>
        IO.raiseError[Config](new ConfigReaderException[Config](e))
      case Right(config) => IO.pure(config)
    }
  }

  case class Database(driver: String,
                      url: String,
                      user: String,
                      password: String)

  case class Server(host: String, port: Int)

  case class Tiles(s3bucket: String,
                   s3prefix: String,
                   s3suffix: Option[String],
                   chunkSize: Int)
}
