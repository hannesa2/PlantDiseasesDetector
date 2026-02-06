package isomora.com.greendoctor

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import info.hannes.github.AppUpdateHelper
import isomora.com.greendoctor.databinding.ActivityMainBinding
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var classifier: Classifier
    private lateinit var bitmap: Bitmap

    private val inputSize = 224
    private val modelPath = "plant_disease_model.tflite"
    private val labelPath = "plant_labels.txt"
    private val samplePath = "soybean.JPG"

    private lateinit var binding: ActivityMainBinding

    // Activity Result Launchers
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        setSupportActionBar(binding.toolbar)

        setContentView(R.layout.activity_main)

        // Initialize Activity Result Launchers
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.extras?.getParcelable("data")
                }
                if (imageBitmap != null) {
                    bitmap = scaleImage(imageBitmap)
                    val toast = Toast.makeText(this, ("Image crop to: w= ${bitmap.width} h= ${bitmap.height}"), Toast.LENGTH_LONG)
                    toast.show()
                    binding.photoImageView.setImageBitmap(bitmap)
                    binding.resultTextView.text = "Your photo image set now."
                }
            } else {
                Toast.makeText(this, "Camera cancel..", Toast.LENGTH_LONG).show()
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                try {
                    bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }
                    Log.d("GreenDoctor", "Success!!!")
                    bitmap = scaleImage(bitmap)
                    binding.photoImageView.setImageBitmap(bitmap)
                    binding.resultTextView.text = "Your photo image set now."
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_LONG).show()
                }
            }
        }

        classifier = Classifier(assets, modelPath, labelPath, inputSize)

        resources.assets.open(samplePath).use {
            bitmap = BitmapFactory.decodeStream(it)
            bitmap = bitmap.scale(inputSize, inputSize)
            binding.photoImageView.setImageBitmap(bitmap)
        }

        findViewById<Button>(R.id.cameraButton).setOnClickListener {
            Log.d("GreenDoctor", "cameraButton")
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(callCameraIntent)
        }

        findViewById<Button>(R.id.galleryButton).setOnClickListener {
            Log.d("GreenDoctor", "galleryButton")
            galleryLauncher.launch("image/*")
        }
        findViewById<Button>(R.id.detectButton).setOnClickListener {
            Log.d("GreenDoctor", "detectButton")
            val results = classifier.recognizeImage(bitmap).firstOrNull()
            binding.resultTextView.text = results?.title + "\n Confidence:" + results?.confidence
        }

        AppUpdateHelper.checkForNewVersion(
            this,
            gitRepoUrl = BuildConfig.GIT_REPOSITORY
        )
    }


    private fun scaleImage(bitmapScale: Bitmap): Bitmap {
        val originalWidth = bitmapScale.width
        val originalHeight = bitmapScale.height
        val scaleWidth = inputSize.toFloat() / originalWidth
        val scaleHeight = inputSize.toFloat() / originalHeight
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmapScale, 0, 0, originalWidth, originalHeight, matrix, true)
    }

}
