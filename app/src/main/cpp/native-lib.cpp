#include <jni.h>
#include <opencv2/core/core.hpp>
#include "opencv2/imgproc/imgproc.hpp"

using namespace cv;

bool compareContourAreas(std::vector<cv::Point> contour1, std::vector<cv::Point> contour2)
{
    double i = fabs(contourArea(cv::Mat(contour1)));
    double j = fabs(contourArea(cv::Mat(contour2)));
    return (i > j);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_coffee_nils_dev_receipts_util_ImageUtil_autoCrop__J(JNIEnv *env, jclass thiz, jlong addr)
{
    Mat* image = (Mat*) addr;

    Rect bounding_rect;

    Mat thresh(image->rows, image->cols, CV_8UC1);
    cvtColor(*image, thresh, COLOR_RGB2GRAY); // convert gray
    threshold(thresh, thresh, 150, 255, THRESH_BINARY + THRESH_OTSU); //Threshold the gray

    // Vector for storing contour
    std::vector<std::vector<Point>> contours;
    std::vector<Vec4i> hierarchy;
    findContours(thresh, contours, hierarchy, RETR_CCOMP, CHAIN_APPROX_SIMPLE);
    sort(contours.begin(), contours.end(), compareContourAreas); //Store the index of largest contour

    bounding_rect = boundingRect((const _InputArray &)contours[0]);
    rectangle(*image, bounding_rect, Scalar(250, 250, 250), 3);
    Mat* croppedImage = new Mat(*image, bounding_rect);//image(bounding_rect);

    // if landscape
    if(croppedImage->cols > croppedImage->rows)
    {
        rotate(*croppedImage, *croppedImage, ROTATE_90_CLOCKWISE);
    }

    return (jlong)croppedImage;
}