package com.aikyn.calculator

import android.content.ContentValues
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import com.aikyn.calculator.databinding.ActivityImageFullBinding
import com.bumptech.glide.Glide
import java.io.File
import java.io.IOException
import kotlin.properties.Delegates

class ImageFullActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageFullBinding
    var saveToGallery = true
    var prefs by Delegates.notNull<SharedPreferences>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageFullBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("TABLE", MODE_PRIVATE)

        saveToGallery = prefs.getBoolean("checkState2", true)
        binding.checkBoxSavetoGallery.isChecked = saveToGallery

        val i = intent

        val name = i.getStringExtra("name")

        val file = File("$filesDir/$name")

        val bytes = file.readBytes()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        binding.title.text = name

        Glide.with(this)
            .load(bitmap)
            .into(binding.fullImage)

        binding.buttonDelete.setOnClickListener {
            if (saveToGallery) savePhotoToExternalStorage(name!!, bitmap)
            file.delete()
            finish()
        }

        binding.buttonRestore.setOnClickListener {
            if (saveToGallery) savePhotoToExternalStorage(name!!, bitmap)
        }

        binding.checkBoxSavetoGallery.setOnCheckedChangeListener { compoundButton, b ->
            saveCheckBoxState(binding.checkBoxSavetoGallery.isChecked)
            updateSaveToGallery()
        }

    }

    fun saveCheckBoxState(checkState: Boolean) {
        prefs.edit().apply {
            putBoolean("checkState2", checkState)
            apply()
        }
    }

    fun updateSaveToGallery() {
        saveToGallery = binding.checkBoxSavetoGallery.isChecked
    }

    private fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap): Boolean {
        val imageCollection = sdk29AndUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)

            if (displayName.endsWith(".webp"))  put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
            if (displayName.endsWith(".png"))  put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (displayName.endsWith(".jpeg") || displayName
                    .endsWith(".jpg"))  put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

            put(MediaStore.Images.Media.WIDTH, bmp.width)
            put(MediaStore.Images.Media.HEIGHT, bmp.height)
        }
        return try {
            contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (displayName.endsWith(".jpeg") || displayName.endsWith(".jpg")) {
                        if(!bmp.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                            throw IOException("Couldn't save bitmap")
                        }
                    }
                    if (displayName.endsWith(".webp")) {
                        if(!bmp.compress(Bitmap.CompressFormat.WEBP, 95, outputStream)) {
                            throw IOException("Couldn't save bitmap")
                        }
                    }
                    if (displayName.endsWith(".png")) {
                        if(!bmp.compress(Bitmap.CompressFormat.PNG, 95, outputStream)) {
                            throw IOException("Couldn't save bitmap")
                        }
                    }
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch(e: IOException) {
            e.printStackTrace()
            false
        }
    }

}