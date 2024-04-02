package isomora.com.greendoctor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import info.hannes.github.AppUpdateHelper
import isomora.com.greendoctor.databinding.ActivityMainBinding
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private lateinit var classifier: Classifier
    private lateinit var bitmap: Bitmap

    private val cameraRequestCode = 0
    private val galleryRequestCode = 2

    private val inputSize = 224
    private val modelPath = "plant_disease_model.tflite"
    private val labelPath = "plant_labels.txt"
    private val samplePath = "soybean.JPG"

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        setSupportActionBar(binding.toolbar)

        setContentView(R.layout.activity_main)
        classifier = Classifier(assets, modelPath, labelPath, inputSize)

        resources.assets.open(samplePath).use {
            bitmap = BitmapFactory.decodeStream(it)
            bitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            binding.photoImageView.setImageBitmap(bitmap)
        }

        binding.cameraButton.setOnClickListener {
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(callCameraIntent, cameraRequestCode)
        }

        binding.galleryButton.setOnClickListener {
            val callGalleryIntent = Intent(Intent.ACTION_PICK)
            callGalleryIntent.type = "image/*"
            startActivityForResult(callGalleryIntent, galleryRequestCode)
        }
        binding.detectButton.setOnClickListener {
            val results = classifier.recognizeImage(bitmap).firstOrNull()
            binding.resultTextView.text = results?.title + "\n Confidence:" + results?.confidence
        }

        AppUpdateHelper.checkForNewVersion(
            this,
            gitRepoUrl = BuildConfig.GIT_REPOSITORY
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequestCode) {
            //Considérons le cas de la caméra annulée
            if (resultCode == Activity.RESULT_OK && data != null) {
                bitmap = data.extras!!.get("data") as Bitmap
                bitmap = scaleImage(bitmap)
                val toast = Toast.makeText(this, ("Image crop to: w= ${bitmap.width} h= ${bitmap.height}"), Toast.LENGTH_LONG)
                toast.setGravity(Gravity.BOTTOM, 0, 20)
                toast.show()
                binding.photoImageView.setImageBitmap(bitmap)
                binding.resultTextView.text = "Your photo image set now."
            } else {
                Toast.makeText(this, "Camera cancel..", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == galleryRequestCode) {
            if (data != null) {
                val uri = data.data

                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                println("Success!!!")
                bitmap = scaleImage(bitmap)
                binding.photoImageView.setImageBitmap(bitmap)
            }
        } else {
            Toast.makeText(this, "Unrecognized request code", Toast.LENGTH_LONG).show()
        }
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
