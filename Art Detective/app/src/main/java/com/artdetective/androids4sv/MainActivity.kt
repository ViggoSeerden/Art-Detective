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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
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
import com.artdetective.androids4sv.ui.theme.Music
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.artdetective.androids4sv.ui.theme.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign

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
            shouldShowCamera.value = false
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
                    modifier = Modifier
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
                shouldShowCamera.value = false
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
            modifier = Modifier
                .fillMaxSize()
//              .border(width = 1.dp, Color.Green)
        ) {val painter: Painter = rememberImagePainter(
                data = R.drawable.monalisa,

            )
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                ) {
                    Text(
                        text = "Which one do you want to check?",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                ) {
                    Button(
                        modifier = Modifier
                            .width(280.dp)
                            .height(120.dp)
                            .padding(10.dp),
                        shape = RoundedCornerShape(20.dp),
                        onClick = { changeData() },
                        colors = ButtonDefaults
                            .buttonColors(backgroundColor = Art, contentColor = Color.White)

                    ) {
                        Text(text = "Art")
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                ) {
                    Button(
                        modifier = Modifier
                            .width(280.dp)
                            .height(120.dp)
                            .padding(10.dp),
                        shape = RoundedCornerShape(20.dp),
                        onClick = { changeData() },
                        colors = ButtonDefaults
                            .buttonColors(backgroundColor = Sculptures, contentColor = Color.White)
                    ) {
                        Text(text = "Sculptures")
                    }
                }
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                ) {
                    Button(
                        modifier = Modifier
                            .width(280.dp)
                            .height(120.dp)
                            .padding(10.dp),
                        shape = RoundedCornerShape(20.dp),
                        onClick = {},
                        colors = ButtonDefaults
                            .buttonColors(backgroundColor = Music, contentColor = Color.White)
                    ) {
                        Text(text = "Music")
                    }
                }
                Row(verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(2.dp, Color.White),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .height(80.dp)
                            .width(90.dp),
                        onClick = { Log.e("wow", "Hello") }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Home,
                                "Home",
                                tint = Color.White,)
                            Text(
                                text = "Home",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(2.dp, Color.White),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .height(80.dp)
                            .width(90.dp),
                        onClick = { Log.e("wow", "Hello") }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Search,
                                "Search",
                                tint = Color.White,)
                            Text(
                                text = "Search",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(2.dp, Color.White),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .height(80.dp)
                            .width(90.dp),
                        onClick = { Log.e("wow", "Hello") }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ShoppingCart,
                                "Shop",
                                tint = Color.White,)
                            Text(
                                text = "Shop",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(2.dp, Color.White),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .height(80.dp)
                            .width(90.dp),
                        onClick = { Log.e("wow", "Hello") }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AccountCircle,
                                "Profile",
                                tint = Color.White,)
                            Text(
                                text = "Profile",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

        }
    }

    @Composable
    fun CropButton() {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp)
//                .border(width = 1.dp, Color.Green)
        ) {
            Row(horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()) {
                Button(modifier = Modifier
                    .width(105.dp)
                    .height(60.dp),
                    onClick = { toggleCropper() },
                    colors = ButtonDefaults
                        .buttonColors(backgroundColor = Art, contentColor = Color.White)
                ) {
                    Text(text = "Crop Image",fontSize = 16.sp, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.padding(7.dp))
                Button(
                    modifier = Modifier
                        .width(105.dp)
                        .height(60.dp),
                    onClick = { croppedPhotoUri = photoUri
                        toggleClassifier() },
                    colors = ButtonDefaults
                        .buttonColors(backgroundColor = Sculptures, contentColor = Color.White)
                ) {
                    Text(text = "Classify Image", fontSize = 16.sp, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.padding(7.dp))
                Button(modifier = Modifier
                    .width(105.dp)
                    .height(60.dp),
                    onClick = { changeData() },
                    colors = ButtonDefaults
                        .buttonColors(backgroundColor = Music, contentColor = Color.White)
                ) {
                    Text(text = "Retake Photo", fontSize = 16.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }

    private fun changeToHome() {
        shouldShowCamera.value = false
        shouldShowPhoto.value = false
        showImageCropper.value = false
        showImageClassifier.value = false
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
                Modifier
                    .fillMaxSize()
                    .background(color = Sculptures),
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
                    Spacer(modifier = Modifier.padding(5.dp))

                    val scaledBitmap = Bitmap.createScaledBitmap(it, imageSize, imageSize, false);
                    TensorFLowHelper.classifyImage(scaledBitmap) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(text = "Image is classified as:" , color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = it.first, color = Color.White, fontSize = 20.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "by " + it.second, color = Color.White, fontSize = 18.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "(${if(it.third.toInt() > 50){it.third.toInt() * 1.5}else{it.third.toInt()}}%)", color = Color.LightGray, fontSize = 16.sp)
                            }
                        }
                    }

                }

                Spacer(modifier = Modifier.padding(11.dp))

                Button(onClick = { changeData() },
                    modifier = Modifier
                        .width(200.dp)
                        .height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults
                        .buttonColors(backgroundColor = Color.Black,
                            contentColor = Color.White)) {
                    Text(text = "Take Another Photo")
                }

                Spacer(modifier = Modifier.padding(11.dp))

                Row(verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(2.dp, Color.White),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .height(80.dp)
                            .width(90.dp),
                        onClick = {changeToHome()}
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Home,
                                "Home",
                                tint = Color.White,
                            )
                            Text(
                                text = "Home",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(2.dp, Color.White),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .height(80.dp)
                            .width(90.dp),
                        onClick = { Log.e("wow", "Hello") }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Search,
                                "Search",
                                tint = Color.White,
                            )
                            Text(
                                text = "Search",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(2.dp, Color.White),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .height(80.dp)
                            .width(90.dp),
                        onClick = { Log.e("wow", "Hello") }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                "Shop",
                                tint = Color.White,
                            )
                            Text(
                                text = "Shop",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                    OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(2.dp, Color.White),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .height(80.dp)
                            .width(90.dp),
                        onClick = { Log.e("wow", "Hello") }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                "Profile",
                                tint = Color.White,
                            )
                            Text(
                                text = "Profile",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}