package com.example.uploadmediademo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.DexterError
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val PICK_IMAGES_CODE = 101;
    private val TAG = "MyImages"

    // store uri of picked images
    private val images: ArrayList<Uri>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initListener()

    }

    private fun initListener() {
        btUpload.setOnClickListener()
        {
            requestStoragePermission();
            pickImages()
        }
    }


    private fun pickImages() {
        val intent= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "images/*"
        //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        //intent.action =
       // intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(Intent.createChooser(intent, "Select Image(s)"), PICK_IMAGES_CODE)
    }


    /**
     * Requesting multiple permissions (storage and camera) at once
     * This uses multiple permission model from dexter
     * On permanent denial opens settings dialog
     */
    private fun requestStoragePermission() {
        Dexter.withActivity(this)
            .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    // check if all permissions are granted
                    if (report.areAllPermissionsGranted()) {
                        pickImages()
                    }
                    // check for permanent denial of any permission
                    if (report.isAnyPermissionPermanentlyDenied) {
                        // show alert dialog navigating to Settings
                        pickImages()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest?>?,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            })
            .withErrorListener { error: DexterError? ->
                Toast.makeText(applicationContext, "Error occurred! ", Toast.LENGTH_SHORT)
                    .show()
            }
            .onSameThread()
            .check()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES_CODE && resultCode == Activity.RESULT_OK) {
            // pick multiple images
            if (data!!.clipData != null) {
                // get number of images
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    images!!.add(imageUri)
                    Log.i(TAG,""+images[i])
                }


            } else {
                // picked single images
                val imageUri = data.data!!
                images!!.add(imageUri)
                Log.i(TAG,""+images[0])
            }
        }
    }
}