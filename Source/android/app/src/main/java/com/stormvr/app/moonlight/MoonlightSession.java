package com.stormvr.app.moonlight;

import android.app.Activity;
import android.view.Surface;

import com.limelight.binding.audio.AndroidAudioRenderer;
import com.limelight.binding.crypto.AndroidCryptoProvider;
import com.limelight.binding.video.CrashListener;
import com.limelight.binding.video.MediaCodecDecoderRenderer;
import com.limelight.binding.video.PerfOverlayListener;
import com.limelight.nvstream.NvConnection;
import com.limelight.nvstream.NvConnectionListener;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.preferences.PreferenceConfiguration;

/**
 * Uma sessão de streaming (Sunshine/Apollo/GeForce) que desenha numa {@link Surface} qualquer:
 *  - tela cheia: Surface de um SurfaceView (MoonlightStreamActivity);
 *  - tela flutuante na Sala 3D: Surface(SurfaceTexture) ligado à textura da sala (próximo passo).
 */
public class MoonlightSession implements NvConnectionListener, PerfOverlayListener, CrashListener {
    public interface Events {
        void onStatus(String text);
        void onStarted();
        void onEnded(int errorCode, String message);
    }

    private final Activity act;
    private final Events ev;
    private NvConnection conn;
    private MediaCodecDecoderRenderer decoder;
    private AndroidAudioRenderer audio;
    private volatile boolean stopped;
    private String lastMessage = "";
    private String host = "";

    public MoonlightSession(Activity act, Events ev) { this.act = act; this.ev = ev; }

    public void start(String host, NvApp app, MoonlightStore st, Surface surface) {
        this.host = host;
        com.limelight.binding.video.MediaCodecHelper.initialize(act, "");   // obrigatorio antes do MediaCodecDecoderRenderer (o Artemis chama isso em Game.onCreate)
        if (!surface.isValid()) throw new IllegalStateException("A tela de vídeo ainda não está pronta.");
        final boolean compatible = st.compatibility();
        final int width = compatible ? 1280 : st.width();
        final int height = compatible ? 720 : st.height();
        final int fps = compatible ? 30 : st.fps();
        final int bitrate = compatible ? 8000 : st.bitrateKbps();
        PreferenceConfiguration prefs = PreferenceConfiguration.readPreferences(act);
        prefs.width = width; prefs.height = height; prefs.fps = fps;
        prefs.bitrate = bitrate; prefs.enableHdr = false; prefs.enablePerfOverlay = false;

        decoder = new MediaCodecDecoderRenderer(act, prefs, this, 0, false, false, false, "", this);
        decoder.setRenderTarget(surface);
        audio = new AndroidAudioRenderer(act, false);

        int formats = MoonBridge.VIDEO_FORMAT_H264;
        if (!compatible && decoder.isHevcSupported()) formats |= MoonBridge.VIDEO_FORMAT_H265;
        if (!compatible && decoder.isAv1Supported()) formats |= MoonBridge.VIDEO_FORMAT_AV1_MAIN8;

        StreamConfiguration config = new StreamConfiguration.Builder()
                .setResolution(width, height)
                .setLaunchRefreshRate(fps)
                .setRefreshRate(fps)
                .setVirtualDisplay(false)
                .setResolutionScaleFactor(100)
                .setApp(app)
                .setEnableUltraLowLatency(false)
                .setBitrate(bitrate)
                .setEnableSops(true)
                .enableLocalAudioPlayback(st.localAudio())
                .setMaxPacketSize(1392)
                .setRemoteConfiguration(StreamConfiguration.STREAM_CFG_AUTO)
                .setSupportedVideoFormats(formats)
                .setAttachedGamepadMask(0)
                .setClientRefreshRateX100(fps * 100)
                .setAudioConfiguration(MoonBridge.AUDIO_CONFIGURATION_STEREO)
                .setColorSpace(decoder.getPreferredColorSpace())
                .setColorRange(decoder.getPreferredColorRange())
                .setPersistGamepadsAfterDisconnect(true)
                .build();

        conn = new NvConnection(act.getApplicationContext(), new ComputerDetails.AddressTuple(host, 47989), 0,
                st.uniqueId(), config, new AndroidCryptoProvider(act), st.cert(host));
        conn.start(audio, decoder, this);
    }

