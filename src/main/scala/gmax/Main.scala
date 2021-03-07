package gmax

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.config.ConfigFactory
import gmax.repo.PersonRepo
import gmax.routes.{HealthRoutes, PersonRoutes}
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig, GZip}
import org.http4s.{Http, Request, Response}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object Main extends IOApp {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  private val cfg = ConfigFactory.load("application.conf")
  private val personRepo: PersonRepo = PersonRepo(cfg.getConfig("db"))

  private val routes: Kleisli[IO, Request[IO], Response[IO]] = Router[IO](
    "/api" -> PersonRoutes.personRoutes(personRepo),
    "/.well-known" -> HealthRoutes.healthRoutes
  ).orNotFound

  private val methodConfig: CORSConfig = cors(cfg.getConfig("cors"))
  private val corsRoutes: Http[IO, IO] = GZip(CORS(routes, methodConfig))

  override def run(args: List[String]): IO[ExitCode] = {

    if (cfg.getBoolean("app.ssl")) {
      BlazeServerBuilder[IO](ec)
        .bindHttp(cfg.getInt("https.port"), cfg.getString("https.host"))
        .withHttpApp(corsRoutes)
        .withSslContext(ssl(cfg.getConfig("https.ssl")))
        .serve
        .compile
        .drain
        .as(ExitCode.Success)

    } else {
      BlazeServerBuilder[IO](ec)
        .bindHttp(cfg.getInt("http.port"), cfg.getString("http.host"))
        .withHttpApp(corsRoutes)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }
}