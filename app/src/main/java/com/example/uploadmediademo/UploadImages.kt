package com.example.uploadmediademo

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MultipartBody

class UploadImages(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    companion object {
        const val TASK: String = "UploadImages"
    }

    override fun doWork(): Result {
        // uploadImages()
        val multiPartImgList = ArrayList<MultipartBody.Part>()
        val IMAGE_KEY= ""
        val imageList : List<String>
        val getImages = inputData.getString("MyImages") ?: return Result.failure()

        val gson  = Gson()
        imageList =gson.fromJson(getImages,Array<String>::class.java).toList()
        for (i in 0 until imageList.size) {
            multiPartImgList.add(AppUtil.getMediaMultiPart(imageList.get(i), IMAGE_KEY))
        }
        return Result.success()
    }


}