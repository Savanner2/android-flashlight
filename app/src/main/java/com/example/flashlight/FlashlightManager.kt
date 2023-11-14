package com.example.flashlight

import android.content.Context
import android.hardware.camera2.CameraManager

class FlashlightManager(val context: Context) {
    var cameraId: String = "0"
    val cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun turnOn() {
        try {
            cameraManager.setTorchMode(cameraId, true)
//            shortToast(context, "Torch turned on")
        } catch (e: Exception) {
            e.printStackTrace()
            shortToast(context, e.message)
        }
    }

    fun turnOff() {
        try {
            cameraManager.setTorchMode(cameraId, false)
//            shortToast(context, "Torch turned off")
        } catch (e: Exception) {
            e.printStackTrace()
            shortToast(context, e.message)
        }
    }

    private var epilepsyController = false
    fun epilepsy() {
        try {
            cameraManager.setTorchMode(cameraId, epilepsyController)
            epilepsyController = !epilepsyController
        } catch (e: Exception) {
            e.printStackTrace()
            shortToast(context, e.message)
        }
    }
}