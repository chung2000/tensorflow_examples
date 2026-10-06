# TensorFlow Examples

<div align="center">
  <img src="https://www.tensorflow.org/images/tf_logo_social.png" /><br /><br />
</div>

<h2>Most important links!</h2>

* [Community examples](./community)
* [Course materials](./courses/udacity_deep_learning) for the [Deep Learning](https://www.udacity.com/course/deep-learning--ud730) class on Udacity

If you are looking to learn TensorFlow, don't miss the
[core TensorFlow documentation](http://github.com/tensorflow/docs)
which is largely runnable code.
Those notebooks can be opened in Colab from
[tensorflow.org](https://tensorflow.org).

<h2>What is this repo?</h2>

This is the TensorFlow example repo.  It has several classes of material:

* Showcase examples and documentation for our fantastic [TensorFlow Community](https://tensorflow.org/community)
* Provide examples mentioned on TensorFlow.org
* Publish material supporting official TensorFlow courses
* Publish supporting material for the [TensorFlow Blog](https://blog.tensorflow.org) and [TensorFlow YouTube Channel](https://youtube.com/tensorflow)

We welcome community contributions, see [CONTRIBUTING.md](CONTRIBUTING.md) and, for style help,
[Writing TensorFlow documentation](https://www.tensorflow.org/community/contribute/docs_style)
guide.

To file an issue, use the tracker in the
[tensorflow/tensorflow](https://github.com/tensorflow/tensorflow/issues/new?template=20-documentation-issue.md) repo.

## License

[Apache License 2.0](LICENSE)

## Skin Classification (Android)

An Android app that classifies skin conditions **on-device** with TensorFlow Lite.
It analyzes a skin photo taken with the camera using `model_float16.tflite` and shows the probability for each of 6 classes.

- **Classes:** acne, atopic dermatitis, normal, psoriasis, rosacea, seborrheic dermatitis
- **Built on:** Google's TensorFlow Lite `image_classification` Android sample (Kotlin), adapted for skin classification
- **Source:** [`skin_classification` branch](https://github.com/chung2000/tensorflow_examples/tree/skin_classification/lite/examples/image_classification/android)

### What I built on top of the sample

- Redesigned the camera screen UI.
- Built the buttons and the result screen that shows each class's probability.
- Connected the button action to the model: pressing a button runs inference on the captured image and shows the result.

<p align="center">
  <img src="screenshots/skin_classification_result.jpg" width="300" alt="Result screen" /><br />
  <sub>Result screen</sub>
</p>

> ⚠️ For reference only. This app does not replace a medical diagnosis.
