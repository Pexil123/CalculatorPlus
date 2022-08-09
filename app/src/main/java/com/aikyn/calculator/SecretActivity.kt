package com.aikyn.calculator

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.aikyn.calculator.databinding.ActivitySecretBinding
import java.io.IOException
import kotlin.properties.Delegates


class SecretActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecretBinding

    var image_array = ArrayList<Image>()

    var pickedPhoto : Uri? = null
    var pickedBitMap: Bitmap? = null
    var isTrue = true
    var prefs by Delegates.notNull<SharedPreferences>()
    var saveToGallery = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecretBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("TABLE", MODE_PRIVATE)

        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                1)
        }

        saveToGallery = prefs.getBoolean("checkState", true)
        binding.checkBoxSaveAlltoGallery.isChecked = saveToGallery

        binding.imageRecycler.layoutManager = GridLayoutManager(this, 3)
        binding.imageRecycler.hasFixedSize()

        binding.buttonAdd.setOnClickListener {
            val galeriIntext = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galeriIntext,2)
        }

        binding.buttonClearAll.setOnClickListener {
            clearInternalStorage()
            loadPhotosFromInternalStorageIntoRV()
        }

        binding.buttonChangePassword.setOnClickListener {
            showInputDialog()
        }

        binding.checkBoxSaveAlltoGallery.setOnCheckedChangeListener { cb, b ->
            saveCheckBoxState(binding.checkBoxSaveAlltoGallery.isChecked)
            updateSaveToGallery()
        }
    }

    fun saveCheckBoxState(checkState: Boolean) {
        prefs.edit().apply {
            putBoolean("checkState", checkState)
            apply()
        }
    }

    fun updateSaveToGallery() {
        saveToGallery = binding.checkBoxSaveAlltoGallery.isChecked
    }

    fun showInputDialog() {
        val builder = android.app.AlertDialog.Builder(this)

        val li = LayoutInflater.from(this)
        val promptsView = li.inflate(R.layout.alert_dialog, null)
        val input = promptsView.findViewById<EditText>(R.id.editText)

        builder.setView(promptsView)

        builder.setIcon(R.drawable.ic_lock)

        builder.setPositiveButton("OK") { dialog, which ->
            if (input.text.isNotEmpty()) savePassword(input.text.toString())
        }

        builder.setNegativeButton("Отмена") { dialog, which ->
            dialog.cancel()
        }
        builder.show()
    }

    fun savePassword(password: String) {
        prefs.edit().apply {
            putString("password", password)
            apply()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            pickedPhoto = data.data

            if (Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(this.contentResolver,pickedPhoto!!)
                pickedBitMap = ImageDecoder.decodeBitmap(source)
            }
            else {
                pickedBitMap = MediaStore.Images.Media.getBitmap(this.contentResolver,pickedPhoto)
            }

            val name = getImageFileName(pickedPhoto!!)

            savePhotoToInternalStorage(name, pickedBitMap!!)

        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun clearInternalStorage() {
        binding.progressCircular.visibility = View.VISIBLE
        isTrue = false
        image_array.clear()
        binding.imageRecycler.adapter = ImageAdapter(image_array, this)
        if (saveToGallery) {
            filesDir.listFiles()!!.filter { it.canRead() && it.isFile }.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) savePhotoToExternalStorage(it.name, bmp)
                it.delete()
            }
        } else {
            filesDir.listFiles()!!.filter { it.canRead() && it.isFile }.map {
                it.delete()
            }
        }

        binding.progressCircular.visibility = View.GONE
        isTrue = true
    }

    override fun onBackPressed() {
        if (isTrue) super.onBackPressed()
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

    private fun loadPhotosFromInternalStorageIntoRV() {
        binding.progressCircular.visibility = View.VISIBLE
        image_array.clear()
        filesDir.listFiles()!!.filter { it.canRead() && it.isFile }.map {
            val bytes = it.readBytes()
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) image_array.add(Image(bmp, it.name))
        }
        binding.imageRecycler.adapter = ImageAdapter(image_array, this)
        binding.progressCircular.visibility = View.GONE
    }

    private fun savePhotoToInternalStorage(filename: String, bmp: Bitmap): Boolean {
        return try {
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                openFileOutput(filename, MODE_PRIVATE).use { stream ->
                    if(!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                        throw IOException("Couldn't save bitmap.")
                    }
                }
            } else if (filename.endsWith(".png")) {
                openFileOutput(filename, MODE_PRIVATE).use { stream ->
                    if(!bmp.compress(Bitmap.CompressFormat.PNG, 95, stream)) {
                        throw IOException("Couldn't save bitmap.")
                    }
                }
            } else if (filename.endsWith(".webp")) {
                openFileOutput(filename, MODE_PRIVATE).use { stream ->
                    if(!bmp.compress(Bitmap.CompressFormat.WEBP, 95, stream)) {
                        throw IOException("Couldn't save bitmap.")
                    }
                }
            } else {
                throw IOException("Couldn't save bitmap.")
            }

            true
        } catch(e: IOException) {
            e.printStackTrace()
            false
        }

    }

    fun getImageFileName(uri: Uri): String {
        var path: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, proj, null, null, null)
        if (cursor!!.moveToFirst()) {
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            path = cursor.getString(column_index)
        }
        cursor.close()
        val name = path?.substring(path.lastIndexOf("/").plus(1))!!
        return name
    }

    override fun onStart() {
        super.onStart()
        loadPhotosFromInternalStorageIntoRV()
    }

}