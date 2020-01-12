#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_coffee_nils_dev_receipts_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
