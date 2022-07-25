package com.tessellation.demo.modules

import cats.effect.{Async, Resource}
import cats.syntax.either._
import com.tessellation.demo.Main.kryoRegistrar
import org.tessellation.ext.kryo._
import org.tessellation.kryo.KryoSerializer

class KryoCodec[F[_]: Async] {
  def serializer(): Resource[F, KryoSerializer[F]] = KryoSerializer.forAsync[F](kryoRegistrar)

  def encode(instance: AnyRef): F[Array[Byte]] = serializer().use(implicit kryo => instance.toBinary.liftTo[F])

  def decode[T](bytes: Array[Byte]): F[T] = serializer().use(implicit kryo => bytes.fromBinary.liftTo[F])
}
