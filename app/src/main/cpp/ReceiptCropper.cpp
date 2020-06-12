/*
 * ReceiptCropper.cpp
 *
 *  Created on: May 22, 2020
 *      Author: nils
 */

#include "ReceiptCropper.h"

#include <iostream>
#include <vector>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>

using namespace cv;
using namespace std;


ReceiptCropper::ReceiptCropper(Mat* mat)
{
    receipt = mat;
    setPortrait();
    crop(receipt, hasBrightEdges(receipt));

}

void ReceiptCropper::swap(int &a, int &b) {
    int temp = a;
    a = b;
    b = temp;
}

bool ReceiptCropper::compareContourAreas(vector<Point> contour_1, vector<Point> contour_2) {
    double i = fabs(contourArea(Mat(contour_1)));
    double j = fabs(contourArea(Mat(contour_2)));
    return (i > j);
}

bool ReceiptCropper::hasBrightEdges(Mat *mat) {
    Rect rect(0, 0, 10, mat->rows);
    Mat left(*mat, rect);
    Rect rect2(mat->cols - 10, 0, 10, mat->rows);
    Mat right(*mat, rect2);

    if (DEBUG) {
        imwrite(DEBUG_OUT_DIR + "left.jpg", left);
    }
    if (DEBUG) {
        imwrite(DEBUG_OUT_DIR + "right.jpg", right);
    }

    Mat edges[] = { left, right };

    Mat left_right_joined;
    hconcat(edges, 2, left_right_joined);
    if (DEBUG) {
        imwrite(DEBUG_OUT_DIR + "lr_join.jpg", left_right_joined);
    }

    Mat hsv;
    cvtColor(left_right_joined, hsv, COLOR_BGR2HSV);

    const auto result = mean(hsv);

    left.release();
    right.release();

    if (result[2] > BRIGHT) {
        hsv.release();
        return true;
    }
    hsv.release();
    return false;
}

void ReceiptCropper::crop(Mat *image, bool brightEdges) {
    int threshVal = brightEdges ? THRESH_BRIGHT : THRESH_NORMAL;

    if (DEBUG) {
        imwrite(DEBUG_OUT_DIR + "original.jpg", *image);
    }
    // "threshold" image to find receipt corners and do rotation.
    Mat *thresh = new Mat(image->rows, image->cols, CV_8UC1);

    cvtColor(*image, *thresh, COLOR_BGR2GRAY);
    if (DEBUG) {
        imwrite(DEBUG_OUT_DIR + "gray.jpg", *thresh);
    }
    //Threshold the gray
    threshold(*thresh, *thresh, threshVal, 255, THRESH_BINARY);
    if (DEBUG) {
        imwrite(DEBUG_OUT_DIR + "threshold.jpg", *thresh);
    }

    // Find the largest contour
    vector<vector<Point> > contours;
    vector<Vec4i> hierarchy;
    RotatedRect rotatedRect;
    findContours(*thresh, contours, hierarchy, RETR_CCOMP, CHAIN_APPROX_SIMPLE);
    sort(contours.begin(), contours.end(), compareContourAreas);
    Rect bounding_rect = boundingRect(contours[0]);
    rotatedRect = minAreaRect(contours[0]);

    // get angle and size from the bounding box
    float angle = rotatedRect.angle;
    Size rect_size = rotatedRect.size;

    if (angle < -45.) {
        angle += 90.0;
        swap(rect_size.width, rect_size.height);
    }

    // get the transition matrix to apply the rotation
    Mat transationMat = getRotationMatrix2D(rotatedRect.center, angle, 1);
    if (DEBUG) {
        imwrite(DEBUG_OUT_DIR + "transition_matrix.jpg", *thresh);
    }

    warpAffine(*image, *image, transationMat, image->size(), INTER_CUBIC);
    if (DEBUG) {
        imwrite(DEBUG_OUT_DIR + "affine.jpg", *image);
    }

    // Convert to gray and threshold
    cvtColor(*image, *thresh, COLOR_BGR2GRAY);
    threshold(*thresh, *thresh, threshVal, 255, THRESH_BINARY);

    findContours(*thresh, contours, hierarchy, RETR_CCOMP, CHAIN_APPROX_SIMPLE);
    sort(contours.begin(), contours.end(), compareContourAreas);
    bounding_rect = boundingRect(contours[0]);
    *image = Mat(*image, bounding_rect);
    if (DEBUG) {
        imwrite(DEBUG_OUT_DIR + "result.jpg", *image);
    }

    this->cropped = image;

    transationMat.release();
    thresh->release();

}

void ReceiptCropper::setPortrait() {
    if (receipt->cols > receipt->rows) {
        rotate(*receipt, *receipt, ROTATE_90_CLOCKWISE);
    }
}

Mat* ReceiptCropper::getCropped() {
    return this->cropped;
}

ReceiptCropper::~ReceiptCropper() {
    // cleanup happens in crop
}

