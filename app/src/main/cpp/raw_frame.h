#pragma once
#include "raw_color.h"

namespace fuji {
/** Output-to-source mapping, shared by preview and export. Straightening trims empty corners. */
struct Frame {
    int width, height, sourceWidth, sourceHeight, turns;
    float rotatedWidth, rotatedHeight, safeWidth, safeHeight, cosine, sine;
    float left, top, cropWidth, cropHeight;
    double radial, vertical, horizontal, redScale, blueScale;

    Frame(int w, int h, const Adjustments& edits) : sourceWidth(w), sourceHeight(h), turns(edits.quarterTurns),
        rotatedWidth(turns % 2 ? h : w), rotatedHeight(turns % 2 ? w : h),
        left(edits.cropLeft), top(edits.cropTop),
        cropWidth(edits.cropRight - left), cropHeight(edits.cropBottom - top),
        radial(edits.distortion * .25), vertical(edits.perspectiveVertical * .35),
        horizontal(edits.perspectiveHorizontal * .35),
        redScale(1 + edits.chromaticRed * .002), blueScale(1 + edits.chromaticBlue * .002) {
        float angle = edits.straighten * 3.14159265358979323846f / 180;
        cosine = std::cos(angle);
        sine = std::sin(angle);
        float scale = std::min(rotatedWidth / (rotatedWidth * cosine + rotatedHeight * std::abs(sine)),
                               rotatedHeight / (rotatedWidth * std::abs(sine) + rotatedHeight * cosine));
        if (radial != 0 || vertical != 0 || horizontal != 0 || redScale != 1 || blueScale != 1) {
            double rotationBound = std::max(cosine + rotatedHeight / rotatedWidth * std::abs(sine),
                                           cosine + rotatedWidth / rotatedHeight * std::abs(sine));
            double perspectiveBound = std::abs(vertical) + std::abs(horizontal);
            double channelBound = std::max({1., redScale, blueScale});
            double low = 0, high = 1;
            // Conservative global bound: keep every channel inside the source, including corners.
            for (int i = 0; i < 40; ++i) {
                double candidate = (low + high) / 2;
                double projected = candidate / (1 - candidate * perspectiveBound);
                double bound = rotationBound * projected * (1 + std::max(0., radial) * projected * projected) * channelBound;
                if (bound <= 1) low = candidate; else high = candidate;
            }
            scale = std::min(scale, float(low));
        }
        safeWidth = rotatedWidth * scale;
        safeHeight = rotatedHeight * scale;
        width = std::max(1, int(std::round(safeWidth * cropWidth)));
        height = std::max(1, int(std::round(safeHeight * cropHeight)));
    }

    std::pair<double, double> unclampedSource(double x, double y, int channel = 1) const {
        double u = (left + (x + .5) / width * cropWidth - .5) * safeWidth;
        double v = (top + (y + .5) / height * cropHeight - .5) * safeHeight;
        double denominator = 1 + horizontal * u * 2 / rotatedWidth + vertical * v * 2 / rotatedHeight;
        u /= denominator;
        v /= denominator;
        double rx = cosine * u + sine * v;
        double ry = -sine * u + cosine * v;
        // ponytail: one radial coefficient; use calibrated multi-coefficient profiles for complex lenses.
        double factor = 1 + radial * (rx * rx + ry * ry) * 4 /
            (double(rotatedWidth) * rotatedWidth + double(rotatedHeight) * rotatedHeight);
        factor *= channel == 0 ? redScale : channel == 2 ? blueScale : 1;
        rx = rx * factor + rotatedWidth / 2;
        ry = ry * factor + rotatedHeight / 2;
        double sx, sy;
        switch (turns) {
            case 1: sx = ry; sy = sourceHeight - rx; break;
            case 2: sx = sourceWidth - rx; sy = sourceHeight - ry; break;
            case 3: sx = sourceWidth - ry; sy = rx; break;
            default: sx = rx; sy = ry;
        }
        return {sx - .5, sy - .5};
    }

    std::pair<float, float> source(int x, int y, int channel = 1) const {
        auto [sx, sy] = unclampedSource(x, y, channel);
        return {std::clamp(sx, 0., double(sourceWidth - 1)), std::clamp(sy, 0., double(sourceHeight - 1))};
    }
};

template <typename Sample>
inline Rgb sampleBilinear(float sx, float sy, int width, int height, const Sample& sample) {
    int left = int(sx), top = int(sy);
    int right = std::min(left + 1, width - 1), bottom = std::min(top + 1, height - 1);
    auto a = sample(left, top);
    if (sx == left && sy == top) return a;
    auto b = sample(right, top), c = sample(left, bottom), d = sample(right, bottom);
    Rgb result{};
    for (int i = 0; i < 3; ++i) result[i] =
        (a[i] * (1 - (sx - left)) + b[i] * (sx - left)) * (1 - (sy - top)) +
        (c[i] * (1 - (sx - left)) + d[i] * (sx - left)) * (sy - top);
    return result;
}

template <typename Sample>
inline Rgb sampleCamera(const Frame& frame, int x, int y, const Sample& sample) {
    auto channelSample = [&](int channel) {
        auto [sx, sy] = frame.source(x, y, channel);
        return sampleBilinear(sx, sy, frame.sourceWidth, frame.sourceHeight, sample);
    };
    auto camera = channelSample(1);
    if (frame.redScale != 1) camera[0] = channelSample(0)[0];
    if (frame.blueScale != 1) camera[2] = channelSample(2)[2];
    return camera;
}

inline Rgb vignette(Rgb pixel, int x, int y, int width, int height, const Adjustments& edits) {
    if (edits.vignette == 0) return pixel;
    float nx = std::abs(2 * (x + .5f) / width - 1), ny = std::abs(2 * (y + .5f) / height - 1);
    float ellipse = std::hypot(nx, ny);
    float shape = edits.vignetteRoundness < 0 ? std::max(nx, ny) :
        std::hypot(nx * std::max(1.f, float(width) / height), ny * std::max(1.f, float(height) / width));
    float distance = ellipse + std::abs(edits.vignetteRoundness) * (shape - ellipse);
    float inner = .05f + .85f * edits.vignetteMidpoint;
    float weight = std::clamp((distance - inner) / (.05f + .95f * edits.vignetteFeather), 0.f, 1.f);
    weight = weight * weight * (3 - 2 * weight);
    float gain = std::exp2(2 * edits.vignette * weight);
    for (auto& c : pixel) c = std::clamp(c * gain, 0.f, 1.f);
    return pixel;
}
} // namespace fuji
