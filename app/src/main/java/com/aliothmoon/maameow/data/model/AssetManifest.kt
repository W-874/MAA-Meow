package com.aliothmoon.maameow.data.model

import kotlinx.serialization.Serializable


@Serializable
data class AssetManifest(
    val archives: List<AssetArchiveDescriptor>,
)

@Serializable
data class AssetArchiveDescriptor(
    val id: String,
    val assetPath: String,
    val destination: String,
    val sha256: String,
    val fileCount: Int,
    val uncompressedSize: Long,
    val versionSha256: String,
)
