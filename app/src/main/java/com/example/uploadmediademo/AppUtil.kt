package com.example.uploadmediademo

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.MultipartBody.Part.Companion.createFormData
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


object AppUtil {

    private val TAG = "MyImages"

    @SuppressLint("ObsoleteSdkInt")
    fun getRealPathFromURI(context: Context, uri: Uri): String? {

        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return context.getExternalFilesDir(null)!!.absolutePath + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                    cursor!!.moveToNext()
                    val fileName = cursor.getString(0)
                    val path = context.getExternalFilesDir(null)!!.absolutePath + "/Download/" + fileName
                    if (!TextUtils.isEmpty(path)) {
                        return path
                    }
                } finally {
                    cursor?.close()
                }
                val id = DocumentsContract.getDocumentId(uri)
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:".toRegex(), "")
                }
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads"), java.lang.Long.valueOf(id))

                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)

        return null
    }


    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author Niks
     */
    private fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                              selectionArgs: Array<String>?): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    fun getCompressImage(context: Context,imageUri: Uri): String? {
        val filePath = getRealPathFromURI(context,imageUri)
        var scaledBitmap: Bitmap? = null
        val options = BitmapFactory.Options()

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true
        var bmp = BitmapFactory.decodeFile(filePath, options)
        var actualHeight = options.outHeight
        var actualWidth = options.outWidth

//      max Height and width values of the compressed image is taken as 816x612
        val maxHeight = 816.0f
        val maxWidth = 612.0f
        var imgRatio = actualWidth / actualHeight.toFloat()
        val maxRatio = maxWidth / maxHeight

//      width and height values are set maintaining the aspect ratio of the image
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight
                actualWidth = (imgRatio * actualWidth).toInt()
                actualHeight = maxHeight.toInt()
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth
                actualHeight = (imgRatio * actualHeight).toInt()
                actualWidth = maxWidth.toInt()
            } else {
                actualHeight = maxHeight.toInt()
                actualWidth = maxWidth.toInt()
            }
        }

//      setting inSampleSize value allows to load a scaled down version of the original image
        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true
        options.inInputShareable = true
        options.inTempStorage = ByteArray(16 * 1024)
        try {
//          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()
        val middleX = actualWidth / 2.0f
        val middleY = actualHeight / 2.0f
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
        val canvas = Canvas(scaledBitmap!!)
        canvas.setMatrix(scaleMatrix)
        canvas.drawBitmap(
            bmp,
            middleX - bmp.width / 2,
            middleY - bmp.height / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )

//      check the rotation of the image and display it properly
        val exif: ExifInterface
        try {
            exif = ExifInterface(filePath!!)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, 0
            )
            Log.d("EXIF", "Exif: $orientation")
            val matrix = Matrix()
            when (orientation) {
                6 -> {
                    matrix.postRotate(90f)
                    Log.d("EXIF", "Exif: $orientation")
                }
                3 -> {
                    matrix.postRotate(180f)
                    Log.d("EXIF", "Exif: $orientation")
                }
                8 -> {
                    matrix.postRotate(270f)
                    Log.d("EXIF", "Exif: $orientation")
                }
            }
            scaledBitmap = Bitmap.createBitmap(
                scaledBitmap, 0, 0,
                scaledBitmap.width, scaledBitmap.height, matrix,
                true
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var out: FileOutputStream? = null
        val filename: String? = getFilename(context)
        try {
            out = FileOutputStream(filename)

//          write the compressed bitmap at the destination specified by filename.
            scaledBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, out)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return filename
    }


    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val heightRatio =
                Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio =
                Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        val totalPixels = width * height.toFloat()
        val totalReqPixelsCap = reqWidth * reqHeight * 2.toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }
        return inSampleSize
    }


    private fun getFilename(context: Context): String? {
        val file =
            File(context.getExternalFilesDir(null)!!.absolutePath, "MyFolder/Images")
        Log.i(TAG,"Image Size After Compressed: "+ getFileSize(file))
        if (!file.exists()) {
            file.mkdirs()
        }


        return file.getAbsolutePath()
            .toString() + "/" + System.currentTimeMillis() + ".jpg"
    }

    fun getMediaMultiPart(profileImagePath: String, key: String): MultipartBody.Part {
        val file = File(profileImagePath)
        val requestFileProfilePic =
            file.asRequestBody(getMimeType(profileImagePath)!!.toMediaTypeOrNull())
        return createFormData(key, file.name, requestFileProfilePic)
    }


