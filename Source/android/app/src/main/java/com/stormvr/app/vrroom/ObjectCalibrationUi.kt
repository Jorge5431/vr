package com.stormvr.app.vrroom

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.*
import kotlin.math.roundToInt

class ObjectCalibrationUi(private val activity: Activity, private val controller: ObjectController) {
    private val prefs = activity.getSharedPreferences("object-controller", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private var session = 0
    private var picker: AlertDialog? = null

    init {
        controller.pattern = ObjectAppearance.decode(prefs.getString("sample", null))
        controller.enabled = controller.pattern != null && prefs.getBoolean("enabled", false)
    }

    private fun message(text: String) = Toast.makeText(activity, text, Toast.LENGTH_LONG).show()

    fun cancelCapture() {
        session++
        controller.capture.set(null)
        picker?.dismiss()
        picker = null
    }

    fun addTo(panel: LinearLayout, startCamera: () -> Unit) {
        val enabled = CheckBox(activity).apply {
            setTextColor(Color.WHITE)
            text = "Mirar com o objeto cadastrado"
            isChecked = controller.enabled
            isEnabled = controller.pattern != null
            setOnCheckedChangeListener { _, value ->
                controller.enabled = value
                prefs.edit().putBoolean("enabled", value).apply()
            }
        }
        panel.addView(enabled)
        panel.addView(Button(activity).apply {
            isAllCaps = false
            text = "Fotografar e cadastrar objeto"
            setOnClickListener {
                val token = ++session
                val request: (CameraPhoto) -> Unit = { photo ->
                    main.post {
                        if (session == token && !activity.isFinishing && !activity.isDestroyed) {
                            showPhoto(photo) { appearance ->
                                controller.pattern = appearance
                                enabled.isEnabled = true
                                enabled.isChecked = true
                                controller.enabled = true
                                prefs.edit().putString("sample", appearance.encode()).putBoolean("enabled", true).apply()
                                message("Objeto cadastrado. Mova-o para mirar e use a pinça para selecionar.")
                            }
                        }
                    }
                }
                message("Segure o objeto com a mão visível. Depois toque em um detalhe dele na foto.")
                controller.capture.set(request)
                startCamera()
                main.postDelayed({
                    if (session == token && controller.capture.compareAndSet(request, null)) {
                        message("A câmera não encontrou uma mão. Volte à aba Caneta / Objeto e tente de novo.")
                    }
                }, 20000)
            }
        })
        panel.addView(TextView(activity).apply {
            setTextColor(Color.LTGRAY)
            text = "Cadastre uma ponta, borda ou marca visível perto dos dedos. Ela define a posição do controle e a mira. A profundidade é estimada pela mão. Para trocar de objeto, fotografe novamente."
        })
    }

    private fun showPhoto(photo: CameraPhoto, onSaved: (ObjectAppearance) -> Unit) {
        val image = ImageView(activity).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            minimumHeight = (140 * activity.resources.displayMetrics.density).toInt()
            setImageBitmap(Bitmap.createBitmap(photo.rgb, photo.w, photo.h, Bitmap.Config.ARGB_8888).also {
                it.density = Bitmap.DENSITY_NONE
            })
        }
        val dialog = AlertDialog.Builder(activity).setTitle("Toque no detalhe que vai controlar a mira")
            .setView(image).setNegativeButton("Cancelar", null).create()
        image.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val xy = floatArrayOf(event.x, event.y)
                val inverse = Matrix()
                if (image.imageMatrix.invert(inverse)) {
                    inverse.mapPoints(xy)
                    val sample = ObjectAppearance.register(photo, xy[0].roundToInt(), xy[1].roundToInt())
                    if (sample != null) { onSaved(sample); dialog.dismiss() }
                    else message("Escolha um detalhe com contraste na parte do objeto que aparece perto dos dedos.")
                }
                image.performClick()
            }
            true
        }
        picker = dialog
        dialog.show()
    }
}
