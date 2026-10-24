#pragma once
#include "raw_color.h"

namespace fuji {
/** Output-to-source mapping, shared by preview and export. Straightening trims empty corners. */
struct Frame {
    int width, height, sourceWidth, sourceHeight, turns;
    float rotatedWidth, rotatedHeight, safeWidth, safeHeight, cosine, sine;
    float left, top, cropWidth, cropHeight;

    Frame(int w, int h, const Adjustments& edits) : sourceWidth(w), sourceHeight(h), turns(edits.quarterTurns),
        rotatedWidth(turns % 2 ? h : w), rotatedHeight(turns % 2 ? w : h),
        left(edits.cropLeft), top(edits.cropTop),
        cropWidth(edits.cropRight - left), cropHeight(edits.cropBottom - top) {
        float angle = edits.straighten * 3.14159265358979323846f / 180;
        cosine = std::cos(angle);
        sine = std::sin(angle);
        float scale = std::min(rotatedWidth / (rotatedWidth * cosine + rotatedHeight * std::abs(sine)),
                               rotatedHeight / (rotatedWidth * std::abs(sine) + rotatedHeight * cosine));
        safeWidth = rotatedWidth * scale;
        safeHeight = rotatedHeight * scale;
        width = std::max(1, int(std::round(safeWidth * cropWidth)));
        height = std::max(1, int(std::round(safeHeight * cropHeight)));
    }

    std::pair<float, float> source(int x, int y) const {
        double u = (left + (x + .5) / width * cropWidth - .5) * safeWidth;
        double v = (top + (y + .5) / height * cropHeight - .5) * safeHeight;
        double rx = cosine * u + sine * v + rotatedWidth / 2;
        double ry = -sine * u + cosine * v + rotatedHeight / 2;
        double sx, sy;
        switch (turns) {
            case 1: sx = ry; sy = sourceHeight - rx; break;
            case 2: sx = sourceWidth - rx; sy = sourceHeight - ry; break;
            case 3: sx = sourceWidth - ry; sy = rx; break;
            default: sx = rx; sy = ry;
        }
        return {std::clamp(sx - .5, 0., double(sourceWidth - 1)),
                std::clamp(sy - .5, 0., double(sourceHeight - 1))};
    }
};

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