    // Recovery is driven by decoded frames, never by an unconditional IDR storm.
    private volatile boolean keepRun = false;
    private void startKeepAlive() {
        if (keepRun) return;
        keepRun = true;
        final MediaCodecDecoderRenderer renderer = decoder;
        new Thread(() -> {
            VideoHealth health = new VideoHealth(android.os.SystemClock.elapsedRealtime());
            while (keepRun && !stopped) {
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
                if (!keepRun || stopped) break;
                int events = health.update(android.os.SystemClock.elapsedRealtime(), renderer.getStormRenderedFrames());
                if ((events & VideoHealth.VIDEO_READY) != 0) ev.onStarted();
                if ((events & VideoHealth.REQUEST_KEYFRAME) != 0) {
                    try { MoonBridge.requestIdrFrame(); } catch (Throwable ignored) { }
                }
                if ((events & VideoHealth.NO_VIDEO) != 0) {
                    ev.onStatus("Conectado, mas sem quadros de vídeo. Use o modo compatível e confira a captura/monitor no Apollo e a rede privada do Windows.");
                }
            }
        }, "StormVR-VideoWatchdog").start();
    }

    public void stop() {
        keepRun = false;
        stopped = true;
        if (conn != null) { conn.stop(); conn = null; }
    }

    public NvConnection connection() { return conn; }

    /**
     * ENCERRAR A SESSAO: desconecta o stream e manda o Sunshine/GeForce FECHAR o app em execucao no PC (o mesmo "Encerrar" do Moonlight).
     * Diferente de so sair (voltar / Home), que deixa o jogo rodando no PC para voltar depois. [done] roda na thread principal.
     */
    public void endSession(MoonlightStore st, Runnable done) {
        final String h = host;
        stop();
        new Thread(() -> {
            try {
                new com.limelight.nvstream.http.NvHTTP(new ComputerDetails.AddressTuple(h, com.limelight.nvstream.http.NvHTTP.DEFAULT_HTTP_PORT), 0,
                        st.uniqueId(), st.cert(h), new AndroidCryptoProvider(act)).quitApp();
            } catch (Throwable t) { /* PC fora do ar / app ja fechado: so sai */ }
            if (done != null) act.runOnUiThread(done);
        }, "StormVR-MoonQuit").start();
    }

    // ---- NvConnectionListener
    @Override public void stageStarting(String stage) { ev.onStatus("Iniciando: " + stage); }
    @Override public void stageComplete(String stage) { }
    @Override public boolean stageFailed(String stage, int portFlags, int errorCode) {
        keepRun = false;
        String reason = errorCode == 403 ? "Apollo recusou iniciar o aplicativo. Em Clientes, habilite Iniciar aplicativos para este celular; somente Ver transmissão não basta." : "Verifique o Apollo, o IP do PC e a rede. Ative o modo compatível para testar o vídeo.";
        lastMessage = "Falha em " + stage + " (" + errorCode + "). " + reason;
        android.util.Log.e("StormVR-Stream", lastMessage + " ports=" + portFlags);
        ev.onEnded(errorCode, lastMessage); return false;
    }
    @Override public void connectionStarted() { ev.onStatus("Conectado ao PC. Aguardando o primeiro quadro de vídeo…"); startKeepAlive(); }
    @Override public void connectionTerminated(int errorCode) { keepRun = false; if (!stopped) ev.onEnded(errorCode, lastMessage); }
    @Override public void connectionStatusUpdate(int connectionStatus) { }
    @Override public void displayMessage(String message) { lastMessage = message == null ? "" : message; ev.onStatus(lastMessage); }
    @Override public void displayTransientMessage(String message) { ev.onStatus(message); }
    @Override public void rumble(short controllerNumber, short lowFreqMotor, short highFreqMotor) { }
    @Override public void rumbleTriggers(short controllerNumber, short leftTrigger, short rightTrigger) { }
    @Override public void setHdrMode(boolean enabled, byte[] hdrMetadata) { }
    @Override public void setMotionEventState(short controllerNumber, byte motionType, short reportRateHz) { }
    @Override public void setControllerLED(short controllerNumber, byte r, byte g, byte b) { }

    // ---- PerfOverlayListener / CrashListener
    @Override public void onPerfUpdate(String text) { }
    @Override public void notifyCrash(Exception e) { ev.onStatus("Falha no decodificador: " + e.getMessage()); }
}
