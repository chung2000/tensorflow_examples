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

package org.tensorflow.lite.examples.imageclassification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import java.nio.MappedByteBuffer
import kotlin.math.exp

class ImageClassifierHelper(
    var threshold: Float = 0.5f, // 사진 촬영이므로 기준을 조금 낮춰도 됩니다 (필터가 이미 강력함)
    var numThreads: Int = 2,
    val context: Context,
    val imageClassifierListener: ClassifierListener?
) {
    private var interpreter: Interpreter? = null
    private val labels = listOf("여드름", "아토피 피부염", "정상", "건선", "주사(로사시아)", "지루성 피부염")

    init { setupImageClassifier() }

    private fun setupImageClassifier() {
        try {
            val modelBuffer: MappedByteBuffer = FileUtil.loadMappedFile(context, "model_float16.tflite")
            val options = Interpreter.Options().setNumThreads(numThreads)
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            imageClassifierListener?.onError("모델 로드 실패: ${e.message}")
        }
    }

    // 사진 한 장을 분석하는 함수 (Rotation 인자는 TensorImage 처리 방식에 따라 사용될 수 있음)
    fun classify(bitmap: Bitmap, rotation: Int) {
        if (interpreter == null) return

        var inferenceTime = SystemClock.uptimeMillis()

        // [장치 1] 피부색 필터링
        // 찍은 사진이 피부가 아니라면(배경 등) 분석 거부
        if (!isLikelySkinColor(bitmap)) {
            imageClassifierListener?.onResults(emptyList(), 0)
            return
        }

        // 전처리: 모델 입력 크기(224x224)로 변환
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)

        val output = Array(1) { FloatArray(labels.size) }
        interpreter?.run(tensorImage.buffer, output)

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        val rawLogits = output[0]
        val maxLogit = rawLogits.maxOrNull() ?: 0f

        Log.d("Classifier", "Capture Mode Max Logit: $maxLogit")

        // [장치 2] 수치 필터링 (배경 오진 차단)
        // 로그에서 확인했던 수치(약 400~500)를 기준으로 설정
        if (maxLogit < 500.0f) {
            imageClassifierListener?.onResults(emptyList(), inferenceTime)
            return
        }

        val probabilities = softmax(rawLogits)

        // 결과 리스트 생성 (버퍼 없이 즉시 반환)
        val results = probabilities.mapIndexed { index, score ->
            Recognition(labels[index], score)
        }.sortedByDescending { it.confidence }

        imageClassifierListener?.onResults(results, inferenceTime)
    }

    private fun isLikelySkinColor(bitmap: Bitmap): Boolean {
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        val pixel = bitmap.getPixel(centerX, centerY)
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)

        // RGB 피부색 간이 조건
        return (r > g) && (r > b) && (r > 60)
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val expValues = logits.map { exp((it - max).toDouble()).toFloat() }
        val sum = expValues.sum()
        return expValues.map { it / sum }.toFloatArray()
    }

    fun clearImageClassifier() {
        interpreter?.close()
        interpreter = null
    }

    data class Recognition(val label: String, val confidence: Float)
    interface ClassifierListener {
        fun onResults(results: List<Recognition>?, inferenceTime: Long)
        fun onError(error: String)
    }
}
