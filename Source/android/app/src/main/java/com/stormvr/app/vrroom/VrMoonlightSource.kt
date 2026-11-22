package com.stormvr.app.vrroom

import android.app.Activity
import android.view.MotionEvent
import android.view.Surface
import android.widget.Toast
import com.limelight.binding.crypto.AndroidCryptoProvider
import com.limelight.nvstream.http.ComputerDetails
import com.limelight.nvstream.http.NvHTTP
import com.stormvr.app.Prefs
import com.stormvr.app.moonlight.MoonlightSession
import com.stormvr.app.moonlight.MoonlightStore

/** Decodes Moonlight directly into the VR texture while the same activity tracks hands. */
class VrMoonlightSource(private val activity: Activity) : VrRoomActivity.ScreenSource, MoonlightSession.Events {
    private val store = MoonlightStore(activity)
    private var session: MoonlightSession? = null
    private var surface: Surface? = null
    private var generation = 0
    @Volatile var status = "Aguardando vídeo do PC"
        private set

    override fun attach(surface: Surface, width: Int, height: Int) {
        if (this.surface === surface && session != null) return
        detach()
        this.surface = surface
        val token = ++generation
        val preferred = Prefs(activity).host
        val host = store.hosts().firstOrNull { it == preferred && store.paired(it) }
            ?: store.hosts().firstOrNull { store.paired(it) }
        if (host == null) {
            report("Pareie o PC na tela Moonlight antes de iniciar SteamVR + mãos.")
            return
        }
        report("Conectando ao vídeo de $host")
        Thread({
            try {
                val http = NvHTTP(ComputerDetails.AddressTuple(host, NvHTTP.DEFAULT_HTTP_PORT), 0,
                    store.uniqueId(), store.cert(host), AndroidCryptoProvider(activity))
                val apps = http.appList
                val chosen = apps.firstOrNull { it.appName.equals(store.tvApp(), true) }
                    ?: apps.firstOrNull { it.appName.equals("Desktop", true) }
                    ?: apps.firstOrNull()
                if (chosen == null) { report("O PC não oferece Desktop para transmissão."); return@Thread }
                activity.runOnUiThread {
                    if (token != generation || this.surface !== surface || !surface.isValid) return@runOnUiThread
                    try {
                        session = MoonlightSession(activity, this)
                        session!!.start(host, chosen, store, surface)
                    } catch (e: Throwable) {
                        session = null
                        report("Falha no vídeo: ${e.message ?: e.javaClass.simpleName}")
                    }
                }
            } catch (e: Exception) { if (token == generation) report("Falha ao conectar: ${e.message ?: e.javaClass.simpleName}") }
        }, "StormVR-DirectMoonlight").start()
    }

    override fun detach() {
        generation++
        session?.stop()
        session = null
        surface = null
    }

    override fun touch(ev: MotionEvent) { /* Gestos do headset ficam com o rastreamento/SteamVR. */ }
    private fun report(message: String) {
        status = message
        if (message.startsWith("Falha") || message.startsWith("Pareie") || message.startsWith("O PC"))
            activity.runOnUiThread { Toast.makeText(activity, message, Toast.LENGTH_LONG).show() }
    }
    override fun onStatus(text: String) { report(text) }
    override fun onStarted() { status = "Vídeo do PC ativo" }
    override fun onEnded(errorCode: Int, message: String) {
        if (errorCode != 0) report("Falha no vídeo ($errorCode): $message")
        else status = "Transmissão encerrada"
    }
}
