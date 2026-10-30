package com.example.pythonmonitor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : Activity() {
    private lateinit var output: TextView
    private val locationRequestCode = 4101
    private val cameraRequestCode = 4102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) Python.start(AndroidPlatform(this))

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val title = TextView(this).apply { text = "Python Monitor"; textSize = 26f }
        val intro = TextView(this).apply {
            text = "Локальная диагностика. Данные не отправляются на сервер."
            textSize = 15f
            setPadding(0, 12, 0, 22)
        }
        output = TextView(this).apply {
            text = "Выберите действие ниже."
            textSize = 15f
            setPadding(0, 12, 0, 20)
        }
        val infoButton = Button(this).apply {
            text = "Информация об устройстве"
            setOnClickListener { showDeviceInfo() }
        }
        val locationButton = Button(this).apply {
            text = "Получить мою геолокацию"
            setOnClickListener { requestLocation() }
        }
        val cameraButton = Button(this).apply {
            text = "Открыть камеру"
            setOnClickListener { openCamera() }
        }
        layout.addView(title)
        layout.addView(intro)
        layout.addView(infoButton)
        layout.addView(locationButton)
        layout.addView(cameraButton)
        layout.addView(output)
        setContentView(layout)
    }

    private fun showDeviceInfo() {
        val values = mapOf(
            "manufacturer" to android.os.Build.MANUFACTURER,
            "model" to android.os.Build.MODEL,
            "android_version" to android.os.Build.VERSION.RELEASE,
            "sdk" to android.os.Build.VERSION.SDK_INT.toString()
        )
        output.text = Python.getInstance().getModule("report")
            .callAttr("format_device_info", values).toString()
    }

    private fun requestLocation() {
        val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                locationRequestCode
            )
        } else readLastLocation()
    }

    private fun readLastLocation() {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        var best: Location? = null
        for (provider in providers) {
            try {
                @Suppress("MissingPermission")
                val candidate = manager.getLastKnownLocation(provider)
                if (candidate != null && (best == null || candidate.time > best!!.time)) best = candidate
            } catch (_: Exception) { }
        }
        val location = best
        output.text = if (location == null) {
            "Последняя позиция недоступна. Включите геолокацию и повторите попытку."
        } else {
            "Координаты устройства:\nШирота: ${location.latitude}\nДолгота: ${location.longitude}\nТочность: ${location.accuracy} м"
        }
    }

    private fun openCamera() {
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), cameraRequestCode)
        } catch (_: Exception) {
            Toast.makeText(this, "Приложение камеры не найдено.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Uses the platform camera activity result for broad compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequestCode && resultCode == Activity.RESULT_OK) {
            output.text = "Камера открыта по вашему нажатию. Снимок не отправлялся."
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequestCode) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) readLastLocation()
            else output.text = "Разрешение на геолокацию не предоставлено."
        }
    }
}
