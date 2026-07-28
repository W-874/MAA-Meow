package com.aliothmoon.maameow.manager

import com.aliothmoon.maameow.domain.models.RemoteBackend

data class RemoteAccessState(
    val shizukuAvailable: Boolean = false,
    val shizukuGranted: Boolean = false,
    val configuredBackend: RemoteBackend = RemoteBackend.SHIZUKU,
) {
    fun isAvailable(backend: RemoteBackend): Boolean {
        return backend == RemoteBackend.SHIZUKU && shizukuAvailable
    }

    fun isGranted(backend: RemoteBackend): Boolean {
        return backend == RemoteBackend.SHIZUKU && shizukuGranted
    }
}
