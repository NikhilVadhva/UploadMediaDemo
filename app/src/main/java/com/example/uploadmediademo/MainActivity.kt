package com.example.uploadmediademo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() , AppUtil.OnPermissionGrantListener{

    private val PICK_IMAGES_CODE = 101;
    private val TAG = "MyImages"


    // store compressed images path of picked images
    private val imageList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initListener()

    }

    private fun initListener() {
        btUpload.setOnClickListener()
        {
            if(isReadFilePermissionGranted())
            {
                pickImages()
            } else {
                AppUtil.askFilePermission(this, this)
            }
        }
    }


    fun isReadFilePermissionGranted(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }


    private fun pickImages() {
        val intent= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "images/*"
        //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        //intent.action =
       // intent.addCategory(Intent.CATEGORY_OPENABLE)
        //intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, "Select Image(s)"), PICK_IMAGES_CODE)
    }


    private fun callUploadImageAPI()
    {
        // passing multiPartList to Worker class
        val gson  = Gson()
        val imagesString = gson.toJson(imageList)
        val data = Data.Builder().putString("MyImages",imagesString)
        val uploadImageRequest : WorkRequest = OneTimeWorkRequestBuilder<UploadImages>()
                                          .setInputData(data.build()).build()
        WorkManager.getInstance(this).enqueue(uploadImageRequest)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES_CODE && resultCode == Activity.RESULT_OK) {
            // pick multiple images
            if (data!!.clipData != null) {
                // get number of images
                val count = data.clipData!!.itemCount
                Log.i(TAG,"File Count : "+count)
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    Log.i(TAG,"ImageUri "+i+": "+imageUri)
                    val fileSize = AppUtil.getFileSize(this,imageUri)
                    Log.i(TAG,"Image Size Before Compressed: "+fileSize)
                    val compressedImage: String? = AppUtil.getCompressImage(this,imageUri)
                    Log.i(TAG,"Compressed Image Uri "+i+": "+compressedImage)
                    imageList.add(compressedImage!!)
                    //Log.i(TAG,""+imageList[i])
                }

            } else {
                // picked single images
                val imageUri = data.data!!
                Log.i(TAG,"ImageUri :"+imageUri)
                val compressedImage: String? = AppUtil.getCompressImage(this,imageUri)
                Log.i(TAG,"Compressed Image Uri :"+compressedImage)
                imageList.add(compressedImage!!)
                //Log.i(TAG,""+imageList[0])
            }

            callUploadImageAPI()

        }
    }

    override fun onPermissionGrantSuccess() {
        pickImages()
    }
}