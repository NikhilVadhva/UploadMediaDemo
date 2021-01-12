package com.example.uploadmediademo

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class UploadImages(context : Context,workerParameters: WorkerParameters) : Worker(context,workerParameters){
    companion object
    {
        const val TASK : String  = "UploadImages"
    }

    override fun doWork(): Result {

       // uploadImages()
        return Result.success()
    }



}