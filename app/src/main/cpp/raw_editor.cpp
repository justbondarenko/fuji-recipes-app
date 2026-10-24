#include "raw_color.h"
#include "raw_detail.h"
#include "raw_frame.h"
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
    float cameraToRgb[3][3]{};
    std::vector<fuji::Rgb> preview, draftPreview, detailDraft;
    Session(std::string p, std::string profile) : path(std::move(p)), lut(profile) {}
};

std::pair<int,int> previewDimensions(const Session& session, bool draft, bool details = false) {
    int divisor = draft ? (details ? 4 : 2) : 1;
    return {std::max(1,session.width/divisor),std::max(1,session.height/divisor)};
}

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
fuji::Rgb cameraPixel(const LibRaw& raw, int x, int y) {
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
    for (int c = 0; c < 3; ++c)
        result[c] = pixel[c] * (color.cam_mul[c] / minimumWb) / color.maximum;
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

fuji::Adjustments adjustments(JNIEnv* env, jfloatArray parameters) {
    int count = env->GetArrayLength(parameters);
    if (count < 61 || count > 16384) throw std::runtime_error("Invalid RAW adjustment count.");
    std::vector<float> values(count);
    env->GetFloatArrayRegion(parameters, 0, count, values.data());
    return fuji::Adjustments::read(values.data(), count);
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
        for (int c = 0; c < 3; ++c) for (int sensor = 0; sensor < 3; ++sensor)
            session->cameraToRgb[c][sensor] = raw->imgdata.color.rgb_cam[c][sensor];
        float scale = std::min(1.f, 1600.f / std::max(width, height));
        session->width = std::max(1, int(width * scale));
        session->height = std::max(1, int(height * scale));
        session->preview = fuji::downsampleCamera(width,height,session->width,session->height,
            [&](int x,int y) { return cameraPixel(*raw,x,y); });
        auto [draftWidth,draftHeight] = previewDimensions(*session,true);
        session->draftPreview = fuji::downsampleCamera(session->width,session->height,draftWidth,draftHeight,
            [&](int x,int y) { return session->preview[y*session->width+x]; });
        auto [detailWidth,detailHeight] = previewDimensions(*session,true,true);
        session->detailDraft = fuji::downsampleCamera(draftWidth,draftHeight,detailWidth,detailHeight,
            [&](int x,int y) { return session->draftPreview[y*draftWidth+x]; });
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

extern "C" JNIEXPORT jintArray JNICALL
Java_dev_bondarenko_fujirecipes_data_raweditor_NativeRaw_outputDimensions(JNIEnv* env, jobject, jlong handle,
    jfloatArray parameters, jboolean full, jboolean draft) {
    try {
        const auto& s = *reinterpret_cast<Session*>(handle);
        if (full && draft) throw std::runtime_error("Export requires full resolution.");
        auto edits = adjustments(env,parameters);
        auto size = full ? std::make_pair(s.fullWidth,s.fullHeight) : previewDimensions(s,draft,edits.hasDetails());
        fuji::Frame frame(size.first,size.second,edits);
        jint values[] = {frame.width, frame.height};
        auto result = env->NewIntArray(2);
        if (result) env->SetIntArrayRegion(result, 0, 2, values);
        return result;
    } catch (const std::exception& e) { failure(env, e); return nullptr; }
}

extern "C" JNIEXPORT void JNICALL
Java_dev_bondarenko_fujirecipes_data_raweditor_NativeRaw_render(JNIEnv* env, jobject, jlong handle,
    jobject bitmap, jfloatArray parameters, jboolean full, jboolean draft) {
    void* pixels = nullptr;
    try {
        auto edits = adjustments(env, parameters);
        auto& s = *reinterpret_cast<Session*>(handle);
        if (full && draft) throw std::runtime_error("Export requires full resolution.");
        std::unique_ptr<LibRaw> raw;
        if (full) raw = decodeRaf(s.path, false);
        auto size = full ? dimensions(*raw) : previewDimensions(s,draft,edits.hasDetails());
        const auto& sourcePreview = draft ? (edits.hasDetails() ? s.detailDraft : s.draftPreview) : s.preview;
        fuji::Frame frame(size.first, size.second, edits);
        AndroidBitmapInfo info{};
        if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS ||
            info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || info.width != unsigned(frame.width) ||
            info.height != unsigned(frame.height)) throw std::runtime_error("Incorrect RAW output size.");
        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS)
            throw std::runtime_error("Could not allocate the rendered image.");
        auto maskSize = previewDimensions(s,!full && draft,edits.hasDetails());
        for (auto& mask : edits.masks) if (mask.enabled && mask.hasEdits() && mask.opacity > 0) mask.prepare(maskSize.first,maskSize.second);
        fuji::Frame fullFrame(s.fullWidth,s.fullHeight,edits);
        const auto globalGains = fuji::whiteBalanceGains(edits);
        auto develop = [&](const fuji::Rgb& camera, const fuji::LocalEdits& local = fuji::LocalEdits{}) {
            auto gains = local.temperature == 0 && local.tint == 0 ? globalGains : fuji::whiteBalanceGains(edits,local);
            fuji::Rgb linear{};
            for (int c = 0; c < 3; ++c) for (int sensor = 0; sensor < 3; ++sensor)
                linear[c] += s.cameraToRgb[c][sensor] * camera[sensor] * gains[sensor];
            return fuji::renderPixel(linear, s.lut, edits, s.baseline, local);
        };
        auto point = [&](int x, int y) {
            auto sample = [&](int px, int py) { return full ? cameraPixel(*raw, px, py) : sourcePreview[py * size.first + px]; };
            fuji::LocalEdits local;
            if (!edits.masks.empty()) {
                auto [sx,sy] = frame.unclampedSource(x,y);
                local = fuji::localEdits(edits.masks,(sx+.5f)/size.first,(sy+.5f)/size.second,float(s.fullWidth)/s.fullHeight);
            }
            return develop(fuji::sampleCamera(frame, x, y, sample),local);
        };
        fuji::Rgb airlight{1,1,1};
        if (edits.dehaze != 0) {
            // Estimate from the same retained image for preview/export, independent of framing.
            std::vector<fuji::Rgb> samples;
            for (int y = 0; y < s.height; y += std::max(1, s.height / 64))
                for (int x = 0; x < s.width; x += std::max(1, s.width / 64)) samples.push_back(develop(s.preview[y * s.width + x]));
            std::sort(samples.begin(), samples.end(), [](const auto& a, const auto& b) {
                return std::min({a[0],a[1],a[2]}) > std::min({b[0],b[1],b[2]});
            });
            int count = std::max(1, int(samples.size() / 1000));
            airlight = {};
            for (int i = 0; i < count; ++i) for (int c = 0; c < 3; ++c) airlight[c] += samples[i][c] / count;
            for (auto& c : airlight) c = std::clamp(c, .6f, 1.f);
        }
        auto write = [&](int x, int y, const fuji::Rgb& rgb) {
            auto* pixel = static_cast<uint8_t*>(pixels) + y * info.stride + x * 4;
            auto finished = fuji::vignette(rgb, x, y, frame.width, frame.height, edits);
            finished = fuji::addGrain(finished,x,y,frame.width,frame.height,fullFrame.width,fullFrame.height,
                edits.grainAmount,edits.grainSize,edits.grainRoughness);
            for (int c = 0; c < 3; ++c) pixel[c] = uint8_t(std::clamp(std::lround(finished[c] * 255), 0L, 255L));
            pixel[3] = 255;
        };
        if (!edits.hasDetails()) {
            for (int y = 0; y < frame.height; ++y) for (int x = 0; x < frame.width; ++x) write(x, y, point(x, y));
        } else {
            float scale = full ? 1.f : float(size.first) / s.fullWidth;
            int halo = fuji::detailHalo(edits, scale);
            // Bound filter memory to a strip, with overlapping halos to avoid seams.
            for (int top = 0; top < frame.height; top += 96) {
                int bottom = std::min(top + 96, frame.height);
                int first = std::max(0, top - halo), last = std::min(frame.height, bottom + halo);
                std::vector<fuji::Rgb> strip(frame.width * (last - first));
                for (int y = first; y < last; ++y) for (int x = 0; x < frame.width; ++x)
                    strip[(y - first) * frame.width + x] = point(x, y);
                fuji::applyDetails(strip, frame.width, last - first, edits, scale, airlight);
                for (int y = top; y < bottom; ++y) for (int x = 0; x < frame.width; ++x)
                    write(x, y, strip[(y - first) * frame.width + x]);
            }
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    } catch (const std::exception& e) {
        if (pixels) AndroidBitmap_unlockPixels(env, bitmap);
        failure(env, e);
    }
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_dev_bondarenko_fujirecipes_data_raweditor_NativeRaw_mapPoints(JNIEnv* env, jobject, jlong handle,
    jfloatArray parameters, jfloatArray points) {
    try {
        int count = env->GetArrayLength(points);
        if (count < 2 || count > 1024 || count % 2) throw std::runtime_error("Invalid mask coordinates.");
        std::vector<float> values(count);
        env->GetFloatArrayRegion(points,0,count,values.data());
        const auto& s = *reinterpret_cast<Session*>(handle);
        fuji::Frame frame(s.fullWidth,s.fullHeight,adjustments(env,parameters));
        for (int i = 0; i < count; i += 2) {
            if (!std::isfinite(values[i]) || !std::isfinite(values[i+1]) || values[i] < 0 || values[i] > 1 || values[i+1] < 0 || values[i+1] > 1)
                throw std::runtime_error("Invalid mask coordinates.");
            auto [x,y] = frame.unclampedSource(values[i]*frame.width-.5, values[i+1]*frame.height-.5);
            values[i] = std::clamp(float((x+.5)/s.fullWidth),0.f,1.f);
            values[i+1] = std::clamp(float((y+.5)/s.fullHeight),0.f,1.f);
        }
        auto result = env->NewFloatArray(count);
        if (result) env->SetFloatArrayRegion(result,0,count,values.data());
        return result;
    } catch (const std::exception& e) { failure(env,e); return nullptr; }
}

extern "C" JNIEXPORT void JNICALL
Java_dev_bondarenko_fujirecipes_data_raweditor_NativeRaw_maskOverlay(JNIEnv* env, jobject, jlong handle,
    jobject bitmap, jfloatArray parameters, jint index, jboolean draft) {
    void* pixels = nullptr;
    try {
        auto edits = adjustments(env,parameters);
        if (index < 0 || index >= int(edits.masks.size())) throw std::runtime_error("Invalid mask selection.");
        auto& s = *reinterpret_cast<Session*>(handle);
        auto [width,height] = previewDimensions(s,draft,edits.hasDetails());
        auto& mask = edits.masks[index]; mask.prepare(width,height);
        fuji::Frame frame(width,height,edits);
        AndroidBitmapInfo info{};
        if (AndroidBitmap_getInfo(env,bitmap,&info) != ANDROID_BITMAP_RESULT_SUCCESS ||
            info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || info.width != unsigned(frame.width) || info.height != unsigned(frame.height))
            throw std::runtime_error("Incorrect mask preview size.");
        if (AndroidBitmap_lockPixels(env,bitmap,&pixels) != ANDROID_BITMAP_RESULT_SUCCESS) throw std::runtime_error("Could not allocate mask overlay.");
        for (int y = 0; y < frame.height; ++y) for (int x = 0; x < frame.width; ++x) {
            auto [sx,sy] = frame.unclampedSource(x,y);
            float coverage = mask.coverage((sx+.5f)/width,(sy+.5f)/height,float(s.fullWidth)/s.fullHeight);
            auto* p = static_cast<uint8_t*>(pixels) + y*info.stride + x*4;
            uint8_t alpha = uint8_t(std::lround(coverage*mask.opacity*128));
            p[0] = p[1] = p[2] = p[3] = alpha; // Premultiplied white; Compose supplies the overlay tint.
        }
        AndroidBitmap_unlockPixels(env,bitmap);
    } catch (const std::exception& e) {
        if (pixels) AndroidBitmap_unlockPixels(env,bitmap);
        failure(env,e);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_dev_bondarenko_fujirecipes_data_raweditor_NativeRaw_close(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<Session*>(handle);
}
