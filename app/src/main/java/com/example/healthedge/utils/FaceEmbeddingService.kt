package com.example.healthedge.utils

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FaceEmbeddingService(private val context: Context) {
    private val interpreter: Interpreter

    init {
        val modelFile = loadModelFile(context, "MobileFaceNet.tflite")
        interpreter = Interpreter(modelFile)
    }

    fun getFaceEmbedding(bitmap: Bitmap): FloatArray {
        val inputSize = 112
        // Prepare a batch of 2 images: the real face and a dummy (all zeros)
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = Array(2) { Array(inputSize) { Array(inputSize) { FloatArray(3) } } }
        // Fill the first image with the real face
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = resized.getPixel(x, y)
                input[0][y][x][0] = ((pixel shr 16 and 0xFF) / 255.0f - 0.5f) * 2 // R
                input[0][y][x][1] = ((pixel shr 8 and 0xFF) / 255.0f - 0.5f) * 2  // G
                input[0][y][x][2] = ((pixel and 0xFF) / 255.0f - 0.5f) * 2        // B
            }
        }
        // The second image is already zeros (dummy)
        val output = Array(2) { FloatArray(192) } // Match model's output shape
        interpreter.run(input, output)
        return output[0] // Return the embedding for the first image
    }

    private fun loadModelFile(context: Context, modelName: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        interpreter.close()
    }
} 