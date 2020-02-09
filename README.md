# Receipts

This app is for keeping track of shopping receipts. Users can take pictures of their receipts and the app will crop and parse basic information off it. Receipts are then stored for later retrieval.

In the root directory, I have included an APK if you simply wish to run the app.

## Getting Started

[This app uses the OpenCV c++ library.](https://sourceforge.net/projects/opencvlibrary/files/4.2.0/opencv-4.2.0-android-sdk.zip/download "OpenCV Android")

First, download the library. Then open src/main/cpp/CmakeLists.txt and edit the lines

```
set(pathToProject /home/nils/AndroidStudioProjects/Receipts)
set(pathToOpenCv /home/nils/Android/OpenCV-android-sdk)
```
to be

```
set(pathToProject 'path to where you cloned this project')
set(pathToOpenCv 'where you downloaded OpenCV-android-sdk')
```

Next you should be able to make and run the project.

