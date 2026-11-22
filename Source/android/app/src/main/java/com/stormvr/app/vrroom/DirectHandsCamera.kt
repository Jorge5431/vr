package com.stormvr.app.vrroom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import kotlin.math.abs

/** Rear camera for devices without ARCore. The renderer takes the newest YUV frame. */
internal class DirectHandsCamera(context: Context, private val onError: (String) -> Unit) : AutoCloseable {
    private val cameras = context.getSystemService(CameraManager::class.java)
    private val thread = HandlerThread("StormVR-Camera").apply { start() }
    private val handler = Handler(thread.looper)
    private val imageLock = Any()
    @Volatile private var closed = false
    private var reader: ImageReader? = null
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var pending: Image? = null
    private var orientation = 90
    private var focalRatio = 0.9f
    private var lastSubmit = 0L

    @SuppressLint("MissingPermission")
    fun start(width: Int, height: Int) {
        handler.post {
            if (closed) return@post
            try {
                val id = cameras.cameraIdList.first { cameras.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK }
                val caps = cameras.getCameraCharacteristics(id)
                orientation = caps.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
                val mm = caps.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
                val sensor = caps.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                if (mm != null && sensor != null && sensor.width > 0) focalRatio = mm / sensor.width
                val sizes = caps.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?.getOutputSizes(ImageFormat.YUV_420_888).orEmpty()
                val size = sizes.minByOrNull { abs(it.width - width) + abs(it.height - height) }
                    ?: error("A câmera não oferece imagem YUV")
                val output = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 4)
                reader = output
                output.setOnImageAvailableListener({ source ->
                    val frame = try { source.acquireLatestImage() } catch (_: IllegalStateException) { null }
                    synchronized(imageLock) {
                        if (closed) frame?.close() else if (frame != null) { pending?.close(); pending = frame }
                    }
                }, handler)
                cameras.openCamera(id, object : CameraDevice.StateCallback() {
                    override fun onOpened(cam: CameraDevice) {
                        if (closed) { cam.close(); return }
                        device = cam
                        try {
                            cam.createCaptureSession(listOf(output.surface), object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(s: CameraCaptureSession) {
                                    if (closed) { s.close(); return }
                                    session = s
                                    try {
                                        val req = cam.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                            addTarget(output.surface)
                                            if (caps.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.contains(
                                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO) == true)
                                                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                                        }
                                        s.setRepeatingRequest(req.build(), null, handler)
                                    } catch (e: Exception) { fail(e.message ?: "Falha ao capturar") }
                                }
                                override fun onConfigureFailed(s: CameraCaptureSession) = fail("Configuração de câmera recusada")
                            }, handler)
                        } catch (e: Exception) { fail(e.message ?: "Falha na câmera") }
                    }
                    override fun onDisconnected(cam: CameraDevice) { cam.close(); fail("Câmera desconectada") }
                    override fun onError(cam: CameraDevice, code: Int) { cam.close(); fail("Câmera indisponível ($code)") }
                }, handler)
            } catch (e: Exception) { fail(e.message ?: "Não foi possível abrir a câmera") }
        }
    }

    private fun fail(message: String) {
        if (!closed) onError(message)
        close()
    }

    /** Called on the GL thread. All data is copied before releasing the camera buffer. */
    fun read(rotation: Int, fps: Int, consume: (Image, Int, Float, Boolean) -> Unit) {
        synchronized(imageLock) {
            if (closed) return
            val image = pending ?: return
            pending = null
            image.use {
                val angle = (orientation - rotation * 90 + 360) % 360
                val inference = lastSubmit == 0L || it.timestamp - lastSubmit >= 1_000_000_000L / fps.coerceIn(1, 60)
                if (inference) lastSubmit = it.timestamp
                consume(it, angle, focalRatio * it.width, inference)
            }
        }
    }

    override fun close() {
        synchronized(imageLock) {
            if (closed) return
            closed = true
            pending?.close(); pending = null
        }
        handler.post {
            session?.close()
            device?.close()
            reader?.close()
            thread.quitSafely()
        }
    }
}
