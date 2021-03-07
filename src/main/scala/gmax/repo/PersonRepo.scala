package gmax.repo

import cats.effect.{ContextShift, IO}
import com.typesafe.config.Config
import gmax.uri
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.ExecutionContextExecutor
import scala.util.Try

case class Person(_id: Int, name: String, age: Int, address: Option[Address] = None)

case class Address(street: String, no: Int, zip: Int)

sealed trait PersonDSL[F[_]] {
  type MongoResult[A] = Either[Throwable, A]

  def getPerson(id: Int): MongoResult[F[Option[Person]]]

  def getPersons: MongoResult[F[Seq[Person]]]

  def addPerson(person: Person): MongoResult[F[InsertOneResult]]

  def deletePerson(id: Int): MongoResult[F[DeleteResult]]

  def updatePerson(person: Person): MongoResult[F[UpdateResult]]
}

class PersonRepo(cfg: Config)(implicit ec: ExecutionContextExecutor) extends PersonDSL[IO] {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)

  private val mongoClient = MongoClient(uri(cfg))
  private val dbName = cfg.getString("person.db")
  private val collectionName = cfg.getString("person.table")

  private val codecRegistry: CodecRegistry = fromRegistries(fromProviders(classOf[Person], classOf[Address]), DEFAULT_CODEC_REGISTRY)

  def queryCollection[R](query: MongoCollection[Person] => R): Either[Throwable, R] =
    Try {
      val database: MongoDatabase = mongoClient.getDatabase(dbName).withCodecRegistry(codecRegistry)
      val collection: MongoCollection[Person] = database.getCollection(collectionName)
      query(collection)
    }.toEither

  def idEqual(objectId: Int): Bson =
    equal("_id", objectId)

  // equal("_id", new ObjectId(objectId))

  def getPersons: MongoResult[IO[Seq[Person]]] =
    queryCollection { collection =>
      collection.find().toFuture()
    } fold(
      e => Left(e),
      person => Right(IO.fromFuture(IO.delay(person)))
    )

  def getPerson(id: Int): MongoResult[IO[Option[Person]]] =
    queryCollection { collection =>
      collection.find(idEqual(id)).first().headOption
    } fold(
      e => Left(e),
      person => Right(IO.fromFuture(IO.delay(person)))
    )

  def addPerson(person: Person): MongoResult[IO[InsertOneResult]] =
    queryCollection { collection =>
      collection.insertOne(person).toFuture
    } fold(
      e => Left(e),
      result => Right(IO.fromFuture(IO.delay(result)))
    )

  def deletePerson(id: Int): MongoResult[IO[DeleteResult]] =
    queryCollection { collection =>
      collection.deleteOne(idEqual(id)).toFuture
    } fold(
      e => Left(e),
      result => Right(IO.fromFuture(IO.delay(result)))
    )

  def updatePerson(person: Person): MongoResult[IO[UpdateResult]] =
    queryCollection { collection =>
      collection.replaceOne(idEqual(person._id), person).toFuture
    } fold(
      e => Left(e),
      result => Right(IO.fromFuture(IO.delay(result)))
    )
}

object PersonRepo {
  def apply(cfg: Config)(implicit ec: ExecutionContextExecutor): PersonRepo =
    new PersonRepo(cfg)
}