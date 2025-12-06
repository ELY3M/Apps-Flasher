package own.appsflasher

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.util.Log


class CameraAccess(private val mContext: Context) {
    companion object {
        private var isFlashlightOn = false
        private var shouldStroboscopeStop = false
        private var isStroboscopeRunning = false
        private var shouldEnableFlashlight = false
        private var index = 0
    }

    private val camManager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun startStroboscope(){
        Log.d("appsflash", "Strobe Started")
        Thread(stroboscope).start()
        shouldStroboscopeStop = false
        isStroboscopeRunning = false
    }

    fun stopStroboscope(){
        Log.d("appsflash", "Strobe Stopped")
        shouldStroboscopeStop = true
        isStroboscopeRunning = false
    }


    fun toggleStroboscope(): Boolean {
        if (!isStroboscopeRunning) {
            disableFlashlight()
        }

        if (isStroboscopeRunning) {
            stopStroboscope()
        } else {
            Thread(stroboscope).start()
        }

        return true
    }

    private val stroboscope = Runnable {
        if (isStroboscopeRunning) {
            return@Runnable
        }

        shouldStroboscopeStop = false
        isStroboscopeRunning = true

        Log.d("appsflash", "switchFlash: ")
        try {
            val cameraId = camManager.cameraIdList[0]

            val camChars = camManager.getCameraCharacteristics(cameraId)

            val isTorchAvailable = camChars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE).toString().toBoolean()
            Log.d("appsflash", "isTorchAvailable: $isTorchAvailable")

            if(isTorchAvailable) {
                index = 0
                while (!shouldStroboscopeStop) {
                    try {
                        camManager.setTorchMode(cameraId, true)
                        Thread.sleep(50) //timer between flashes
                        camManager.setTorchMode(cameraId, false)
                        Thread.sleep(50) //timer between flashes
                        ///Log.d("appsflash", "CameraAccess Pass ")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        shouldStroboscopeStop = true
                    }
                }
                isStroboscopeRunning = false
            }
        } catch (e:CameraAccessException) {
            e.printStackTrace()
        }

        isStroboscopeRunning = false
        shouldStroboscopeStop = false
        if (shouldEnableFlashlight) {
            enableFlashlight()
            shouldEnableFlashlight = false
        }
    }

    private fun stateChanged(isEnabled: Boolean) {
        isFlashlightOn = isEnabled
    }

    private fun disableFlashlight() {
        if (isStroboscopeRunning) {
            return
        }
        camManager.cameraIdList.forEach {
            val camChars = camManager.getCameraCharacteristics(it)
            val isTorchAvailable = camChars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE).toString().toBoolean()
            if(isTorchAvailable)
                camManager.setTorchMode(it,false)
        }
        stateChanged(false)
    }

    private fun enableFlashlight() {
        shouldStroboscopeStop = true
        if (isStroboscopeRunning) {
            shouldEnableFlashlight = true
            return
        }
        camManager.cameraIdList.forEach {
            val camChars = camManager.getCameraCharacteristics(it)
            val isTorchAvailable = camChars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE).toString().toBoolean()
            if(isTorchAvailable)
                camManager.setTorchMode(it,true)
        }

        val mainRunnable = Runnable { stateChanged(true) }
        Handler(mContext.mainLooper).post(mainRunnable)
    }
}