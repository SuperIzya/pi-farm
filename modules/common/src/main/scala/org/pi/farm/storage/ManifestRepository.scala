package org.pi.farm.storage

import org.pi.farm.plugin.Manifest

import zio.{NonEmptyChunk, ULayer, ZLayer}

trait ManifestRepository {
  def manifests: NonEmptyChunk[Manifest]
}

object ManifestRepository {
  def live(manifest: Manifest, manifests: Manifest*): ULayer[ManifestRepository] = live(
    NonEmptyChunk(manifest, manifests*)
  )

  def live(m: NonEmptyChunk[Manifest]): ULayer[ManifestRepository] = ZLayer.succeed(new ManifestRepository {
    val manifests: NonEmptyChunk[Manifest] = m
  })
}
