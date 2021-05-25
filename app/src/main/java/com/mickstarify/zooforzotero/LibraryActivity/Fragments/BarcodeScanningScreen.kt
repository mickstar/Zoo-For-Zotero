package com.mickstarify.zooforzotero.LibraryActivity.Fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.mickstarify.zooforzotero.LibraryActivity.ViewModels.LibraryListViewModel
import com.mickstarify.zooforzotero.R

class BarcodeScanningScreen : Fragment() {

    companion object {
        fun newInstance() = BarcodeScanningScreen()
        const val REQUEST_CAMERA = 1729
    }

    private lateinit var viewModel: LibraryListViewModel
    lateinit var cameraView: SurfaceView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.barcode_scanning_screen_fragment, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.barcode_scanning_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProvider(requireActivity()).get(LibraryListViewModel::class.java)

        cameraView = requireView().findViewById<SurfaceView>(R.id.cameraPreview)
        cameraView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {}

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                showCameraPreview(width, height)
            }
        })
    }

    override fun onStart() {
        super.onStart()

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        } else {
            // start camera feed
        }
    }

    // we will use this to determine the amount of frames in a row that we detect a barcode.
    // this is done for accuracy reasons as using one frame was allowing the phone to misdetect
    // barcodes.
    var detectedInARow: Int = 0
    var lastChange = 0L
    var lastReadBarcodeNo: String = ""

    var camera: CameraDevice? = null

    fun showCameraPreview(width: Int, height: Int) {
        val cameraView = requireView().findViewById<SurfaceView>(R.id.cameraPreview)

        try {
            val cameraBkgHandler = Handler()

            val cameraManager =
                requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager

            cameraManager.cameraIdList.find {
                val characteristics = cameraManager.getCameraCharacteristics(it)
                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)

                return@find cameraDirection != null && cameraDirection == CameraCharacteristics.LENS_FACING_BACK
            }?.let {
                val cameraStateCallback = object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        this@BarcodeScanningScreen.camera = camera
                        val barcodeDetector = BarcodeDetector.Builder(requireContext())
                            .setBarcodeFormats(Barcode.ALL_FORMATS)
                            .build()

                        if (barcodeDetector.isOperational == false) {
                            Toast.makeText(
                                requireContext(),
                                "Google mobile services (GMS) unable. Barcode scanning unsupported.",
                                Toast.LENGTH_SHORT
                            ).show()
                            stopScanning()
                        }

                        val imgReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
                        imgReader.setOnImageAvailableListener({ reader ->
                            val cameraImage = reader.acquireNextImage()
                            val buffer = cameraImage.planes.first().buffer
                            val bytes = ByteArray(buffer.capacity())
                            buffer.get(bytes)

                            val bitmap =
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.count(), null)
                            val frameToProcess = Frame.Builder().setBitmap(bitmap).build()
                            val barcodeResults = barcodeDetector.detect(frameToProcess)
                            if (barcodeResults.size() > 0) {
                                // Scanned a barcode!
                                val result = barcodeResults.valueAt(0)
                                Log.d("zotero", "scanned ${result.rawValue}")
                                if (result.rawValue == lastReadBarcodeNo) {
                                    detectedInARow++
                                } else {
                                    detectedInARow = 0
                                    lastReadBarcodeNo = result.rawValue
                                }

                                if (detectedInARow == 3) {
                                    val barcodeNo = barcodeResults.valueAt(0).rawValue
                                    viewModel.scannedBarcodeNumber(barcodeNo)
                                    stopScanning()
                                }
                            } else {
                            }
                            try {
                                cameraImage.close()
                            } catch (e: IllegalStateException) {
                                // camera already closed.
                            }

                        }, cameraBkgHandler)

                        val captureStateCallback = object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                val builder =
                                    camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                                builder.addTarget(cameraView.holder.surface)
                                builder.addTarget(imgReader.surface)
                                session.setRepeatingRequest(builder.build(), null, null)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                            }
                        }

                        camera.createCaptureSession(
                            listOf(cameraView.holder.surface, imgReader.surface),
                            captureStateCallback,
                            cameraBkgHandler
                        )
                    }

                    override fun onClosed(camera: CameraDevice) {
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                    }
                }

                cameraManager.openCamera(it, cameraStateCallback, cameraBkgHandler)
                return
            }

        } catch (e: CameraAccessException) {
        } catch (e: SecurityException) {
        }
    }

    private fun stopScanning() {
        try {
            this.camera?.close()
        } catch (e: IllegalStateException) {
            // camera already closed.
        }
        findNavController().navigateUp()
    }

    override fun onStop() {
        super.onStop()
        try {
            this.camera?.close()
        } catch (e: IllegalStateException) {
            // camera already closed.
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            && requestCode == REQUEST_CAMERA
        ) {
            showCameraPreview(cameraView.measuredWidth, cameraView.measuredHeight)
        } else {
            Toast.makeText(
                requireContext(),
                "This functionality requires the camera. Please provide the camera permission.",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigateUp()
        }
    }
}