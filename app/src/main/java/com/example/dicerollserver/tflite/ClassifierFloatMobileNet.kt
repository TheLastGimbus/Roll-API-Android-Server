/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

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

/** This TensorFlowLite classifier works with the float MobileNet model.  */
class ClassifierFloatMobileNet
/**
 * Initializes a `ClassifierFloatMobileNet`.
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
        get() = "dice_classifier.tflite"

    override val labelPath: String
        get() = "dice_labels.txt"

    override val preprocessNormalizeOp: TensorOperator
        get() = NormalizeOp(IMAGE_MEAN, IMAGE_STD)

    override val postprocessNormalizeOp: TensorOperator
        get() = NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)

    companion object {

        /** Float MobileNet requires additional normalization of the used input.  */
        private val IMAGE_MEAN = 127.5f

        private val IMAGE_STD = 127.5f

        /**
         * Float model does not need dequantization in the post-processing. Setting mean and std as 0.0f
         * and 1.0f, repectively, to bypass the normalization.
         */
        private val PROBABILITY_MEAN = 0.0f

        private val PROBABILITY_STD = 1.0f
    }
}
