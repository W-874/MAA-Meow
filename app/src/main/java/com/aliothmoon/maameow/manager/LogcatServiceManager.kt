package com.aliothmoon.maameow.manager

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.aliothmoon.maameow.BuildConfig
import com.aliothmoon.maameow.ILogcatService
import com.aliothmoon.maameow.domain.models.RemoteBackend
import com.aliothmoon.maameow.remote.LogcatCaptureServiceImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

object LogcatServiceManager {

    private val _service = MutableStateFlow<ILogcatService?>(null)

    // --- Shizuku ---
    private val serviceTag = UUID.randomUUID().toString()
    private val serviceVersion = AtomicInteger(100)
    private var currentServiceArgs: Shizuku.UserServiceArgs? = null

    private val shizukuConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Timber.i("LogcatService connected via Shizuku: %s", name)
            _service.value = ILogcatService.Stub.asInterface(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.i("LogcatService disconnected via Shizuku: %s", name)
            _service.value = null
        }
    }

    fun bind() {
        if (_service.value != null) return
        bindViaShizuku()
    }

    fun unbind() {
        // Shizuku
        val args = currentServiceArgs
        if (args != null) {
            currentServiceArgs = null
            runCatching {
                Shizuku.unbindUserService(args, shizukuConnection, true)
            }.onFailure {
                Timber.w(it, "unbind logcat shizuku service failed")
            }
        }

        _service.value = null
    }

    suspend fun startCapture(appPid: Int, servicePid: Int, userDir: String) {
        withTimeout(10_000) {
            _service.first { it != null }
        }?.startCapture(appPid, servicePid, userDir)
    }

    // --- Shizuku 绑定 ---

    private fun bindViaShizuku() {
        val args = Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, LogcatCaptureServiceImpl::class.java.name)
        ).apply {
            processNameSuffix("logcat")
            daemon(false)
            tag(serviceTag)
            version(serviceVersion.incrementAndGet())
            debuggable(BuildConfig.DEBUG)
        }
        currentServiceArgs = args

        try {
            Shizuku.bindUserService(args, shizukuConnection)
        } catch (e: Exception) {
            Timber.e(e, "bindLogcatService via Shizuku failed")
        }
    }

}
