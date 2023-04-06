package com.artdetective.androids4sv

import android.graphics.Bitmap
import com.artdetective.androids4sv.ml.Model
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

object TensorFLowHelper {

    val imageSize = 224

    @Composable
    fun classifyImage(image: Bitmap, callback : (@Composable (Pair<String, Float>) -> Unit)) {
        val model: Model = Model.newInstance(LocalContext.current)

        // Creates inputs for reference.
        val inputFeature0 =
            TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(imageSize * imageSize)
        image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)
        var pixel = 0
        //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val `val` = intValues[pixel++] // RGB
                byteBuffer.putFloat((`val` shr 16 and 0xFF) * (1f / 1))
                byteBuffer.putFloat((`val` shr 8 and 0xFF) * (1f / 1))
                byteBuffer.putFloat((`val` and 0xFF) * (1f / 1))
            }
        }
        inputFeature0.loadBuffer(byteBuffer)

        // Runs model inference and gets result.
        val outputs: Model.Outputs = model.process(inputFeature0)
        val outputFeature0: TensorBuffer = outputs.getOutputFeature0AsTensorBuffer()

        // normalize confidence scores to be between 0 and 1
        val confidences = outputFeature0.floatArray
        val sum = confidences.sum()
        for (i in confidences.indices) {
            confidences[i] /= sum
        }

        // find the index of the class with the biggest confidence.
        val scale = 100.0f
        var maxPos = 0
        var maxConfidence = 0f
        for (i in confidences.indices) {
            val confidence = confidences[i] * scale
            if (confidence > maxConfidence) {
                maxConfidence = confidence
                maxPos = i
            }
        }
        val confidencePercentage = maxConfidence.toInt()
        val classes = arrayOf("Birth of Venus by Sandro Botticelli", "Creation of Adam by Michelangelo Buonarroti", "Guernica by Pablo Picasso",
            "Kiss by Gustav Klimt", "Las Meninas by Diego Velázquez", "Last Supper by Leonardo da Vinci", "Mona Lisa by Leonardo da Vinci",
            "Night Watch by Rembrandt van Rijn", "Scream by Edvard Munch", "Starry Night by Vincent van Gogh")
        callback.invoke(Pair(classes[maxPos], maxConfidence))

        // Releases model resources if no longer used.
        model.close()
    }


}