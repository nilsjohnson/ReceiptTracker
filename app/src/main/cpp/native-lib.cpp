#include <jni.h>
#include <string>

#include <opencv2/core/core.hpp>
#include <opencv2/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include "opencv2/imgproc/imgproc.hpp"
//#include <iostream>//
//#include <string>

using namespace cv;
using namespace std;


extern "C" JNIEXPORT jstring JNICALL
Java_coffee_nils_dev_receipts_MainActivity_stringFromJNI( JNIEnv *env, jobject) /* this */
{
    string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


bool compareContourAreas(std::vector<cv::Point> contour1, std::vector<cv::Point> contour2)
{
    double i = fabs(contourArea(cv::Mat(contour1)));
    double j = fabs(contourArea(cv::Mat(contour2)));
    return (i > j);
}


extern "C"
JNIEXPORT jlong JNICALL
Java_coffee_nils_dev_receipts_ReceiptActivity_autoCrop(JNIEnv *env, jobject thiz, jlong addr) {
    // open the image
    // Mat image = imread(path, IMREAD_COLOR);
    //Mat image = mat;

    //long myLong = (long)addr;
    Mat* img_ptr = (Mat*)addr;
    Mat image = *img_ptr;

    Rect bounding_rect;

    Mat thresh(image.rows, image.cols, CV_8UC1);
    //Convert to gray
    cvtColor(image, thresh, COLOR_BGR2GRAY);
    threshold(thresh, thresh, 150, 255, THRESH_BINARY + THRESH_OTSU); //Threshold the gray

    // Vector for storing contour
    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;
    findContours(thresh, contours, hierarchy, RETR_CCOMP, CHAIN_APPROX_SIMPLE);
    sort(contours.begin(), contours.end(), compareContourAreas); //Store the index of largest contour

    bounding_rect = boundingRect((const _InputArray &)contours[0]);
    rectangle(image, bounding_rect, Scalar(250, 250, 250), 3);
    Mat* croppedImage = new Mat(image(bounding_rect));//image(bounding_rect);

    // if landscape
    if(croppedImage->cols > croppedImage->rows)
    {
        rotate(*croppedImage, *croppedImage, ROTATE_90_CLOCKWISE);
    }

    return (jlong)croppedImage;
}