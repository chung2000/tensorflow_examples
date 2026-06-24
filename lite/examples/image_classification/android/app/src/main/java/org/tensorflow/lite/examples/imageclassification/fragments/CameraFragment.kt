/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.imageclassification.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.tensorflow.lite.examples.imageclassification.ImageClassifierHelper
import org.tensorflow.lite.examples.imageclassification.R
import org.tensorflow.lite.examples.imageclassification.databinding.FragmentCameraBinding
import java.io.File
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment(), ImageClassifierHelper.ClassifierListener {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService

    // [변경 1] MediaActionSound 제거하고 SoundPool 사용
    private lateinit var soundPool: SoundPool
    private var shutterSoundId: Int = 0

    // [설정] 셔터 소리 크기 (0.0f ~ 1.0f 사이로 조절하세요)
    // 현재 30% 크기로 설정됨. 더 작게 하려면 0.1f로 수정.
    private val shutterVolume = 0.3f

    private val classificationAdapter by lazy { ClassificationResultsAdapter() }
    private var currentPhotoUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) processSelectedImage(uri)
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) currentPhotoUri?.let { processSelectedImage(it) }
        else Toast.makeText(requireContext(), "촬영 취소됨", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // [변경 2] SoundPool 초기화 (소리 로딩)
        initSoundPool()

        binding.recyclerviewResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = classificationAdapter
        }

        imageClassifierHelper = ImageClassifierHelper(context = requireContext(), imageClassifierListener = this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.viewFinder.post {
            setUpCamera()
            setupTouchToFocus()
        }

        setupCaptureButton()
        setupGalleryButton()
        setupSystemCameraButton()
        setupClosePopupButton()
    }

    // [변경 3] SoundPool 설정 함수
    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA) // 미디어 볼륨 사용 (알림 볼륨 아님)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        // res/raw/shutter.mp3 파일을 로드합니다.
        // 파일명이 다르다면 R.raw.shutter 부분을 R.raw.파일명 으로 바꾸세요.
        // 파일이 없으면 에러가 나므로 꼭 1단계를 먼저 진행해주세요.
        try {
            shutterSoundId = soundPool.load(requireContext(), R.raw.shutter, 1)
        } catch (e: Exception) {
            Log.e("SoundPool", "Sound file not found in res/raw", e)
        }
    }

    private fun setupCaptureButton() {
        binding.captureButton.setOnClickListener {
            // [변경 4] SoundPool로 소리 재생 (볼륨 조절 적용)
            // play(soundID, leftVolume, rightVolume, priority, loop, rate)
            if (shutterSoundId != 0) {
                soundPool.play(shutterSoundId, shutterVolume, shutterVolume, 1, 0, 1.0f)
            }

            val imageCapture = imageCapture ?: return@setOnClickListener
            imageCapture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    val rotation = image.imageInfo.rotationDegrees
                    imageClassifierHelper.classify(bitmap, rotation)
                    image.close()
                }
                override fun onError(exception: ImageCaptureException) { Log.e("CameraFragment", "Error", exception) }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
        // [변경 5] 메모리 해제
        soundPool.release()
    }

    // --- 이하 기존 코드와 100% 동일 (변경 없음) ---

    private fun setupSystemCameraButton() {
        binding.systemCameraButton.setOnClickListener {
            val photoFile = File.createTempFile("IMG_", ".jpg", requireContext().cacheDir)
            val authority = "${requireContext().packageName}.provider"
            currentPhotoUri = FileProvider.getUriForFile(requireContext(), authority, photoFile)
            takePictureLauncher.launch(currentPhotoUri)
        }
    }

    private fun setupGalleryButton() {
        binding.galleryButton.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun setupClosePopupButton() {
        binding.closePopupButton.setOnClickListener {
            binding.resultPopup.visibility = View.GONE
            binding.dimmingBackground.visibility = View.GONE
            binding.buttonContainer.visibility = View.VISIBLE
            binding.tvGuide.visibility = View.VISIBLE
            classificationAdapter.updateResults(emptyList())
        }
    }

    override fun onResults(results: List<ImageClassifierHelper.Recognition>?, inferenceTime: Long) {
        activity?.runOnUiThread {
            if (_binding == null) return@runOnUiThread
            if (results.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "피부를 인식할 수 없습니다.\n다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                return@runOnUiThread
            }
            classificationAdapter.updateResults(results)
            binding.dimmingBackground.visibility = View.VISIBLE
            binding.resultPopup.visibility = View.VISIBLE
            binding.buttonContainer.visibility = View.INVISIBLE
            binding.tvGuide.visibility = View.INVISIBLE
        }
    }

    private fun processSelectedImage(uri: Uri) {
        try {
            Toast.makeText(requireContext(), "사진 분석 중...", Toast.LENGTH_SHORT).show()
            val bitmap = loadBitmapFromUri(uri)
            if (bitmap != null) {
                imageClassifierHelper.classify(bitmap, 0)
            } else {
                Toast.makeText(requireContext(), "사진을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("CameraFragment", "Error processing image", e)
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val contentResolver = requireContext().contentResolver
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val exif = inputStream?.use { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotationDegrees != 0f) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees)
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else { bitmap }
        } catch (e: Exception) { null }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
        imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
        } catch (exc: Exception) { Log.e("CameraFragment", "Binding failed", exc) }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchToFocus() {
        binding.viewFinder.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val factory = binding.viewFinder.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point).build()
                camera?.cameraControl?.startFocusAndMetering(action)
                return@setOnTouchListener true
            }
            true
        }
    }

    override fun onError(error: String) {
        activity?.runOnUiThread { Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show() }
    }
}
