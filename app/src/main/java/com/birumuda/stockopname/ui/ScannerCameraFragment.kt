package com.birumuda.stockopname.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.birumuda.stockopname.BaseScannerActivity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.birumuda.stockopname.R

class ScannerCameraFragment : Fragment() {

	private lateinit var viewFinder: PreviewView
	private lateinit var btnScan: Button
	private lateinit var scannerLine: View
	private lateinit var cameraExecutor: ExecutorService
	private var barcodeScanner: BarcodeScanner? = null
	private var isScanning = false
	private var camera: Camera? = null
	private var blinkAnimator: ObjectAnimator? = null

	private val requestPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted: Boolean ->
		if (isGranted) {
			startCamera()
		} else {
			Toast.makeText(context, "Izin kamera dibutuhkan untuk scan barcode", Toast.LENGTH_SHORT).show()
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		return inflater.inflate(R.layout.fragment_scanner_camera, container, false)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		viewFinder = view.findViewById(R.id.viewFinder)
		btnScan = view.findViewById(R.id.btnScan)
		scannerLine = view.findViewById(R.id.scannerLine)
		cameraExecutor = Executors.newSingleThreadExecutor()

		val options = BarcodeScannerOptions.Builder()
			.setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
			.build()
		barcodeScanner = BarcodeScanning.getClient(options)

		if ((activity as? BaseScannerActivity)?.isUseCamera == true) {
			if (allPermissionsGranted()) {
				startCamera()
			} else {
				requestPermissionLauncher.launch(Manifest.permission.CAMERA)
			}
		}

		btnScan.setOnTouchListener { _, event ->
			if (!btnScan.isEnabled) return@setOnTouchListener false

			when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					startScanning()
					true
				}
				MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
					stopScanning()
					true
				}
				else -> false
			}
		}
	}

	private fun startScanning() {
		isScanning = true
		btnScan.text = getString(R.string.btn_scan_active)
		btnScan.setTextColor(ContextCompat.getColor(requireContext(), R.color.scan_button_active_text))
		btnScan.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.scan_button_active))
		scannerLine.visibility = View.VISIBLE
		startBlinkAnimation()
	}

	private fun stopScanning() {
		isScanning = false
		btnScan.text = getString(R.string.btn_scan_idle)
		btnScan.setTextColor(ContextCompat.getColor(requireContext(), R.color.scan_button_idle_text))
		btnScan.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.scan_button_idle))
		scannerLine.visibility = View.GONE
		stopBlinkAnimation()
	}

	private fun startBlinkAnimation() {
		if (blinkAnimator == null) {
			blinkAnimator = ObjectAnimator.ofFloat(
				scannerLine,
				"alpha",
				1f,
				0.2f
			).apply {
				duration = 500
				repeatCount = ValueAnimator.INFINITE
				repeatMode = ValueAnimator.REVERSE
				interpolator = LinearInterpolator()
			}
		}
		blinkAnimator?.start()
	}

	private fun stopBlinkAnimation() {
		blinkAnimator?.cancel()
		scannerLine.alpha = 1f
	}

	fun startCamera() {
		val context = context ?: return
		val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

		cameraProviderFuture.addListener({
			val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

			val preview = Preview.Builder()
				.build()
				.also {
					it.setSurfaceProvider(viewFinder.surfaceProvider)
				}

			val imageAnalyzer = ImageAnalysis.Builder()
				.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
				.build()
				.also {
					it.setAnalyzer(cameraExecutor) { imageProxy ->
						processImageProxy(imageProxy)
					}
				}

			val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

			try {
				cameraProvider.unbindAll()
				camera = cameraProvider.bindToLifecycle(
					viewLifecycleOwner, cameraSelector, preview, imageAnalyzer
				)
			} catch (exc: Exception) {
				Log.e("PrintlabelCamera", "Use case binding failed", exc)
			}

		}, ContextCompat.getMainExecutor(context))
	}

	fun stopCamera() {
		val context = context ?: return
		val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
		cameraProviderFuture.addListener({
			val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
			cameraProvider.unbindAll()
		}, ContextCompat.getMainExecutor(context))
	}

	@SuppressLint("UnsafeOptInUsageError")
	private fun processImageProxy(imageProxy: ImageProxy) {
		if (!isScanning) {
			imageProxy.close()
			return
		}

		val mediaImage = imageProxy.image
		if (mediaImage != null) {
			val rotationDegrees = imageProxy.imageInfo.rotationDegrees
			val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

			barcodeScanner?.process(image)
				?.addOnSuccessListener { barcodes ->
					for (barcode in barcodes) {
						val rawValue = barcode.rawValue
						val boundingBox = barcode.boundingBox

						if (rawValue != null && boundingBox != null && isScanning) {
							// Map barcode coordinates to PreviewView coordinates
							val mappedRect = calculateRectOnView(boundingBox, imageProxy)

							if (isInsideViewfinder(mappedRect)) {
								activity?.runOnUiThread {
									if (isScanning) {
										stopScanning()
										(activity as? BaseScannerActivity)?.findBarcode(rawValue)
									}
								}
								break
							}
						}
					}
				}
				?.addOnFailureListener {
					Log.e("PrintlabelCamera", "Barcode scanning failed", it)
				}
				?.addOnCompleteListener {
					imageProxy.close()
				}
		} else {
			imageProxy.close()
		}
	}

	/**
	 * Menghitung posisi pusat barcode di koordinat PreviewView secara manual.
	 * Koordinat ML Kit sudah relatif terhadap gambar yang diputar (Portrait).
	 */
	private fun calculateRectOnView(boundingBox: Rect, imageProxy: ImageProxy): RectF {
		val rotation = imageProxy.imageInfo.rotationDegrees
		val imgW = imageProxy.width.toFloat()
		val imgH = imageProxy.height.toFloat()

		// Dimensi gambar setelah rotasi (posisi portrait yang dilihat ML Kit)
		val finalImgW = if (rotation % 180 != 0) imgH else imgW
		val finalImgH = if (rotation % 180 != 0) imgW else imgH

		val viewW = viewFinder.width.toFloat()
		val viewH = viewFinder.height.toFloat()

		// Normalisasi koordinat pusat barcode (0.0 - 1.0) pada gambar portrait
		val nx = boundingBox.centerX() / finalImgW
		val ny = boundingBox.centerY() / finalImgH

		// Hitung skala dan offset berdasarkan ScaleType FILL_CENTER pada PreviewView
		val scale = Math.max(viewW / finalImgW, viewH / finalImgH)
		val offsetX = (viewW - finalImgW * scale) / 2f
		val offsetY = (viewH - finalImgH * scale) / 2f

		// Petakan ke koordinat view layar
		val vx = nx * finalImgW * scale + offsetX
		val vy = ny * finalImgH * scale + offsetY

		return RectF(vx - 2, vy - 2, vx + 2, vy + 2)
	}

	private fun isInsideViewfinder(barcodeRect: RectF): Boolean {
		val density = resources.displayMetrics.density
		// Margin sesuai dengan scanner_overlay.xml
		// L/R: 40dp, T/B: 65dp
		val marginHorizontal = 40f * density
		val marginVertical = 65f * density

		val viewfinderRect = RectF(
			marginHorizontal,
			marginVertical,
			viewFinder.width - marginHorizontal,
			viewFinder.height - marginVertical
		)

		// Cek apakah pusat barcode berada di dalam kotak putih secara visual
		return viewfinderRect.contains(barcodeRect.centerX(), barcodeRect.centerY())
	}

	private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
		requireContext(), Manifest.permission.CAMERA
	) == PackageManager.PERMISSION_GRANTED

	override fun onDestroyView() {
		super.onDestroyView()
		stopBlinkAnimation()
		cameraExecutor.shutdown()
	}

	fun setHold(hold: Boolean) {
		activity?.runOnUiThread {
			if (!isAdded) return@runOnUiThread
			btnScan.isEnabled = !hold
			if (hold) {
				stopScanning()
				btnScan.text = getString(R.string.msg_wait)
				btnScan.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.grey))
			} else {
				btnScan.text = getString(R.string.btn_scan_idle)
				btnScan.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.dark_red))
			}
		}
	}

}
