/*
 * ReceiptCropper.h
 *
 *  Created on: May 22, 2020
 *      Author: nils
 */

#ifndef RECEIPTCROPPER_H_
#define RECEIPTCROPPER_H_

#include <opencv2/core/core.hpp>

using namespace std;
using namespace cv;
// if DEBUG, you may print images for each step of the way.
const string DEBUG_OUT_DIR = "/home/nils/eclipse-cpp-ws/receipt-cropper/out/";
const bool DEBUG = false;


class ReceiptCropper {
private:
    // The original receipt
    Mat* receipt;
    // the cropped receipt
    Mat* cropped;
    const int THRESH_VALUE = 150;
    void swap(int &a, int &b);
    static bool compareContourAreas(vector<Point> contour_1, vector<Point> contour_2);
    void crop(Mat* mat);
    void setPortrait();
public:
    ReceiptCropper(Mat* receipt);
    Mat* getCropped();

    virtual ~ReceiptCropper();
};

#endif /* RECEIPTCROPPER_H_ */
