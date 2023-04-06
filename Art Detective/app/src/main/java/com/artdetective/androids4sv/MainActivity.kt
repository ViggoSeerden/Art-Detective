package com.artdetective.androids4sv

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberImagePainter
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.artdetective.androids4sv.TensorFLowHelper.imageSize
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // Use the returned uri.
            croppedPhotoUri = result.uriContent!!
            toggleClassifier()
        } else {
            // An error occurred.
            val exception = result.error
        }
    }

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)

    private lateinit var photoUri: Uri
    private lateinit var croppedPhotoUri: Uri

    private var shouldShowPhoto: MutableState<Boolean> = mutableStateOf(false)

    private var showImageCropper: MutableState<Boolean> = mutableStateOf(false)

    private var showImageClassifier: MutableState<Boolean> = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("kilo", "Permission granted")
            shouldShowCamera.value = true
        } else {
            Log.i("kilo", "Permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (shouldShowCamera.value) {
                CameraView(
                    outputDirectory = outputDirectory,
                    executor = cameraExecutor,
                    onImageCaptured = ::handleImageCapture,
                    onError = { Log.e("kilo", "View error:", it) },
                )
            }
            else {
                if (!shouldShowPhoto.value && !showImageCropper.value) {
                    CamButton()
                }
            }

            if (shouldShowPhoto.value) {
                Image(
                    painter = rememberImagePainter(photoUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                Log.e("knskrt", "$photoUri")

                CropButton()
            }

            if (showImageCropper.value) {
                ImageSelectorAndCropper(photoUri)
            }

            if (showImageClassifier.value) {
                ImagePicker(croppedPhotoUri)
            }
        }

        requestCameraPermission()

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("kilo", "Permission previously granted")
                shouldShowCamera.value = true
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> Log.i("kilo", "Show camera permissions dialog")

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun handleImageCapture(uri: Uri) {
        Log.i("kilo", "Image captured: $uri")
        shouldShowCamera.value = false

        photoUri = uri
        shouldShowPhoto.value = true
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }

        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    @Composable
    fun CamButton() {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
//                .border(width = 1.dp, Color.Green)
        ) {
            Button(
                onClick = { changeData() },
                colors = ButtonDefaults
                    .buttonColors(backgroundColor = Color.Black, contentColor = Color.White)
            ) {
                Text(text = "Take Picture")
            }
        }
    }

    @Composable
    fun CropButton() {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
//                .border(width = 1.dp, Color.Green)
        ) {
            Row() {
                Button(
                    onClick = { toggleCropper() },
                    colors = ButtonDefaults
                        .buttonColors(backgroundColor = Color.Black, contentColor = Color.White)
                ) {
                    Text(text = "Crop Image")
                }
                Spacer(modifier = Modifier.padding(20.dp))
                Button(
                    onClick = { croppedPhotoUri = photoUri
                        toggleClassifier() },
                    colors = ButtonDefaults
                        .buttonColors(backgroundColor = Color.Black, contentColor = Color.White)
                ) {
                    Text(text = "Classify Image")
                }
                Spacer(modifier = Modifier.padding(20.dp))
                Button(
                    onClick = { changeData() },
                    colors = ButtonDefaults
                        .buttonColors(backgroundColor = Color.Black, contentColor = Color.White)
                ) {
                    Text(text = "Retake Photo")
                }
            }
        }
    }

    //Onclick of CamButton
    private fun changeData() {
        shouldShowCamera.value = true
        shouldShowPhoto.value = false
        showImageCropper.value = false
        showImageClassifier.value = false
    }

    private fun toggleCropper() {
        shouldShowCamera.value = false
        shouldShowPhoto.value = false
        showImageCropper.value = true
        showImageClassifier.value = false
    }

    private fun toggleClassifier() {
        shouldShowCamera.value = false
        shouldShowPhoto.value = false
        showImageCropper.value = false
        showImageClassifier.value = true
    }

    @Composable
    fun ImageSelectorAndCropper(madePhotoUri: Uri) {
        val options = CropImageContractOptions(madePhotoUri, CropImageOptions())
        cropImage.launch(options)
//        var imageUri by remember {
//            mutableStateOf<Uri?>(madePhotoUri)
//        }
//        val context = LocalContext.current
//        val bitmap =  remember {
//            mutableStateOf<Bitmap?>(null)
//        }
//
//        val imageCropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
//            if (result.isSuccessful) {
//                // use the cropped image
//                croppedPhotoUri = result.uriContent!!
//                toggleClassifier()
//            } else {
//                // an error occurred cropping
//                val exception = result.error
//            }
//        }
//
//        val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
//            val cropOptions = CropImageContractOptions(uri, CropImageOptions())
//            imageCropLauncher.launch(cropOptions)
//        }
//
//        imagePickerLauncher.launch("image/*")
//
//        if (imageUri != null) {
//            if (Build.VERSION.SDK_INT < 28) {
//                bitmap.value = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
//            } else {
//                val source = ImageDecoder.createSource(context.contentResolver, imageUri!!)
//                bitmap.value = ImageDecoder.decodeBitmap(source)
//            }
//            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
//                Text("Pick image to crop")
//            }
//        }
    }

    @Composable
    fun ImagePicker(madePhotoUri: Uri) {
        var photoUri by remember {
            mutableStateOf<Uri?>(madePhotoUri)
        }

        val context = LocalContext.current
        var bitmap by remember {
            mutableStateOf<Bitmap?>(null)
        }

        Scaffold(modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                photoUri?.let {
                    if (Build.VERSION.SDK_INT < 28)
                        bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    else {
                        val source = ImageDecoder.createSource(context.contentResolver, it)
                        bitmap = ImageDecoder.decodeBitmap(
                            source,
                            ImageDecoder.OnHeaderDecodedListener { decoder, info, source ->
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                decoder.isMutableRequired = true
                            })
                    }
                }

                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Image from the gallery",
                        Modifier.size(400.dp)
                    )
                    Spacer(modifier = Modifier.padding(20.dp))

                    val scaledBitmap = Bitmap.createScaledBitmap(it, imageSize, imageSize, false);
                    TensorFLowHelper.classifyImage(scaledBitmap) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Image is classified as:")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = it.first, color = Color.Black, fontSize = 20.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "by " + it.second, color = Color.DarkGray, fontSize = 18.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "(${if(it.third.toInt() > 50){it.third.toInt() * 1.5}else{it.third.toInt()}}%)", color = Color.Gray, fontSize = 16.sp)
                            }
                        }
                    }

                }

                Spacer(modifier = Modifier.padding(20.dp))

                Button(onClick = {
                    changeData()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Take Another Photo")
                }
            }
        }
    }
}