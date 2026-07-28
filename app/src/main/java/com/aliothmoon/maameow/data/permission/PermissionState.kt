package com.aliothmoon.maameow.data.permission

import com.aliothmoon.maameow.domain.models.RemoteBackend

/**
 * 权限状态数据类
 */
data class PermissionState(
    val shizukuAvailable: Boolean = false,
    val shizuku: Boolean = false,
    val startupBackend: RemoteBackend = RemoteBackend.SHIZUKU,
    val overlay: Boolean = false,
    val storage: Boolean = false,
    val accessibility: Boolean = false,
    val batteryWhitelist: Boolean = false,
    val notification: Boolean = false
) {
    fun isStartupBackendAvailable(backend: RemoteBackend): Boolean {
        return backend == RemoteBackend.SHIZUKU && shizukuAvailable
    }

    val remoteAccessGranted: Boolean
        get() = startupBackend == RemoteBackend.SHIZUKU && shizuku

    val allRequiredGranted: Boolean
        get() = remoteAccessGranted && overlay && storage && accessibility && batteryWhitelist && notification
}
