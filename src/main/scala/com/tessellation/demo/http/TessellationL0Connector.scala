package com.tessellation.demo.http

import cats.effect.Async
import cats.syntax.applicative._
import com.tessellation.demo.Main
import org.http4s.Method.GET
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{Request, Status, Uri}
import org.tessellation.dag.snapshot.{GlobalSnapshot, SnapshotOrdinal}
import org.tessellation.ext.codecs.BinaryCodec._
import org.tessellation.kryo.KryoSerializer
import org.tessellation.security.signature.Signed

trait TessellationL0Connector[F[_]] {
  def getLatestGlobalSnapshot: F[Either[String, Signed[GlobalSnapshot]]]

  def getGlobalSnapshot(snapshotOrdinal: SnapshotOrdinal): F[Either[String, Signed[GlobalSnapshot]]]
}

case class Http4STessellationL0Connector[F[_]: Async](urlPrefix: String) extends TessellationL0Connector[F] {

  private val globalSnapshots = "global-snapshots"

  private[http] val latestGlobalSnapshotUri = url(globalSnapshots + "/latest")

  private[http] def globalSnapshotForOrdinalUri(snapshotOrdinal: SnapshotOrdinal) =
    url(s"$globalSnapshots/${snapshotOrdinal.value}")

  private def url(path: String) = Uri.fromString(s"$urlPrefix/$path").toOption.get

  private def buildClient = EmberClientBuilder.default[F].build

  override def getLatestGlobalSnapshot: F[Either[String, Signed[GlobalSnapshot]]] =
    globalSnapshot(Request(method = GET, uri = latestGlobalSnapshotUri))

  override def getGlobalSnapshot(snapshotOrdinal: SnapshotOrdinal): F[Either[String, Signed[GlobalSnapshot]]] =
    globalSnapshot(Request(method = GET, uri = globalSnapshotForOrdinalUri(snapshotOrdinal)))

  private def globalSnapshot(request: Request[F]) =
    buildClient.use { client => {
      KryoSerializer.forAsync[F](Main.kryoRegistrar).use { implicit kryo =>
        client.run(request).use {
          case Status.Successful(r) =>
            r.attemptAs[Signed[GlobalSnapshot]].leftMap(_.message).value
          case response =>
            val e: Either[String, Signed[GlobalSnapshot]] =
              Left(s"Request $request failed with status ${response.status.code}")
            e.pure
        }
      }
    }
  }
}
