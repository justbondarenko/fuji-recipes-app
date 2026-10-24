#include "raw_color.h"
#include "libraw/libraw.h"
#include <android/bitmap.h>
#include <jni.h>
#include <memory>
#include <cstring>

namespace {
struct Session {
    std::string path;
    fuji::Lut lut;
    int width = 0, height = 0, fullWidth = 0, fullHeight = 0;
    float baseline = 1;
    std::vector<fuji::Rgb> preview;
    Session(std::string p, std::string profile) : path(std::move(p)), lut(profile) {}
};

void check(int result) {
    if (result != LIBRAW_SUCCESS) throw std::runtime_error(std::string("RAW decoding: ") + LibRaw::strerror(result));
}

std::unique_ptr<LibRaw> decodeRaf(const std::string& path, bool half, std::pair<int, int>* fullSize = nullptr) {
    auto raw = std::make_unique<LibRaw>(LIBRAW_OPTIONS_NO_DATAERR_CALLBACK);
    auto& p = raw->imgdata.params;
    p.half_size = half;
    p.output_color = 0; // Keep sensor RGB: WB and the camera matrix are applied in float below.
    p.no_auto_scale = 1;
    p.no_auto_bright = 1;
    p.adjust_maximum_thr = 0;
    p.highlight = 1;
    p.user_qual = 2;
    raw->imgdata.rawparams.max_raw_memory_mb = 768;
    check(raw->open_file(path.c_str()));
    if (std::strcmp(raw->imgdata.idata.make, "Fujifilm") != 0 &&
        std::strcmp(raw->imgdata.idata.make, "FUJIFILM") != 0)
        throw std::runtime_error("Choose a Fujifilm RAF file.");
    auto& s = raw->imgdata.sizes;
    if (uint64_t(s.raw_width) * s.raw_height > 110000000)
        throw std::runtime_error("This RAW exceeds the 110 megapixel limit.");
    // Respect the active image and the camera's selected aspect ratio, before demosaicing.
    raw->adjust_to_raw_inset_crop(3);
    if (fullSize) *fullSize = (s.flip & 4) ? std::make_pair(s.height, s.width) : std::make_pair(s.width, s.height);
    check(raw->unpack());
    check(raw->dcraw_process());
    if (raw->imgdata.idata.colors != 3 || !raw->imgdata.image)
        throw std::runtime_error("This sensor format is not supported.");
    return raw;
}

std::pair<int, int> dimensions(const LibRaw& raw) {
    const auto& s = raw.imgdata.sizes;
    return (s.flip & 4) ? std::make_pair(s.height, s.width) : std::make_pair(s.width, s.height);
}

// Coordinates are displayed upright; the sensor buffer itself has not been rotated.
fuji::Rgb linearPixel(const LibRaw& raw, int x, int y) {
    const auto& d = raw.imgdata;
    int row = y, col = x;
    if (d.sizes.flip & 4) std::swap(row, col);
    if (d.sizes.flip & 2) row = d.sizes.height - 1 - row;
    if (d.sizes.flip & 1) col = d.sizes.width - 1 - col;
    const auto* pixel = d.image[row * d.sizes.width + col];
    const auto& color = d.color;
    float minimumWb = std::min({color.cam_mul[0], color.cam_mul[1], color.cam_mul[2]});
    if (!(minimumWb > 0) || color.maximum == 0)
        throw std::runtime_error("The RAW has no usable as-shot white balance.");
    fuji::Rgb result{};
    for (int c = 0; c < 3; ++c) for (int sensor = 0; sensor < 3; ++sensor)
        result[c] += color.rgb_cam[c][sensor] * pixel[sensor] *
                     (color.cam_mul[sensor] / minimumWb) / color.maximum;
    return result;
}

void failure(JNIEnv* env, const std::exception& error) {
    env->ThrowNew(env->FindClass("java/io/IOException"), error.what());
}

std::string stringValue(JNIEnv* env, jstring value) {
    const char* chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) throw std::runtime_error("Could not read RAW path.");
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_dev_bondarenko_fujirecipes_data_raweditor_NativeRaw_open(JNIEnv* env, jobject, jstring path, jbyteArray profile) {
    try {
        int length = env->GetArrayLength(profile);
        if (length > 2 * 1024 * 1024) throw std::runtime_error("Color profile is too large.");
        std::string text(length, '\0');
        env->GetByteArrayRegion(profile, 0, length, reinterpret_cast<jbyte*>(text.data()));
        auto session = std::make_unique<Session>(stringValue(env, path), text);
        std::pair<int, int> fullSize;
        auto raw = decodeRaf(session->path, true, &fullSize);
        auto [width, height] = dimensions(*raw);
        session->fullWidth = fullSize.first;
        session->fullHeight = fullSize.second;
        auto dr = raw->imgdata.makernotes.fuji.DevelopmentDynamicRange;
        session->baseline = dr == 400 ? 4.f : dr == 200 ? 2.f : 1.f;
        float scale = std::min(1.f, 1600.f / std::max(width, height));
        session->width = std::max(1, int(width * scale));
        session->height = std::max(1, int(height * scale));
        session->preview.resize(session->width * session->height);
        for (int y = 0; y < session->height; ++y) for (int x = 0; x < session->width; ++x) {
            fuji::Rgb sum{};
            int count = 0;
            for (int sy = y * height / session->height; sy < (y + 1) * height / session->height; ++sy)
                for (int sx = x * width / session->width; sx < (x + 1) * width / session->width; ++sx) {
                    auto pixel = linearPixel(*raw, sx, sy);
                    for (int c = 0; c < 3; ++c) sum[c] += pixel[c];
                    ++count;
                }
            for (auto& c : sum) c /= count;
            session->preview[y * session->width + x] = sum;
        }
        return reinterpret_cast<jlong>(session.release());
    } catch (const std::exception& e) { failure(env, e); return 0; }
}

extern "C" JNIEXPORT jintArray JNICALL
Java_dev_bondarenko_fujirecipes_data_raweditor_NativeRaw_dimensions(JNIEnv* env, jobject, jlong handle) {
    const auto& s = *reinterpret_cast<Session*>(handle);
    jint values[] = {s.width, s.height, s.fullWidth, s.fullHeight};
    auto result = env->NewIntArray(4);
    if (result) env->SetIntArrayRegion(result, 0, 4, values);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_bondarenko_fujirecipes_data_raweditor_NativeRaw_render(JNIEnv* env, jobject, jlong handle,
    jobject bitmap, jfloat exposure, jfloat shadows, jfloat highlights, jboolean full) {
    void* pixels = nullptr;
    try {
        if (!std::isfinite(exposure) || std::abs(exposure) > 3 || !std::isfinite(shadows) ||
            std::abs(shadows) > 1 || !std::isfinite(highlights) || std::abs(highlights) > 1)
            throw std::runtime_error("Invalid RAW adjustment.");
        auto& s = *reinterpret_cast<Session*>(handle);
        std::unique_ptr<LibRaw> raw;
        if (full) raw = decodeRaf(s.path, false);
        auto size = full ? dimensions(*raw) : std::make_pair(s.width, s.height);
        AndroidBitmapInfo info{};
        if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS ||
            info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || info.width != unsigned(size.first) ||
            info.height != unsigned(size.second)) throw std::runtime_error("Incorrect RAW output size.");
        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS)
            throw std::runtime_error("Could not allocate the rendered image.");
        fuji::Adjustments edits{exposure, shadows, highlights};
        for (int y = 0; y < size.second; ++y) {
            auto* row = static_cast<uint8_t*>(pixels) + y * info.stride;
            for (int x = 0; x < size.first; ++x) {
                auto linear = full ? linearPixel(*raw, x, y) : s.preview[y * s.width + x];
                auto rgb = fuji::renderPixel(linear, s.lut, edits, s.baseline);
                for (int c = 0; c < 3; ++c) row[x * 4 + c] = uint8_t(std::clamp(std::lround(rgb[c] * 255), 0L, 255L));
                row[x * 4 + 3] = 255;
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    } catch (const std::exception& e) {
        if (pixels) AndroidBitmap_unlockPixels(env, bitmap);
        failure(env, e);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_dev_bondarenko_fujirecipes_data_raweditor_NativeRaw_close(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<Session*>(handle);
}
