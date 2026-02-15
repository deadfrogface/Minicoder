#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "MinicodeJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#ifdef LLAMA_AVAILABLE
#include "llama.h"
#include <vector>
#include <cmath>
#include <cstring>
#include <algorithm>
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
    (void) top_p;
    (void) repeat_penalty;
#ifdef LLAMA_AVAILABLE
    if (!prompt || !env) return env->NewStringUTF("");
    if (!s_ctx) return env->NewStringUTF("");
    const char *promptStr = env->GetStringUTFChars(prompt, nullptr);
    if (!promptStr) return env->NewStringUTF("");
    const int32_t text_len = (int32_t) strlen(promptStr);
    const llama_model *model = llama_get_model(s_ctx);
    const llama_vocab *vocab = llama_model_get_vocab(model);
    if (!vocab) {
        env->ReleaseStringUTFChars(prompt, promptStr);
        return env->NewStringUTF("");
    }
    std::vector<llama_token> prompt_tokens(std::min(2048, (int)llama_n_ctx(s_ctx)));
    int32_t n_prompt = llama_tokenize(vocab, promptStr, text_len,
        prompt_tokens.data(), (int32_t)prompt_tokens.size(), true, false);
    env->ReleaseStringUTFChars(prompt, promptStr);
    if (n_prompt <= 0) return env->NewStringUTF("");
    prompt_tokens.resize((size_t)n_prompt);
    llama_memory_clear(llama_get_memory(s_ctx), true);
    const uint32_t n_batch = llama_n_batch(s_ctx);
    for (int32_t i = 0; i < n_prompt; ) {
        int32_t chunk = (int32_t) std::min((uint32_t)(n_prompt - i), n_batch);
        struct llama_batch b = llama_batch_get_one(prompt_tokens.data() + i, chunk);
        if (llama_decode(s_ctx, b) != 0) {
            LOGI("Decode prompt failed");
            return env->NewStringUTF("");
        }
        i += chunk;
    }
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    llama_sampler *smpl = llama_sampler_chain_init(sparams);
    if (!smpl) return env->NewStringUTF("");
    llama_sampler_chain_add(smpl, llama_sampler_init_temp((double)(temperature <= 0.f ? 1e-6f : temperature)));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist((uint32_t)(seed & 0xFFFFFFFFu)));
    std::string result;
    result.reserve((size_t)max_tokens * 4);
    char piece[32];
    const llama_token eos = llama_vocab_eos(vocab);
    for (int n = 0; n < max_tokens; n++) {
        llama_token tok = llama_sampler_sample(smpl, s_ctx, -1);
        if (tok == eos || llama_vocab_is_eog(vocab, tok)) break;
        int len = llama_token_to_piece(vocab, tok, piece, sizeof(piece), 0, true);
        if (len > 0) result.append(piece, (size_t)len);
        struct llama_batch b = llama_batch_get_one(&tok, 1);
        if (llama_decode(s_ctx, b) != 0) break;
    }
    llama_sampler_free(smpl);
    return env->NewStringUTF(result.c_str());
#else
    (void) env;
    (void) prompt;
    (void) max_tokens;
    (void) temperature;
    (void) seed;
    return env->NewStringUTF("");
#endif
}

}
