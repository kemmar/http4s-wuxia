package com.fiirb.db

import cats.Monad
import cats.effect.{Async, IO}
import cats.effect.kernel.{Resource, Sync}
import com.fiirb.config.DatabaseConfig
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway

import scala.concurrent.ExecutionContext

object Database {
  def transactor[F[_]](config: DatabaseConfig, executionContext: ExecutionContext)(implicit contextShift: Async[F]): Resource[F, HikariTransactor[F]] = {
    HikariTransactor.newHikariTransactor[F](
      config.driver,
      config.url,
      config.user,
      config.password,
      executionContext
    )
  }

  def initialize[F[_]](transactor: HikariTransactor[F])(implicit F: Monad[F]): F[Unit] = {
    transactor.configure { dataSource =>
      F.pure {
        val flyWay = Flyway.configure().dataSource(dataSource).load()
        flyWay.migrate()
        ()
      }
    }
  }
}