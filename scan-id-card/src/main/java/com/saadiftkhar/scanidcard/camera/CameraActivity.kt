package com.saadiftkhar.scanidcard.camera

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.saadiftkhar.scanidcard.R
import com.saadiftkhar.scanidcard.databinding.ActivityCameraBinding
import com.saadiftkhar.scanidcard.utils.CommonUtils
import com.saadiftkhar.scanidcard.utils.FileUtils
import com.saadiftkhar.scanidcard.utils.ImageUtils
import com.saadiftkhar.scanidcard.utils.PermissionUtils
import com.saadiftkhar.scanidcard.utils.ScreenUtils
import com.saadiftkhar.toaster.Toaster
import java.io.File

class CameraActivity : AppCompatActivity(),View.OnClickListener {

    private lateinit var binding: ActivityCameraBinding

    private var mCropBitmap: Bitmap? = null

    private var mType = 0
    private var isToast = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)

        val checkPermissionFirst: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionUtils.checkPermissionFirst(
                this, ScanIDCard.PERMISSION_CODE_FIRST, arrayOf(
                    Manifest.permission.CAMERA
                )
            )
        } else {
            PermissionUtils.checkPermissionFirst(
                this, ScanIDCard.PERMISSION_CODE_FIRST, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
            )
        }

        if (checkPermissionFirst) {
            init()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.cameraPreview.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.cameraPreview.onStop()
    }

    override fun onClick(v: View) {
        when (v) {
            binding.cameraPreview -> {
                binding.cameraPreview.focus()
            }

            binding.ivCameraClose -> {
                finish()
            }

            binding.ivCameraTake -> {
                if (CommonUtils.isFastClick().not()) {
                    takePhoto()
                }
            }

            binding.ivCameraFlash -> {
                if (CameraUtils.hasFlash(this)) {
                    val isFlashOn = binding.cameraPreview.switchFlashLight()
                    binding.ivCameraFlash.setImageResource(if (isFlashOn) R.drawable.ic_flash_on_sic else R.drawable.ic_flash_off_sic)
                } else {
                    Toaster.info(this, getString(R.string.no_flash))
                }
            }

            binding.ivResultOk -> {
                confirm()
            }

            binding.ivResultCancel -> {
                binding.cameraPreview.isEnabled = true
                binding.cameraPreview.addCallback()
                binding.cameraPreview.startPreview()
                binding.ivCameraFlash.setImageResource(R.drawable.ic_flash_off_sic)
                setTakePhotoLayout()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var isPermissions = true
        for (i in permissions.indices) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                isPermissions = false
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i]!!).not()) { //The user selected "Don't ask again"
                    if (isToast) {
                        Toaster.info(this, getString(R.string.manually_open_permission))
                        isToast = false
                    }
                }
            }
        }
        isToast = true
        if (isPermissions) {
            Log.d("onRequestPermission", "onRequestPermissionsResult: " + "Allow all permissions")
            init()
        } else {
            Log.d(
                "onRequestPermission",
                "onRequestPermissionsResult: " + "Permission is not allowed"
            )
            finish()
        }
    }

    private fun init() {
        setContentView(binding.root)
        mType = intent.getIntExtra(ScanIDCard.TAKE_TYPE, 0)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        initView()
        initListener()
    }

    private fun initView() {
        val screenMinSize =
            Math.min(ScreenUtils.getScreenWidth(this), ScreenUtils.getScreenHeight(this)).toFloat()
        val screenMaxSize =
            Math.max(ScreenUtils.getScreenWidth(this), ScreenUtils.getScreenHeight(this)).toFloat()
        val height = (screenMinSize * 0.75).toInt().toFloat()
        val width = (height * 75.0f / 47.0f).toInt().toFloat()

        //Get the width of the bottom "operation area"
        val flCameraOptionWidth = (screenMaxSize - width) / 2
        val containerParams =
            LinearLayout.LayoutParams(width.toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
        val cropParams = LinearLayout.LayoutParams(width.toInt(), height.toInt())
        val cameraOptionParams = LinearLayout.LayoutParams(
            flCameraOptionWidth.toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        binding.llCameraCropContainer.layoutParams = containerParams
        binding.ivCameraCrop.layoutParams = cropParams

        //Get the width of the "camera cropping area" to dynamically set the width of the bottom "operation area" to center the "camera cropping area"
        binding.flCameraOption.layoutParams = cameraOptionParams
        when (mType) {
            ScanIDCard.TYPE_IDCARD_FRONT -> binding.ivCameraCrop.setImageResource(R.drawable.ic_id_card_frame_sic)
            ScanIDCard.TYPE_IDCARD_BACK -> binding.ivCameraCrop.setImageResource(R.drawable.ic_id_card_frame_sic)
        }

        /*Added a 0.5-second transition interface to solve the problem of slow startup of the preview interface caused by individual mobile phones applying for permission for the first time.*/
        Handler(Looper.getMainLooper()).postDelayed(
            { runOnUiThread { binding.cameraPreview.isVisible = true } }, 500
        )
    }

    private fun initListener() {
        binding.cameraPreview.setOnClickListener(this)
        binding.ivCameraFlash.setOnClickListener(this)
        binding.ivCameraClose.setOnClickListener(this)
        binding.ivCameraTake.setOnClickListener(this)
        binding.ivResultOk.setOnClickListener(this)
        binding.ivResultCancel.setOnClickListener(this)
    }
    
    private fun takePhoto() {
        binding.cameraPreview.isEnabled = false
        CameraUtils.getCamera().setOneShotPreviewCallback { bytes, camera ->
            val size = camera.parameters.previewSize
            camera.stopPreview()
            Thread {
                val w = size.width
                val h = size.height
                val bitmap =
                    ImageUtils.getBitmapFromByte(bytes, w, h)
                cropImage(bitmap)
            }.start()
        }
    }


    private fun cropImage(bitmap: Bitmap) {
        val left = binding.viewCameraCropLeft.width.toFloat()
        val top = binding.ivCameraCrop.top.toFloat()
        val right = binding.ivCameraCrop.right.plus(left)
        val bottom = binding.ivCameraCrop.bottom.toFloat()
        val leftProportion = left / binding.cameraPreview.width
        val topProportion = top / binding.cameraPreview.height
        val rightProportion = right / binding.cameraPreview.width
        val bottomProportion = bottom / binding.cameraPreview.bottom

        mCropBitmap = Bitmap.createBitmap(
            bitmap,
            (leftProportion * bitmap.width.toFloat()).toInt(),
            (topProportion * bitmap.height.toFloat()).toInt(),
            ((rightProportion - leftProportion) * bitmap.width.toFloat()).toInt(),
            ((bottomProportion - topProportion) * bitmap.height.toFloat()).toInt()
        )
        runOnUiThread {
            binding.cropImageView.layoutParams =
                LinearLayout.LayoutParams(binding.ivCameraCrop.width, binding.ivCameraCrop.height)
            setCropLayout()
            binding.cropImageView.setImageBitmap(mCropBitmap)
        }
    }


    private fun setCropLayout() {
        binding.ivCameraCrop.isVisible = false
        binding.cameraPreview.isVisible = false
        binding.clCameraOption.isVisible = false
        binding.cropImageView.isVisible = true
        binding.clCameraResult.isVisible = true
        binding.tvInstructions.text = ""
    }


    private fun setTakePhotoLayout() {
        binding.ivCameraCrop.isVisible = true
        binding.cameraPreview.isVisible = true
        binding.clCameraOption.isVisible = true
        binding.cropImageView.isVisible = false
        binding.clCameraResult.isVisible = false
        binding.tvInstructions.text = getString(R.string.touch_to_focus)
        binding.cameraPreview.focus()
    }

    private fun confirm() {
        binding.cropImageView.crop({ bitmap ->
            if (bitmap == null) {
                Toaster.error(applicationContext, getString(R.string.crop_fail))
                finish()
            }
            val imagePath =
                StringBuffer().append(FileUtils.getImageCacheDir(this@CameraActivity)).append(
                    File.separator
                ).append(System.currentTimeMillis()).append(".jpg").toString()

            if (ImageUtils.save(bitmap, imagePath, Bitmap.CompressFormat.JPEG)) {
                val intent = Intent()
                intent.putExtra(ScanIDCard.IMAGE_PATH, imagePath)
                setResult(ScanIDCard.RESULT_CODE, intent)
                finish()
            }

        }, true)
    }
}