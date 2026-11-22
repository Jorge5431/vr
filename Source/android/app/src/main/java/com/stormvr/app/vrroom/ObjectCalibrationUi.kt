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
        controller.alternatePattern = ObjectAppearance.decode(prefs.getString("sample_second_view", null))
        controller.penMode = prefs.getBoolean("pen_mode", true)
        controller.enabled = (controller.penMode || controller.pattern != null) &&
            prefs.getBoolean("enabled", controller.penMode)
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
            text = "Usar caneta/objeto como controle"
            isChecked = controller.enabled
            isEnabled = controller.penMode || controller.pattern != null
            setOnCheckedChangeListener { _, value ->
                controller.enabled = value
                prefs.edit().putBoolean("enabled", value).apply()
            }
        }
        panel.addView(enabled)
        val penModeOption = CheckBox(activity).apply {
            setTextColor(Color.WHITE)
            text = "Reconhecer STABILO point 88 azul pelas cores e formato"
            isChecked = controller.penMode
            setOnCheckedChangeListener { _, value ->
                controller.penMode = value
                controller.clear()
                prefs.edit().putBoolean("pen_mode", value).apply()
                enabled.isEnabled = value || controller.pattern != null
                if (value) enabled.isChecked = true
                if (!enabled.isEnabled) enabled.isChecked = false
            }
        }
        panel.addView(penModeOption)
        panel.addView(Button(activity).apply {
            isAllCaps = false
            text = "Cadastrar a ponta da caneta/objeto"
            setOnClickListener {
                val token = ++session
                val request: (CameraPhoto) -> Unit = { photo ->
                    main.post {
                        if (session == token && !activity.isFinishing && !activity.isDestroyed) {
                            showPhoto(photo) { appearance ->
                                controller.pattern = appearance
                                controller.alternatePattern = null
                                controller.penMode = false
                                penModeOption.isChecked = false
                                enabled.isEnabled = true
                                enabled.isChecked = true
                                controller.enabled = true
                                prefs.edit().putString("sample", appearance.encode()).remove("sample_second_view")
                                    .putBoolean("enabled", true).putBoolean("pen_mode", false).apply()
                                message("Ponta cadastrada. Ela controla a mão que a segura; pinça seleciona.")
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
        panel.addView(Button(activity).apply {
            isAllCaps = false
            text = "Adicionar outra vista da caneta"
            setOnClickListener {
                if (controller.pattern == null) {
                    message("Cadastre primeiro a ponta da caneta.")
                    return@setOnClickListener
                }
                val token = ++session
                val request: (CameraPhoto) -> Unit = { photo ->
                    main.post {
                        if (session == token && !activity.isFinishing && !activity.isDestroyed) {
                            showPhoto(photo) { appearance ->
                                controller.alternatePattern = appearance
                                prefs.edit().putString("sample_second_view", appearance.encode()).apply()
                                message("Segunda vista cadastrada. A mesma ponta pode ser reconhecida em outro ângulo.")
                            }
                        }
                    }
                }
                message("Gire a caneta e fotografe a mesma ponta por outro ângulo. Toque nela na imagem.")
                controller.capture.set(request)
                startCamera()
                main.postDelayed({ if (session == token) controller.capture.compareAndSet(request, null) }, 20000)
            }
        })
        panel.addView(TextView(activity).apply {
            setTextColor(Color.LTGRAY)
            text = "Com as duas opções acima marcadas, mostre à câmera a mão, o corpo laranja e a tampa azul da caneta. O reconhecimento agora separa a caneta de outros objetos alaranjados. Para outro objeto, cadastre uma marca abaixo. A direção em profundidade é estimada, não medida."
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
