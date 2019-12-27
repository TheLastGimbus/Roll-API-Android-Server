/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/


// Ye, pretty much copied it from TF Lite examples because it's good

package com.example.dicerollserver.tflite

import android.app.Activity

import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.ops.NormalizeOp

import java.io.IOException

/** This TensorFlow Lite classifier works with the quantized MobileNet model.  */
class ClassifierQuantizedMobileNet
/**
 * Initializes a `ClassifierQuantizedMobileNet`.
 *
 * @param activity
 */
@Throws(IOException::class)
constructor(activity: Activity, device: Classifier.Device, numThreads: Int) :
    Classifier(activity, device, numThreads) {

    override// you can download this file from
    // see build.gradle for where to obtain this file. It should be auto
    // downloaded into assets.
    val modelPath: String
        get() = "mobilenet_v1_1.0_224_quant.tflite"

    override val labelPath: String
        get() = "labels.txt"

    override val preprocessNormalizeOp: TensorOperator
        get() = NormalizeOp(IMAGE_MEAN, IMAGE_STD)

    override val postprocessNormalizeOp: TensorOperator
        get() = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)

    companion object {

        /**
         * The quantized model does not require normalization, thus set mean as 0.0f, and std as 1.0f to
         * bypass the normalization.
         */
        private val IMAGE_MEAN = 0.0f

        private val IMAGE_STD = 1.0f

        /** Quantized MobileNet requires additional dequantization to the output probability.  */
        private val PROBABILITY_MEAN = 0.0f

        private val PROBABILITY_STD = 255.0f
    }
}
