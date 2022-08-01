package com.tessellation.demo.utils

import cats.effect.Async
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.tessellation.demo.Main.kryoRegistrar
import org.tessellation.dag.snapshot.StateChannelSnapshotBinary
import org.tessellation.keytool.KeyStoreUtils.readKeyPairFromStore
import org.tessellation.kryo.KryoSerializer
import org.tessellation.security.SecurityProvider
import org.tessellation.security.signature.Signed

case class Signer[F[_]: Async](keyStorePath: String, keyAlias: String, password: String) {

  def sign(binary: StateChannelSnapshotBinary): F[Signed[StateChannelSnapshotBinary]] =
    SecurityProvider.forAsync[F].use { implicit sp =>
      KryoSerializer.forAsync[F](kryoRegistrar).use { implicit kryo =>
        for {
          keyPair <- readKeyPairFromStore[F](keyStorePath, keyAlias, password.toCharArray, password.toCharArray)
          signed <- Signed.forAsyncKryo[F, StateChannelSnapshotBinary](binary, keyPair)
        } yield signed
      }
    }
}
