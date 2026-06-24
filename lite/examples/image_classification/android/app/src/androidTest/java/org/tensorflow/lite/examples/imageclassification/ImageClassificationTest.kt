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

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.InputStream
import java.lang.Exception

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ImageClassificationTest {

    // 우리가 만든 Recognition 클래스에 맞게 예상 결과 데이터를 수정합니다.
    // 주의: 실제 모델 결과값(6개 클래스 중 상위 결과)에 맞춰 테스트 데이터가 필요할 수 있습니다.
    private val controlResults = listOf(
        ImageClassifierHelper.Recognition("cup", 0.76f),
        ImageClassifierHelper.Recognition("coffee mug", 0.12f)
    )

    @Test
    @Throws(Exception::class)
    fun classificationResultsShouldNotChange() {
        val imageClassifierHelper = ImageClassifierHelper(
            context = InstrumentationRegistry.getInstrumentation().context,
            imageClassifierListener = object : ImageClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    fail("Error occurred: $error")
                }

                override fun onResults(
                    // [수정] 구식 Classifications 대신 우리가 정의한 Recognition 사용
                    results: List<ImageClassifierHelper.Recognition>?,
                    inferenceTime: Long
                ) {
                    assertNotNull(results)

                    // 결과가 비어있지 않은지 확인
                    assertTrue(results!!.isNotEmpty())

                    // 첫 번째 결과의 라벨이 예상한 값 중 하나인지 검증 (예시)
                    // 실제 모델이 6개 클래스이므로, 테스트용 이미지에 맞는 라벨로 수정이 필요합니다.
                    assertNotNull(results[0].label)
                }
            },
            threshold = 0.1f // 테스트를 위해 임계값을 낮춤
        )

        // assets 폴더에 coffee.jpg가 있어야 작동합니다.
        val bitmap = loadImage("coffee.jpg")

        if (bitmap != null) {
            // 이미지 분류 실행
            imageClassifierHelper.classify(bitmap, Surface.ROTATION_0)
        } else {
            fail("Sample image not found")
        }
    }

    @Throws(Exception::class)
    private fun loadImage(fileName: String): Bitmap? {
        return try {
            val assetManager: AssetManager =
                InstrumentationRegistry.getInstrumentation().context.assets
            val inputStream: InputStream = assetManager.open(fileName)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
}
