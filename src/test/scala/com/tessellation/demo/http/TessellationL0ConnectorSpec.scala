package com.tessellation.demo.http

import cats.effect.IO
import com.tessellation.demo.BaseSpec
import eu.timepit.refined.types.numeric.NonNegLong
import org.tessellation.dag.snapshot._

class TessellationL0ConnectorSpec extends BaseSpec {
  private val tessellationBaseUrl = "http://localhost:12345"
  private val connector = Http4STessellationL0Connector[IO](tessellationBaseUrl)
  private val latestGlobalSnapshotPath = "/global-snapshots/latest"

  "url" should {
    "be parsed successfully" in {
      connector.latestGlobalSnapshotUri.renderString shouldBe s"$tessellationBaseUrl$latestGlobalSnapshotPath"

      connector
        .globalSnapshotForOrdinalUri(SnapshotOrdinal(NonNegLong.MinValue))
        .renderString shouldBe s"$tessellationBaseUrl/global-snapshots/0"
    }
  }
}
