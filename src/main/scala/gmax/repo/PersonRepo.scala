package gmax.repo

import cats.tagless.{Derive, FunctorK}
import cats.~>
import com.typesafe.config.Config
import gmax.uri
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.result._
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

case class Person(_id: Int, name: String, age: Int, address: Option[Address] = None)

case class Address(street: String, no: Int, zip: Int)

trait MongoResult {
  type Result[F[_], A] = Either[Throwable, F[A]]
}

trait PersonDSL[F[_]] extends MongoResult {
  def getPerson(id: Int): Result[F, Option[Person]]

  def getPersons: Result[F, Seq[Person]]

  def addPerson(person: Person): Result[F, InsertOneResult]

  def deletePerson(id: Int): Result[F, DeleteResult]

  def updatePerson(person: Person): Result[F, UpdateResult]
}

trait PersonK extends MongoResult {
  type InsertOneResultK[F[_]] = Result[F, InsertOneResult]
  type UpdateResultK[F[_]] = Result[F, UpdateResult]
  type DeleteResultK[F[_]] = Result[F, DeleteResult]
  type SeqResultK[F[_]] = Result[F, Seq[Person]]
  type OptionResultK[F[_]] = Result[F, Option[Person]]

  implicit object updateResultK extends FunctorK[UpdateResultK] {
    override def mapK[F[_], G[_]](af: UpdateResultK[F])(fk: F ~> G): UpdateResultK[G] =
      af.map(fk(_))
  }

  implicit object deleteResultK extends FunctorK[DeleteResultK] {
    override def mapK[F[_], G[_]](af: DeleteResultK[F])(fk: F ~> G): DeleteResultK[G] =
      af.map(fk(_))
  }

  implicit object insertOneResultK extends FunctorK[InsertOneResultK] {
    override def mapK[F[_], G[_]](af: InsertOneResultK[F])(fk: F ~> G): InsertOneResultK[G] =
      af.map(fk(_))
  }

  implicit object seqResultK extends FunctorK[SeqResultK] {
    override def mapK[F[_], G[_]](af: SeqResultK[F])(fk: F ~> G): SeqResultK[G] =
      af.map(fk(_))
  }

  implicit object optionResultK extends FunctorK[OptionResultK] {
    override def mapK[F[_], G[_]](af: OptionResultK[F])(fk: F ~> G): OptionResultK[G] =
      af.map(fk(_))
  }

  implicit val functorK: FunctorK[PersonDSL] = Derive.functorK[PersonDSL]
}

class PersonRepo(cfg: Config)(implicit ec: ExecutionContextExecutor) extends PersonDSL[Future] {

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

  def getPersons: Result[Future, Seq[Person]] =
    queryCollection(_.find().toFuture)

  def getPerson(id: Int): Result[Future, Option[Person]] =
    queryCollection(_.find(idEqual(id)).first().headOption)

  def addPerson(person: Person): Result[Future, InsertOneResult] =
    queryCollection(_.insertOne(person).toFuture)

  def deletePerson(id: Int): Result[Future, DeleteResult] =
    queryCollection(_.deleteOne(idEqual(id)).toFuture)

  def updatePerson(person: Person): Result[Future, UpdateResult] =
    queryCollection(_.replaceOne(idEqual(person._id), person).toFuture)
}

object PersonRepo {
  def apply(cfg: Config)(implicit ec: ExecutionContextExecutor): PersonRepo =
    new PersonRepo(cfg)
}
