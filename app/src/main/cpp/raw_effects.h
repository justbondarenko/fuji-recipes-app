#pragma once
#include "raw_color.h"
#include <deque>

namespace fuji {
inline std::vector<float> boxBlur(const std::vector<float>& input, int width, int height, int radius) {
    std::vector<float> horizontal(input.size()), output(input.size());
    // Sliding sums keep large-radius local contrast linear in the number of pixels.
    for (int y = 0; y < height; ++y) {
        double sum = 0;
        for (int k = -radius; k <= radius; ++k) sum += input[y * width + std::clamp(k, 0, width - 1)];
        for (int x = 0; x < width; ++x) {
            horizontal[y * width + x] = sum / (2 * radius + 1);
            sum += input[y * width + std::min(width - 1, x + radius + 1)] - input[y * width + std::max(0, x - radius)];
        }
    }
    for (int x = 0; x < width; ++x) {
        double sum = 0;
        for (int k = -radius; k <= radius; ++k) sum += horizontal[std::clamp(k, 0, height - 1) * width + x];
        for (int y = 0; y < height; ++y) {
            output[y * width + x] = sum / (2 * radius + 1);
            sum += horizontal[std::min(height - 1, y + radius + 1) * width + x] - horizontal[std::max(0, y - radius) * width + x];
        }
    }
    return output;
}

inline std::vector<float> localMinimum(const std::vector<float>& input, int width, int height, int radius) {
    std::vector<float> horizontal(input.size()), output(input.size());
    auto pass = [&](const auto& source, auto& target, int lines, int length, int stride, int lineStride) {
        for (int line = 0; line < lines; ++line) {
            std::deque<int> window;
            int next = 0, offset = line * lineStride;
            for (int p = 0; p < length; ++p) {
                while (next < length && next <= p + radius) {
                    while (!window.empty() && source[offset + window.back() * stride] >= source[offset + next * stride]) window.pop_back();
                    window.push_back(next++);
                }
                while (window.front() < p - radius) window.pop_front();
                target[offset + p * stride] = source[offset + window.front() * stride];
            }
        }
    };
    pass(input, horizontal, height, width, 1, width);
    pass(horizontal, output, width, height, width, 1);
    return output;
}

inline void dehaze(std::vector<Rgb>& image, int width, int height, float amount, float scale, const Rgb& airlight) {
    if (amount == 0) return;
    if (amount < 0) {
        for (auto& pixel : image) for (int c = 0; c < 3; ++c) pixel[c] += -amount * .4f * (airlight[c] - pixel[c]);
        return;
    }
    // Dark-channel transmission: He, Sun & Tang, CVPR 2009, doi:10.1109/CVPR.2009.5206515.
    std::vector<float> dark(image.size());
    for (size_t i = 0; i < image.size(); ++i) dark[i] = std::min({image[i][0] / airlight[0], image[i][1] / airlight[1], image[i][2] / airlight[2]});
    int radius = std::max(1, int(std::round(12 * scale)));
    dark = boxBlur(localMinimum(dark, width, height, radius), width, height, radius);
    // ponytail: smoothed dark-channel transmission; guided refinement if boundary halos remain.
    for (size_t i = 0; i < image.size(); ++i) {
        float transmission = std::max(.2f, 1 - amount * .85f * dark[i]);
        for (int c = 0; c < 3; ++c) image[i][c] = std::clamp((image[i][c] - airlight[c]) / transmission + airlight[c], 0.f, 1.f);
    }
}

inline void localContrast(std::vector<Rgb>& image, int width, int height, float amount, float scale, bool texture) {
    if (amount == 0) return;
    std::vector<float> luma(image.size());
    for (size_t i = 0; i < image.size(); ++i) luma[i] = luminance(image[i]);
    auto base = boxBlur(luma, width, height, std::max(1, int(std::round((texture ? 6 : 40) * scale))));
    for (size_t i = 0; i < image.size(); ++i) {
        float weight = texture ? 1 : 4 * luma[i] * (1 - luma[i]);
        float detail = (luma[i] - base[i]) * amount * weight;
        for (auto& c : image[i]) c = std::clamp(c + detail, 0.f, 1.f);
    }
}
} // namespace fuji
