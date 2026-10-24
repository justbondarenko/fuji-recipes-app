#pragma once
#include "raw_color.h"
#include "raw_effects.h"

namespace fuji {
inline int detailHalo(const Adjustments& edits, float scale) {
    int noise = (edits.colorNoise > 0 || edits.luminanceNoise > 0) ? std::max(1, int(std::round(2 * scale))) : 0;
    int sharp = edits.sharpening > 0 ? int(std::ceil(3 * std::max(.5f, edits.sharpenRadius * scale))) : 0;
    int texture = edits.texture != 0 ? std::max(1, int(std::round(6 * scale))) : 0;
    int clarity = edits.clarity != 0 ? std::max(1, int(std::round(40 * scale))) : 0;
    int haze = edits.dehaze > 0 ? 2 * std::max(1, int(std::round(12 * scale))) : 0;
    int fringe = (edits.defringePurple > 0 || edits.defringeGreen > 0) ? std::max(1, int(std::round(2 * scale))) : 0;
    return noise + sharp + texture + clarity + haze + fringe;
}

inline void defringe(std::vector<Rgb>& image, int width, int height, const Adjustments& edits, float scale) {
    if (edits.defringePurple == 0 && edits.defringeGreen == 0) return;
    const auto source = image;
    int radius = std::max(1, int(std::round(2 * scale)));
    for (int y = 0; y < height; ++y) for (int x = 0; x < width; ++x) {
        const auto& center = source[y * width + x];
        float purple = std::min(center[0], center[2]) - center[1];
        float green = center[1] - std::max(center[0], center[2]);
        float amount = purple > .025f ? edits.defringePurple : green > .025f ? edits.defringeGreen : 0;
        if (amount == 0) continue;
        float low = 1, high = 0;
        // Require nearby neutral dark and bright pixels, keeping flat colored subjects untouched.
        for (int distance = 1; distance <= radius; ++distance) for (int direction = 0; direction < 4; ++direction) {
            int nx = std::clamp(x + (direction == 0 ? -distance : direction == 1 ? distance : 0), 0, width - 1);
            int ny = std::clamp(y + (direction == 2 ? -distance : direction == 3 ? distance : 0), 0, height - 1);
            const auto& neighbor = source[ny * width + nx];
            if (std::max({neighbor[0], neighbor[1], neighbor[2]}) - std::min({neighbor[0], neighbor[1], neighbor[2]}) <= .08f) {
                float luma = luminance(neighbor);
                low = std::min(low, luma);
                high = std::max(high, luma);
            }
        }
        float luma = luminance(center);
        if (high - low < .2f || low >= luma - .03f || high <= luma + .03f) continue;
        // ponytail: conservative neutral-edge defringe; add hue-range controls for colored-edge fringes.
        float weight = amount * std::clamp((std::max(purple, green) - .025f) / .15f, 0.f, 1.f);
        for (int c = 0; c < 3; ++c) image[y * width + x][c] = luma + (center[c] - luma) * (1 - weight);
    }
}

inline void denoise(std::vector<Rgb>& image, int width, int height, const Adjustments& edits, float scale) {
    if (edits.colorNoise == 0 && edits.luminanceNoise == 0) return;
    auto source = image;
    int radius = std::max(1, int(std::round(2 * scale)));
    float sigma = .025f + .10f * (1 - edits.noiseDetail);
    float inverseRange = 1 / (2 * sigma * sigma);
    for (int y = 0; y < height; ++y) for (int x = 0; x < width; ++x) {
        const auto& center = source[y * width + x];
        float centerY = luminance(center);
        Rgb sum{};
        float total = 0;
        // ponytail: local bilateral denoise; use multiscale processing if larger-grain noise remains.
        for (int dy = -radius; dy <= radius; ++dy) for (int dx = -radius; dx <= radius; ++dx) {
            const auto& sample = source[std::clamp(y + dy, 0, height - 1) * width + std::clamp(x + dx, 0, width - 1)];
            float difference = luminance(sample) - centerY;
            float chromaDifference = (sample[0] - sample[2]) - (center[0] - center[2]);
            float range = 1 + difference * difference * inverseRange + chromaDifference * chromaDifference * 8;
            float weight = 1 / (range * range * (1 + dx * dx + dy * dy));
            for (int c = 0; c < 3; ++c) sum[c] += sample[c] * weight;
            total += weight;
        }
        for (auto& c : sum) c /= total;
        float filteredY = luminance(sum);
        float outputY = centerY + edits.luminanceNoise * (filteredY - centerY);
        for (int c = 0; c < 3; ++c) image[y * width + x][c] = std::clamp(
            outputY + (center[c] - centerY) * (1 - edits.colorNoise) +
            (sum[c] - filteredY) * edits.colorNoise, 0.f, 1.f);
    }
}

inline void sharpen(std::vector<Rgb>& image, int width, int height, const Adjustments& edits, float scale) {
    if (edits.sharpening == 0) return;
    float sigma = std::max(.5f, edits.sharpenRadius * scale);
    int radius = int(std::ceil(3 * sigma));
    std::vector<float> kernel(radius * 2 + 1), horizontal(width * height);
    float total = 0;
    for (int k = -radius; k <= radius; ++k) {
        kernel[k + radius] = std::exp(-k * k / (2 * sigma * sigma));
        total += kernel[k + radius];
    }
    for (auto& k : kernel) k /= total;
    for (int y = 0; y < height; ++y) for (int x = 0; x < width; ++x) {
        float value = 0;
        for (int k = -radius; k <= radius; ++k)
            value += luminance(image[y * width + std::clamp(x + k, 0, width - 1)]) * kernel[k + radius];
        horizontal[y * width + x] = value;
    }
    for (int y = 0; y < height; ++y) for (int x = 0; x < width; ++x) {
        float blurred = 0;
        for (int k = -radius; k <= radius; ++k)
            blurred += horizontal[std::clamp(y + k, 0, height - 1) * width + x] * kernel[k + radius];
        auto& pixel = image[y * width + x];
        float difference = luminance(pixel) - blurred;
        float threshold = edits.sharpenThreshold * .08f;
        float detail = std::copysign(std::max(0.f, std::abs(difference) - threshold), difference);
        for (auto& c : pixel) c = std::clamp(c + detail * edits.sharpening * 2, 0.f, 1.f);
    }
}

inline void applyDetails(std::vector<Rgb>& image, int width, int height, const Adjustments& edits, float scale, const Rgb& airlight = {1,1,1}) {
    defringe(image, width, height, edits, scale);
    dehaze(image, width, height, edits.dehaze, scale, airlight);
    denoise(image, width, height, edits, scale);
    localContrast(image, width, height, edits.texture, scale, true);
    localContrast(image, width, height, edits.clarity, scale, false);
    sharpen(image, width, height, edits, scale);
}
} // namespace fuji
