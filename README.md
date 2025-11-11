This repository contains quick start examples for 
[Banuba Face AR SDK on Android](https://docs.banuba.com/face-ar-sdk-v1/android/android_overview) 
and [Amazon IVS SDK integration](https://aws.amazon.com/ivs/). 
In them, Interactive Video Service provides the core streaming functionality, while Banuba Face AR SDK enhances it with virtual backgrounds, face filters, and other effects.

![image](image.png)

The Amazon part is based on CustomSourceActivity from https://github.com/aws-samples/amazon-ivs-broadcast-android-sample. Read more about integrating it in our blog post: https://www.banuba.com/blog/how-to-build-a-live-streaming-app-with-amazon-ivs-and-banuba-sdk

Banuba Face AR SDK doesn’t collect, store, or process any user data, making it secure by design and compliant with GDPR and other data protection regulations.


# Getting Started

1. Get the latest Banuba SDK client token. Please fill in our form on [form on banuba.com](https://www.banuba.com/face-filters-sdk) website, or contact us via [info@banuba.com](mailto:info@banuba.com).
2. Open the project in Android Studio
3. Copy and Paste your banuba client token into BANUBA_CLIENT_TOKEN of `TokensAndConfig.kt`
4. Visit Amazon Console to sign up and get key and endpoint 
5. Copy and Paste your Amazon IVS key/endpoint into AMAZON_IV_KEY/AMAZON_IV_ENDPOINT of TokensAndConfig.kt
6. In Android Studio run the necessary target.


# Contributing

Contributions are what make the open source community such an amazing place to be learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

