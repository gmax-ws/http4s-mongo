package gmax

import cats.data.Kleisli
import cats.effect._
import cats.tagless.implicits._
import cats.~>
import com.typesafe.config.ConfigFactory
import gmax.repo.{PersonDSL, PersonK, PersonRepo}
import gmax.routes.{HealthRoutes, PersonRoutes}
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, CORSConfig, GZip}
import org.http4s.{Http, Request, Response}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object Main extends IOApp with PersonK {
  implicit val global: ExecutionContextExecutor = ExecutionContext.global

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  private val fk: Future ~> IO = new (Future ~> IO) {
    def apply[A](t: Future[A]): IO[A] = IO.fromFuture(IO.delay(t))
  }

  private val cfg = ConfigFactory.load("application.conf")
  private val personRepo: PersonDSL[Future] = PersonRepo(cfg.getConfig("db"))
  private val personRepoIO: PersonDSL[IO] = personRepo.mapK(fk)

  private val routes: Kleisli[IO, Request[IO], Response[IO]] = Router[IO](
    "/api" -> PersonRoutes.personRoutes(personRepoIO),
    "/.well-known" -> HealthRoutes.healthRoutes
  ).orNotFound

  private val methodConfig: CORSConfig = cors(cfg.getConfig("cors"))
  private val corsRoutes: Http[IO, IO] = GZip(CORS(routes, methodConfig))

  override def run(args: List[String]): IO[ExitCode] = {

    if (cfg.getBoolean("app.ssl")) {
      BlazeServerBuilder[IO](global)
        .bindHttp(cfg.getInt("https.port"), cfg.getString("https.host"))
        .withHttpApp(corsRoutes)
        .withSslContext(ssl(cfg.getConfig("https.ssl")))
        .serve
        .compile
        .drain
        .as(ExitCode.Success)

    } else {
      BlazeServerBuilder[IO](global)
        .bindHttp(cfg.getInt("http.port"), cfg.getString("http.host"))
        .withHttpApp(corsRoutes)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
  }
}