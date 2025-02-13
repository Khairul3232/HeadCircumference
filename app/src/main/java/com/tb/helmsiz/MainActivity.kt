package com.tb.helmsiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import com.tb.helmsiz.ui.theme.HelmSizTheme

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Text

import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

import android.graphics.RectF
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.camera.core.Preview as CameraPreview


import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permission", "Camera permission granted")
                // You can now access the camera
            } else {
                Log.d("Permission", "Camera permission denied")
                // Handle permission denial (e.g., show a message to the user)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request camera permission
        requestCameraPermission()
//        enableEdgeToEdge()
        setContent {
            HelmSizTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MeasureHeadCircumferenceScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
//        setContent {
//            HelmSizTheme {
//                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
    }


    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("Permission", "Camera permission already granted")
                // Camera permission is already granted, proceed with camera-related tasks
            }
            else -> {
                // Request permission if not already granted
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }
}

@Composable
fun MeasureHeadCircumferenceScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    if (!SensorAndCameraCheck(context)) {
        Text("Required sensors or camera not available!")
    } else {
        MeasureHeadCircumference(modifier)
    }
}



@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}



//@Composable
//fun SensorAndCameraCheck(onSensorsAvailable: () -> Unit) {
//    val context = LocalContext.current
//    val sensorsAvailable = remember {
//        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
//        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
//        proximitySensor != null && hasCamera
//    }
//
//    if (sensorsAvailable) {
//        onSensorsAvailable()
//    } else {
//        Text("Required sensors or camera not available on this device.")
//    }
//}

fun SensorAndCameraCheck(context: Context): Boolean {
    val packageManager = context.packageManager

    // Check if the device has a proximity sensor
    val hasProximitySensor = packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY)

    // Check if the device has a front-facing camera
    val hasFrontCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)

    return hasProximitySensor && hasFrontCamera
}


@Composable
fun ProximitySensorListener(onDistanceUpdate: (Float) -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    DisposableEffect(Unit) {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.firstOrNull()?.let { distance ->
                    onDistanceUpdate(distance)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        proximitySensor?.let {
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
}



@OptIn(ExperimentalGetImage::class)
@Composable
fun FaceDetectionPreview(onFaceBoundsUpdate: (RectF?) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val faceDetector = remember {
        FaceDetection.getClient(FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build())
    }

    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = CameraPreview.Builder().build()
            val imageAnalyzer = ImageAnalysis.Builder().build()

            imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    faceDetector.process(inputImage)
                        .addOnSuccessListener { faces ->
                            if (faces.isNotEmpty()) {
                                val bounds = faces[0].boundingBox
                                onFaceBoundsUpdate(RectF(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat()))
                            } else {
                                onFaceBoundsUpdate(null)
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                }
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
            preview.setSurfaceProvider(previewView.surfaceProvider)
        }, ContextCompat.getMainExecutor(ctx))

        previewView
    })
}

@Composable
fun MeasureHeadCircumference(modifier: Modifier) {
    Text(
        text = "Hello Khai!",
        modifier = modifier
    )
    var distance by remember { mutableStateOf<Float?>(null) }
    var faceBounds by remember { mutableStateOf<RectF?>(null) }
    var headCircumference by remember { mutableStateOf<Float?>(null) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Distance: ${distance ?: "Calculating..."} cm")
        Text("Head Circumference: ${headCircumference ?: "Calculating..."} cm")

        ProximitySensorListener { newDistance ->
            distance = newDistance
            headCircumference = calculateHeadCircumference(distance, faceBounds)
        }

        FaceDetectionPreview { newFaceBounds ->
            faceBounds = newFaceBounds
            headCircumference = calculateHeadCircumference(distance, faceBounds)
        }

    }


//    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
//        ProximitySensorListener { newDistance ->
//            distance = newDistance
//            headCircumference = calculateHeadCircumference(distance, faceBounds)
//        }
//
//        FaceDetectionPreview { newFaceBounds ->
//            faceBounds = newFaceBounds
//            headCircumference = calculateHeadCircumference(distance, faceBounds)
//        }
//
//        Text("Distance: ${distance ?: "Calculating..."} cm")
//        Text("Head Circumference: ${headCircumference ?: "Calculating..."} cm")
//    }
}

fun calculateHeadCircumference(distance: Float?, faceBounds: RectF?): Float? {
    if (distance == null || faceBounds == null) return null

    // Estimate the head circumference based on face bounds and distance
    val faceWidth = faceBounds.right - faceBounds.left
//    return (faceWidth / distance) * 100 // Adjust scaling as needed for units
    return ((faceWidth / distance) * 100) / 100
}

@ComposePreview(showBackground = true)
@Composable
fun GreetingPreview() {
    HelmSizTheme {
        Greeting("Android")
    }
}