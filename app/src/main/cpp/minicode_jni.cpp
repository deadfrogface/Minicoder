#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "MinicodeJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_minicode_LlmEngine_nativeLoadModel(JNIEnv *env, jobject thiz, jstring path) {
    (void) env;
    (void) thiz;
    (void) path;
    return JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_minicode_LlmEngine_nativeGenerate(JNIEnv *env, jobject thiz,
    jstring prompt, jint max_tokens, jfloat temperature, jfloat top_p,
    jfloat repeat_penalty, jint seed) {
    (void) thiz;
    (void) prompt;
    (void) max_tokens;
    (void) temperature;
    (void) top_p;
    (void) repeat_penalty;
    (void) seed;
    return env->NewStringUTF("");
}

}
