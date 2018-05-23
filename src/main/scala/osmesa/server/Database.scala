package osmesa.server

import cats.effect.IO
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

object Database {

  def transactor(dbconfig: Config.Database): IO[HikariTransactor[IO]] = {
    HikariTransactor.newHikariTransactor[IO](dbconfig.driver, dbconfig.url, dbconfig.user, dbconfig.password)
  }

  def initialize(transactor: HikariTransactor[IO]): IO[Unit] = {
    transactor.configure { datasource =>
      IO {
        val flyWay = new Flyway()
        flyWay.setDataSource(datasource)
        flyWay.migrate()
      }
    }
  }
}
