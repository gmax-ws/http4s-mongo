package gmax.routes

import cats.effect.IO
import gmax.json.KVJson._
import gmax.repo.{Person, PersonRepo}
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

sealed trait Routes extends Http4sDsl[IO]

object HealthRoutes extends Routes {
  def healthRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "ready" => Ok("ready")
    case GET -> Root / "live" => Ok("live")
  }
}

object PersonRoutes extends Routes {

  def personRoutes(personRepo: PersonRepo): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "persons" =>
      personRepo.getPersons.fold(
        error => NotFound(kvJson("message", error)),
        persons => Ok(persons)
      )

    case req@POST -> Root / "person" =>
      req.decode[Person] { person =>
        personRepo.addPerson(person).fold(
          error => NotFound(kvJson("message", error)),
          result => Created(result.map(_.wasAcknowledged()))
        )
      }

    case GET -> Root / "person" / IntVar(id) =>
      personRepo.getPerson(id).fold(
        error => NotFound(kvJson("message", error)),
        person => person flatMap {
          case Some(p) => Ok(p)
          case None => NotFound(kvJson("message", s"Person $id not found!"))
        }
      )

    case req@PUT -> Root / "person" =>
      req.decode[Person] { person =>
        personRepo.updatePerson(person).fold(
          error => NotFound(kvJson("message", error)),
          result => Ok(result.map(_.getModifiedCount))
        )
      }

    case DELETE -> Root / "person" / IntVar(id) =>
      personRepo.deletePerson(id).fold(
        error => NotFound(kvJson("message", error)),
        result => Ok(result.map(_.getDeletedCount))
      )
  }
}