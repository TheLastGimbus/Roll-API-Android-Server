package com.example.dicerollserver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import fi.iki.elonen.NanoHTTPD
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.Executors

private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val TAG_SERVER = "Server"
    }

    private lateinit var imageCapture: ImageCapture

    private lateinit var wifiLock: WifiManager.WifiLock
    private val WIFI_LOCK_TAG = "server_wifi_lock"

    private val server = WebServer(8822)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        // Acquire wifi lock to keep wifi alive to handle request to server
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WIFI_LOCK_TAG)
        wifiLock.acquire()

        server.start()

        Notifs.createChannels(this)
    }

    override fun onResume() {
        super.onResume()
        if (!server.isAlive) {
            server.stop()
            server.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
        if (wifiLock.isHeld) wifiLock.release()
    }


    private val executor = Executors.newSingleThreadExecutor()

    private fun startCamera() {
        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().build()
        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Capture img use case
        val captureConfig = ImageCaptureConfig.Builder().apply {
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            setFlashMode(FlashMode.ON)
            setTargetResolution(Size(1200, 1600))
        }.build()
        imageCapture = ImageCapture(captureConfig)
        button_capture.setOnClickListener {
            getPicture { }
        }

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun getPicture(
        name: String = "${System.currentTimeMillis()}.jpg",
        result: (picture: File?) -> Unit
    ) {
        val file = File(
            externalMediaDirs.first(),
            name
        )
        imageCapture.takePicture(file, executor,
            object : ImageCapture.OnImageSavedListener {
                override fun onError(
                    imageCaptureError: ImageCapture.ImageCaptureError,
                    message: String,
                    exc: Throwable?
                ) {
                    val msg = "Photo capture failed: $message"
                    Log.e("CameraXApp", msg, exc)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                    result(null)
                }

                override fun onImageSaved(file: File) {
                    val msg = "Photo capture succeeded: ${file.absolutePath}"
                    Log.d("CameraXApp", msg)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                    result(file)
                }
            })
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    private inner class WebServer(PORT: Int) : NanoHTTPD(PORT) {

        // This is to use Regex in 'when'
        operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

        override fun serve(session: IHTTPSession): Response {
            Log.i(TAG_SERVER, "New server request, uri: ${session.uri}")
            var res: Response? = null
            when (session.uri) {
                "/" -> {
                    Log.i(TAG_SERVER, "Uri wants a new shake...")
                    getPicture { picFile ->
                        res = if (picFile != null) {
                            Log.i(TAG_SERVER, "Taking pic success, sending...")
                            newFixedLengthResponse(
                                Response.Status.OK,
                                "image/jpg",
                                picFile.inputStream(),
                                picFile.length()
                            )
                        } else {
                            Log.e(TAG_SERVER, "Taking pic failure!")
                            newFixedLengthResponse(
                                Response.Status.INTERNAL_ERROR,
                                MIME_PLAINTEXT,
                                "Camera error!"
                            )
                        }
                    }
                }
                in Regex("/picture/\\d+.jpg") -> {
                    Log.i(TAG, "Uri wants a picture...")
                    val picFile = File(
                        externalMediaDirs.first(),
                        session.uri.removePrefix("/picture/")
                    )
                    res = if (picFile.exists()) {
                        Log.i(TAG_SERVER, "Picture exists! Sending...")
                        newFixedLengthResponse(
                            Response.Status.OK,
                            "image/jpg",
                            picFile.inputStream(),
                            picFile.length()
                        )
                    } else {
                        Log.e(TAG_SERVER, "Picture doesn't exist!")
                        newFixedLengthResponse(
                            Response.Status.NOT_FOUND,
                            MIME_PLAINTEXT,
                            "404: Not found"
                        )
                    }
                }
                else -> res = newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    MIME_PLAINTEXT,
                    "404: Not found"
                )
            }


            // This ".toString()" turns out to be very important
            // It doesn't work without it because no
            while (res == null) {
                TAG_SERVER.toString()
            }
            res!!.closeConnection(true)
            Log.i(TAG_SERVER, "Serving response...")
            return res!!
        }

        override fun start() {
            super.start()
            Log.i(TAG_SERVER, "Server started")
        }

        override fun stop() {
            super.stop()
            Log.i(TAG_SERVER, "Server stopped")
        }
    }

}
