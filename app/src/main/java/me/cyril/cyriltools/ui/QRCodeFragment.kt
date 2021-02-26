package me.cyril.cyriltools.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.*
import me.cyril.cyriltools.R
import me.cyril.cyriltools.base.BaseFragment
import java.util.*
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min

class QRCodeFragment : BaseFragment(), CoroutineScope {

    companion object {
        private const val TYPE = "image/*"
        private const val READ_REQUEST_CODE = 0
        private const val CROP_REQUEST_CODE = 1
        private const val WRITE_REQUEST_CODE = 2
    }

    override val resLayout: Int
        get() = R.layout.fragment_qrcode
    override val resMenu: Int
        get() = R.menu.menu_qr_code
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + SupervisorJob()

    private val ivQRCode by bindView<ImageView>(R.id.qr_code)
    private val etContent by bindView<EditText>(R.id.qr_code_content)

    private var isGenerated = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnEncode = view.findViewById<Button>(R.id.qr_code_encode)
        btnEncode.setOnClickListener {
            etContent?.text?.let {
                try {
                    val bitmap = encode(it.toString())
                    ivQRCode?.setImageBitmap(bitmap)
                    isGenerated = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        ivQRCode?.setOnClickListener {
            selectImage()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bitmap = (ivQRCode?.drawable as? BitmapDrawable)?.bitmap
        bitmap?.run {
            ivQRCode?.setImageBitmap(null)
            recycle()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            val bundle = data?.extras
            when (requestCode) {
                READ_REQUEST_CODE -> {
                    val intent = Intent("com.android.camera.action.CROP").apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setDataAndType(uri, TYPE)
                        putExtra("aspectX", 1)
                        putExtra("aspectY", 1)
                        putExtra("outputX", 500)
                        putExtra("outputY", 500)
                        putExtra("return-data", true)
                    }
                    startActivityForResult(intent,
                        CROP_REQUEST_CODE
                    )
                }
                CROP_REQUEST_CODE -> {
                    val bitmap = bundle?.getParcelable<Bitmap>("data")
                    bitmap?.let {
                        ivQRCode?.setImageBitmap(it)
                        isGenerated = false
                        try {
                            etContent?.setText(decode(it))
                        } catch (e: Exception) {
                            mActivity?.snack("Decoding fail")
                        }
                    }
                }
                WRITE_REQUEST_CODE -> {
                    uri?.run {
                        try {
                            launch(coroutineContext) {
                                val bitmap = (ivQRCode?.drawable as? BitmapDrawable)?.bitmap
                                bitmap?.run {
                                    withContext(Dispatchers.IO) {
                                        val outputStream =
                                            mActivity?.contentResolver?.openOutputStream(uri)
                                        compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                    }
                                }
                                withContext(Dispatchers.Main) { mActivity?.snack("QR code saved") }
                            }
                        } catch (e: Exception) {
                            mActivity?.snack("Saving fail")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save_image -> saveImage()
        }
        return true
    }

    @Suppress("SimpleDateFormat")
    private fun saveImage() {
        if (isGenerated) {
            val time = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = TYPE
                putExtra(Intent.EXTRA_TITLE, "$time.jpg")
            }
            startActivityForResult(intent,
                WRITE_REQUEST_CODE
            )
        } else {
            mActivity?.snack("No generated QR code")
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent,
            READ_REQUEST_CODE
        )
    }

    private fun encode(content: String): Bitmap {
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
//        hints[EncodeHintType.CHARACTER_SET] = CHARSET
        hints[EncodeHintType.MARGIN] = 1

        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 500, 500, hints)
        val pixels = IntArray(500 * 500)
        for (y in 0 until 500) {
            for (x in 0 until 500) {
                pixels[y * 500 + x] = if (bitMatrix.get(x, y)) 0 else 0xffffff
            }
        }

        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.RGB_565)
        bitmap.setPixels(pixels, 0, 500, 0, 0, 500, 500)

        return bitmap
    }

    private fun decode(bitmap: Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        var pixels = width * height
        val aRGB = IntArray(pixels)
        bitmap.getPixels(aRGB, 0, width, 0, 0, width, height)

        val yuv420SP =
            ByteArray(pixels + (if (width % 2 == 0) width else (width + 1)) * (if (height % 2 == 0) height else (height + 1)) / 2)
        var y: Int
        var u: Int
        var v: Int
        var r: Int
        var g: Int
        var b: Int
        var index = 0
        var yIndex = 0
        for (i in 0 until height) {
            for (j in 0 until width) {
                r = aRGB[index].and(0xff0000).shr(16)
                g = aRGB[index].and(0xff00).shr(8)
                b = aRGB[index].and(0xff)
                ++index

                y = (66 * r + 129 * g + 25 * b + 128).shr(8) + 16
                u = (-38 * r - 74 * g + 112 * b + 128).shr(8) + 128
                v = (112 * r - 94 * g - 18 * b + 128).shr(8) + 128
                y = max(0, min(y, 255))
                u = max(0, min(u, 255))
                v = max(0, min(v, 255))

                yuv420SP[yIndex++] = y.toByte()
                if ((j % 2 == 0) and (i % 2 == 0)) {
                    yuv420SP[pixels++] = v.toByte()
                    yuv420SP[pixels++] = u.toByte()
                }
            }
        }

        val source = PlanarYUVLuminanceSource(yuv420SP, width, height, 0, 0, width, height, true)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

//        val hints = HashMap<DecodeHintType, Any>()
//        hints[DecodeHintType.CHARACTER_SET] = CHARSET
//        hints[DecodeHintType.POSSIBLE_FORMATS] = BarcodeFormat.QR_CODE
//        hints[DecodeHintType.TRY_HARDER] = true

        return QRCodeReader().decode(binaryBitmap, null).toString()
    }

}
