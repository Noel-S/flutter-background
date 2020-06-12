package com.example.background

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.background.services.Location
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MainActivity: FlutterActivity() {
    var service: Intent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        service = Intent(activity.applicationContext, Location::class.java)

        val fine_location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse_location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine_location != PackageManager.PERMISSION_GRANTED && coarse_location != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 101)
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.background.location").setMethodCallHandler { call, result ->
            if (call.method == "startServiceLocation") {
                val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
                val specialPermGranted = sharedPref.getBoolean("special_perm", false);
                if (specialPermGranted) {
                    startServiceLocation();
                    result.success("Service started.")
                } else {
                    openSpecialPermissions()
                    sharedPref.edit().putBoolean("special_perm", true).apply();
                }
            } else if (call.method == "stopServiceLocation") {
                //stopServiceLocation()
                result.success("Service stopped.")
            }
        }
    }

    fun startServiceLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service)
        } else {
            startService(service)
        }
    }

    private fun stopServiceLocation() {
        stopService(service)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServiceLocation()
    }

    private fun isMiUi(): Boolean {
        return getSystemProperty("ro.miui.ui.version.name").isNotBlank()
    }

    private fun goToXiaomiPermissionsv8() {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
        intent.putExtra("extra_pkgname", packageName)
        startActivity(intent)
    }

    private fun goToXiaomiPermissionsv567() {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
        intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
        intent.putExtra("extra_pkgname", packageName)
        startActivity(intent)
    }

    private fun goToDetails() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun openSpecialPermissions() {
        if (isMiUi()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                goToXiaomiPermissionsv8()
            } else {
                goToXiaomiPermissionsv567()
            }
        } else { //Si no es xiaomi abre los detalles de la aplicacion
            goToDetails()
        }
    }

    private fun getSystemProperty(propName: String): String {
        val line: String
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            input = BufferedReader(InputStreamReader(p.inputStream), 1024)
            line = input.readLine()
            input.close()
        } catch (ex: IOException) {
            return ""
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return line
    }
}
