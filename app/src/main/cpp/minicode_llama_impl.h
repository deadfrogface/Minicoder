#ifndef MINICODE_LLAMA_IMPL_H
#define MINICODE_LLAMA_IMPL_H

#ifdef __cplusplus
extern "C" {
#endif

int minicode_llama_load(const char* path);
void minicode_llama_unload(void);
int minicode_llama_generate(const char* prompt, int max_tokens, float temperature,
    float top_p, float repeat_penalty, int seed, char* out_buf, int out_size, int timeout_sec);

#ifdef __cplusplus
}
#endif
