package gmax.json

import io.circe.Json

sealed trait KVJsonCodec[T] {
  def kv(key: String, value: T): Json
}

object KVJson {

  implicit object kvString extends KVJsonCodec[String] {
    def kv(key: String, value: String): Json = Json.obj(
      (key, Json.fromString(value))
    )
  }

  implicit object kvInt extends KVJsonCodec[Int] {
    def kv(key: String, value: Int): Json = Json.obj(
      (key, Json.fromInt(value))
    )
  }

  implicit object kvThrowable extends KVJsonCodec[Throwable] {
    def kv(key: String, value: Throwable): Json = Json.obj(
      (key, Json.fromString(value.getMessage))
    )
  }

  def kvJson[A](key: String, value: A)(implicit F: KVJsonCodec[A]): Json =
    F.kv(key, value)
}
