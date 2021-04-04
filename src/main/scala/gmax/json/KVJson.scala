package gmax.json

import io.circe.Json

sealed trait KVJsonCodec[T] {
  def kv(key: String, value: T): Json
}

object KVJson {

  implicit lazy val kvString: KVJsonCodec[String] = new KVJsonCodec[String] {
    def kv(key: String, value: String): Json = Json.obj(
      (key, Json.fromString(value))
    )
  }

  implicit lazy val kvInt: KVJsonCodec[Int] = new KVJsonCodec[Int] {
    def kv(key: String, value: Int): Json = Json.obj(
      (key, Json.fromInt(value))
    )
  }

  implicit lazy val kvThrowable: KVJsonCodec[Throwable] = new KVJsonCodec[Throwable] {
    def kv(key: String, value: Throwable): Json = Json.obj(
      (key, Json.fromString(value.getMessage))
    )
  }

  def kvJson[A](key: String, value: A)(implicit F: KVJsonCodec[A]): Json =
    F.kv(key, value)
}