//    fun getMediaMultiPart(
//        profileImagePath: String,
//        key: String?
//    ): MultipartBody.Part? {
//        val file = File(profileImagePath)
//        val requestFileProfilePic: RequestBody =
//            RequestBody.create(file, getMimeType(profileImagePath)?.toMediaTypeOrNull())
//       // return createFormData.createFormData(key, file.name, requestFileProfilePic)*/
//    }

    private fun getMimeType(url: String): String? {
        val path = Uri.fromFile(File(url))
        val type_map = MimeTypeMap.getSingleton()
        var extension = MimeTypeMap.getFileExtensionFromUrl(path.toString())
        extension = extension.toLowerCase(Locale.ROOT)
        if (extension.contains(".")) {
            extension = extension.substring(extension.lastIndexOf("."))
        }
        return type_map.getMimeTypeFromExtension(extension)
    }


    fun askFilePermission(activity: MainActivity,
        onPermissionGrantListener: OnPermissionGrantListener
    ) {
        val permission: MutableList<String> =
            ArrayList()
        permission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permission.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            requestPermission(
            activity,
            permission,
            onPermissionGrantListener
        )
    }



    /**
     * Requesting multiple permissions (storage and camera) at once
     * This uses multiple permission model from dexter
     * On permanent denial opens settings dialog
     */

    fun requestPermission(
        activity: MainActivity,
        permission: List<String?>?,
        onPermissionGrantListener: OnPermissionGrantListener
    ) {
        Dexter.withActivity(activity).withPermissions(permission)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    // check if all permissions are granted or not
                    if (report.areAllPermissionsGranted()) {
                        onPermissionGrantListener.onPermissionGrantSuccess()
                    }
                    // check for permanent denial of any permission show alert dialog
                    if (report.isAnyPermissionPermanentlyDenied) {
                        // open Settings activity
                        showSettingsDialog(activity)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            })
            .onSameThread()
            .check()
    }


    /**
     * Showing Alert Dialog with Settings option in case of deny any permission
     */
    private fun showSettingsDialog(activity: MainActivity) {
        val snackbar: Snackbar = Snackbar
            .make(
                activity.findViewById(android.R.id.content),
                activity.getString(R.string.permission),
                Snackbar.LENGTH_LONG
            )
            .setAction(
                R.string.snakeBarEnable,
                { v -> openSettings(activity) })
        snackbar.show()
    }

    // navigating settings app
    private fun openSettings(activity: MainActivity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri =
            Uri.fromParts("package", activity.getPackageName(), null)
        intent.data = uri
        activity.startActivityForResult(intent, 101)
    }


    interface OnPermissionGrantListener {
        fun onPermissionGrantSuccess()
    }

     fun  getFileSize(context: Context,filePath : Uri) : String?
    {
        //val file = File(filePath.lastPathSegment.toString())
        val fileSize  = DocumentFile.fromSingleUri(context,filePath)!!.length()
        val fileSizeInBytes = fileSize
        Log.i(TAG,"FileSizeBytes:"+fileSizeInBytes)
        val fileSizeInKB: Float = (fileSizeInBytes/1024).toFloat()
        Log.i(TAG,"FileSizeKB:"+fileSizeInKB)
        val fileSizeInMB: Float = fileSizeInKB/1024
        Log.i(TAG,"FileSizeMB:"+fileSizeInMB)

        return (fileSizeInMB).toString()
    }

    fun  getFileSize(file : File) : String?
    {
        val fileSizeInBytes = file.length()
        Log.i(TAG,"FileSizeBytes:"+fileSizeInBytes)
        val fileSizeInKB: Float = (fileSizeInBytes/1024).toFloat()
        Log.i(TAG,"FileSizeKB:"+fileSizeInKB)
        val fileSizeInMB: Float = fileSizeInKB/1024
        Log.i(TAG,"FileSizeMB:"+fileSizeInMB)

        return (fileSizeInMB).toString()
    }

}