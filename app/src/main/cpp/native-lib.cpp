#include <jni.h>
#include <opencv2/core/core.hpp>
#include <vector>
#include "opencv2/imgproc/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"

#include "ReceiptCropper.h"


using namespace cv;
using namespace std;


extern "C"
JNIEXPORT jlong JNICALL
Java_coffee_nils_dev_receipts_util_ImageUtil_autoCrop__J(JNIEnv *env, jclass thiz, jlong addr)
{
    Mat* image = (Mat*)addr;
    ReceiptCropper* cropper = new ReceiptCropper(image);
    image = cropper->getCropped();
    delete cropper;

    return (jlong)image;
}