#include <jni.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <pthread.h>
#include <unistd.h>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
}

#define LOG_TAG "FFmpegPlayer"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

ANativeWindow *nativeWindow = nullptr;
AVFormatContext *formatContext = nullptr;
AVCodecContext *codecContext = nullptr;
SwsContext *swsCtx = nullptr;
AVFrame *frame = nullptr;
AVFrame *rgbFrame = nullptr;
AVPacket *packet = nullptr;
uint8_t *buffer = nullptr;
int videoStreamIndex = -1;

volatile bool isPlaying = false;
volatile bool isPaused = false;

pthread_t decodeThread;
pthread_mutex_t stateMutex = PTHREAD_MUTEX_INITIALIZER;

char videoPath[1024] = {0};
jobject globalSurface = nullptr;
JavaVM *javaVm = nullptr;

void *decodeRoutine(void *) {
    JNIEnv *env;
    javaVm->AttachCurrentThread(&env, nullptr);

    ANativeWindow *window = ANativeWindow_fromSurface(env, globalSurface);
    nativeWindow = window;

    avformat_network_init();
    formatContext = avformat_alloc_context();

    if (avformat_open_input(&formatContext, videoPath, nullptr, nullptr) != 0) {
        LOGE("Failed to open file: %s", videoPath);
        return nullptr;
    }

    if (avformat_find_stream_info(formatContext, nullptr) < 0) {
        LOGE("Failed to get stream info");
        return nullptr;
    }

    videoStreamIndex = -1;
    for (int i = 0; i < formatContext->nb_streams; ++i) {
        if (formatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoStreamIndex = i;
            break;
        }
    }

    if (videoStreamIndex == -1) {
        LOGE("No video stream found");
        return nullptr;
    }

    AVCodecParameters *codecpar = formatContext->streams[videoStreamIndex]->codecpar;
    const AVCodec *codec = avcodec_find_decoder(codecpar->codec_id);
    codecContext = avcodec_alloc_context3(codec);
    avcodec_parameters_to_context(codecContext, codecpar);
    avcodec_open2(codecContext, codec, nullptr);

    ANativeWindow_setBuffersGeometry(nativeWindow, codecContext->width, codecContext->height,
                                     WINDOW_FORMAT_RGBA_8888);

    frame = av_frame_alloc();
    rgbFrame = av_frame_alloc();
    packet = av_packet_alloc();

    int bufferSize = av_image_get_buffer_size(AV_PIX_FMT_RGBA, codecContext->width,
                                              codecContext->height, 1);
    buffer = static_cast<uint8_t *>(av_malloc(bufferSize));
    av_image_fill_arrays(rgbFrame->data, rgbFrame->linesize, buffer, AV_PIX_FMT_RGBA,
                         codecContext->width, codecContext->height, 1);

    swsCtx = sws_getContext(
            codecContext->width,
            codecContext->height,
            codecContext->pix_fmt,
            codecContext->width,
            codecContext->height,
            AV_PIX_FMT_RGBA,
            SWS_BILINEAR,
            nullptr, nullptr, nullptr);

    while (isPlaying && av_read_frame(formatContext, packet) >= 0) {
        pthread_mutex_lock(&stateMutex);
        bool paused = isPaused;
        pthread_mutex_unlock(&stateMutex);

        if (paused) {
            usleep(100 * 1000);
            continue;
        }

        if (packet->stream_index == videoStreamIndex) {
            avcodec_send_packet(codecContext, packet);
            if (avcodec_receive_frame(codecContext, frame) == 0) {
                sws_scale(swsCtx, frame->data, frame->linesize, 0,
                          codecContext->height, rgbFrame->data, rgbFrame->linesize);

                ANativeWindow_Buffer windowBuffer;
                if (ANativeWindow_lock(nativeWindow, &windowBuffer, nullptr) == 0) {
                    uint8_t *dst = static_cast<uint8_t *>(windowBuffer.bits);
                    uint8_t *src = rgbFrame->data[0];
                    int srcStride = rgbFrame->linesize[0];

                    for (int h = 0; h < codecContext->height; h++) {
                        memcpy(dst + h * windowBuffer.stride * 4,
                               src + h * srcStride,
                               codecContext->width * 4);
                    }

                    ANativeWindow_unlockAndPost(nativeWindow);
                    usleep(1000 * 16);
                }
            }
        }
        av_packet_unref(packet);
    }

    // Cleanup
    pthread_mutex_lock(&stateMutex);
    isPlaying = false;
    pthread_mutex_unlock(&stateMutex);

    if (nativeWindow) ANativeWindow_release(nativeWindow);
    if (swsCtx) sws_freeContext(swsCtx);
    if (packet) av_packet_free(&packet);
    if (frame) av_frame_free(&frame);
    if (rgbFrame) av_frame_free(&rgbFrame);
    if (buffer) av_free(buffer);
    if (codecContext) avcodec_free_context(&codecContext);
    if (formatContext) avformat_close_input(&formatContext);

    nativeWindow = nullptr;
    swsCtx = nullptr;
    frame = nullptr;
    rgbFrame = nullptr;
    buffer = nullptr;
    packet = nullptr;
    codecContext = nullptr;
    formatContext = nullptr;

    env->DeleteGlobalRef(globalSurface);
    globalSurface = nullptr;
    javaVm->DetachCurrentThread();

    LOGI("Decode thread exited");

    return nullptr;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_longkd_videoplayer_NativePlayer_init(JNIEnv *env, jobject thiz) {
    env->GetJavaVM(&javaVm);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_longkd_videoplayer_NativePlayer_playVideo(JNIEnv *env, jobject /* this */,
                                                   jstring filePath, jobject surface) {
    const char *path = env->GetStringUTFChars(filePath, nullptr);
    strncpy(videoPath, path, sizeof(videoPath) - 1);
    env->ReleaseStringUTFChars(filePath, path);

    pthread_mutex_lock(&stateMutex);
    if (isPlaying) {
        pthread_mutex_unlock(&stateMutex);
        return; // Already playing
    }
    isPlaying = true;
    isPaused = false;

    globalSurface = env->NewGlobalRef(surface);
    pthread_mutex_unlock(&stateMutex);

    pthread_create(&decodeThread, nullptr, decodeRoutine, nullptr);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_longkd_videoplayer_NativePlayer_pauseVideo(JNIEnv *, jobject) {
    pthread_mutex_lock(&stateMutex);
    isPaused = true;
    pthread_mutex_unlock(&stateMutex);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_longkd_videoplayer_NativePlayer_resumeVideo(JNIEnv *, jobject) {
    pthread_mutex_lock(&stateMutex);
    isPaused = false;
    pthread_mutex_unlock(&stateMutex);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_longkd_videoplayer_NativePlayer_stopVideo(JNIEnv *, jobject) {
    pthread_mutex_lock(&stateMutex);
    if (!isPlaying) {
        pthread_mutex_unlock(&stateMutex);
        return;
    }
    isPlaying = false;
    pthread_mutex_unlock(&stateMutex);

    pthread_join(decodeThread, nullptr);  // Wait for cleanup
}
