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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.imageclassification.ImageClassifierHelper
import org.tensorflow.lite.examples.imageclassification.databinding.ItemClassificationResultBinding

class ClassificationResultsAdapter : RecyclerView.Adapter<ClassificationResultsAdapter.ViewHolder>() {
    private var categories: List<ImageClassifierHelper.Recognition> = emptyList()

    fun updateResults(newCategories: List<ImageClassifierHelper.Recognition>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClassificationResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category.label, category.confidence)
    }

    override fun getItemCount() = categories.size

    class ViewHolder(private val binding: ItemClassificationResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(label: String, confidence: Float) {
            binding.tvLabel.text = label
            // 확률을 퍼센트(%) 단위로 소수점 한 자리까지 표시
            binding.tvScore.text = String.format("%.1f%%", confidence * 100)
        }
    }
}
