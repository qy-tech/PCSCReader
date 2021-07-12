package com.jax.pcscdemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.jax.pcscdemo.ui.main.MainFragment
import kotlinx.coroutines.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_PERMISSION = 0x00
    }

    private val context by lazy {
        this@MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }

        requestPermission()

    }

    private fun requestPermission() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(
            context,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            REQUEST_CODE_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            val denied = grantResults.any { it != PackageManager.PERMISSION_GRANTED }
            if (denied) {
                requestPermission()
            }
        }
    }


}