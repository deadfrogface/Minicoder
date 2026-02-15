#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "MinicodeJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#ifdef LLAMA_AVAILABLE
#include "llama.h"
#include <vector>
#endif

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_minicode_LlmEngine_nativeLoadModel(JNIEnv *env, jobject thiz, jstring path) {
    (void) thiz;
#ifdef LLAMA_AVAILABLE
    if (!path) return JNI_FALSE;
    const char *pathStr = env->GetStringUTFChars(path, nullptr);
    if (!pathStr) return JNI_FALSE;
    llama_backend_init();
    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = -1;
    llama_model *model = llama_model_load_from_file(pathStr, mparams);
    env->ReleaseStringUTFChars(path, pathStr);
    if (!model) {
        LOGI("Failed to load model");
        return JNI_FALSE;
    }
    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = 2048;
    cparams.n_batch = 512;
    llama_context *ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        llama_model_free(model);
        return JNI_FALSE;
    }
    static llama_model *s_model = nullptr;
    static llama_context *s_ctx = nullptr;
    if (s_ctx) { llama_free(s_ctx); s_ctx = nullptr; }
    if (s_model) { llama_model_free(s_model); s_model = nullptr; }
    s_model = model;
    s_ctx = ctx;
    LOGI("Model loaded");
    return JNI_TRUE;
#else
    (void) env;
    (void) path;
    return JNI_FALSE;
#endif
}

JNIEXPORT jstring JNICALL
Java_com_minicode_LlmEngine_nativeGenerate(JNIEnv *env, jobject thiz,
    jstring prompt, jint max_tokens, jfloat temperature, jfloat top_p,
    jfloat repeat_penalty, jint seed) {
    (void) thiz;
    (void) temperature;
    (void) top_p;
    (void) repeat_penalty;
    (void) seed;
#ifdef LLAMA_AVAILABLE
    if (!prompt || !env) return env->NewStringUTF("");
    const char *promptStr = env->GetStringUTFChars(prompt, nullptr);
    if (!promptStr) return env->NewStringUTF("");
    (void) max_tokens;
    (void) promptStr;
    env->ReleaseStringUTFChars(prompt, promptStr);
    return env->NewStringUTF("");
#else
    (void) env;
    (void) prompt;
    (void) max_tokens;
    return env->NewStringUTF("");
#endif
}

}
