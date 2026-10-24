#pragma once

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>

namespace fuji {
using Rgb = std::array<float, 3>;

inline float srgb(float x) {
    x = std::max(0.f, x);
    return x <= .0031308f ? 12.92f * x : 1.055f * std::pow(x, 1.f / 2.4f) - .055f;
}

struct Lut {
    int size = 0;
    std::vector<Rgb> values;

    explicit Lut(const std::string& text) {
        std::istringstream input(text);
        std::string line;
        while (std::getline(input, line)) {
            auto first = line.find_first_not_of(" \t\r");
            if (first == std::string::npos || line[first] == '#') continue;
            std::istringstream row(line.substr(first));
            if (line.compare(first, 11, "LUT_3D_SIZE") == 0) {
                std::string label;
                row >> label >> size;
                if (size < 2 || size > 64) throw std::runtime_error("Invalid color profile size.");
            } else {
                Rgb v{};
                if (!(row >> v[0] >> v[1] >> v[2])) throw std::runtime_error("Unreadable color profile.");
                for (float c : v) if (!std::isfinite(c) || c < 0 || c > 1)
                    throw std::runtime_error("Invalid color profile value.");
                if (values.size() >= 64 * 64 * 64) throw std::runtime_error("Color profile is too large.");
                values.push_back(v);
            }
        }
        if (size == 0 || values.size() != size_t(size * size * size))
            throw std::runtime_error("Incomplete color profile.");
    }

    Rgb apply(Rgb rgb) const {
        int low[3];
        float fraction[3];
        for (int c = 0; c < 3; ++c) {
            float p = std::clamp(rgb[c], 0.f, 1.f) * (size - 1);
            low[c] = std::min(int(p), size - 2);
            fraction[c] = p - low[c];
        }
        Rgb result{};
        // .cube order: red changes fastest, then green, then blue.
        for (int b = 0; b < 2; ++b) for (int g = 0; g < 2; ++g) for (int r = 0; r < 2; ++r) {
            float weight = (r ? fraction[0] : 1 - fraction[0]) *
                           (g ? fraction[1] : 1 - fraction[1]) *
                           (b ? fraction[2] : 1 - fraction[2]);
            const auto& value = values[((low[2] + b) * size + low[1] + g) * size + low[0] + r];
            for (int c = 0; c < 3; ++c) result[c] += weight * value[c];
        }
        return result;
    }
};

struct Adjustments {
    float exposure = 0, shadows = 0, highlights = 0;
    float temperature = 0, tint = 0, contrast = 0, whites = 0, blacks = 0;
    float saturation = 0, vibrance = 0;
    float sharpening = 0, sharpenRadius = 1, sharpenThreshold = 0;
    float colorNoise = 0, luminanceNoise = 0, noiseDetail = .5f;
    float clarity = 0, texture = 0, dehaze = 0;
    float cropLeft = 0, cropTop = 0, cropRight = 1, cropBottom = 1;
    int quarterTurns = 0;
    float straighten = 0;
    float vignette = 0, vignetteMidpoint = .5f, vignetteRoundness = 0, vignetteFeather = .5f;

    static Adjustments read(const float* p, int count) {
        if (count != 29) throw std::runtime_error("Invalid RAW adjustment count.");
        constexpr float minimum[] = {-3,-1,-1,-1,-1,-1,-1,-1,-1,-1,0,.5f,0,0,0,0,-1,-1,-1,0,0,0,0,0,-15,-1,0,-1,0};
        constexpr float maximum[] = {3,1,1,1,1,1,1,1,1,1,1,3,1,1,1,1,1,1,1,1,1,1,1,3,15,1,1,1,1};
        for (int i = 0; i < count; ++i) {
            if (!std::isfinite(p[i]) || p[i] < minimum[i] || p[i] > maximum[i])
                throw std::runtime_error("Invalid RAW adjustment.");
        }
        if (p[21] - p[19] < .04999f || p[22] - p[20] < .04999f || p[23] != std::round(p[23]))
            throw std::runtime_error("Invalid RAW framing.");
        return {p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7],
                p[8], p[9], p[10], p[11], p[12], p[13], p[14], p[15], p[16], p[17], p[18],
                p[19], p[20], p[21], p[22], int(p[23]), p[24], p[25], p[26], p[27], p[28]};
    }

    bool hasDetails() const {
        return sharpening > 0 || colorNoise > 0 || luminanceNoise > 0 || clarity != 0 || texture != 0 || dehaze != 0;
    }
};

inline float luminance(const Rgb& rgb) { return .2126f * rgb[0] + .7152f * rgb[1] + .0722f * rgb[2]; }

inline Rgb whiteBalanceGains(const Adjustments& edits) {
    // Relative camera-space WB, not a fabricated absolute Kelvin reading for Auto WB.
    return {std::exp2(edits.temperature * .75f), std::exp2(-edits.tint * .5f), std::exp2(-edits.temperature * .75f)};
}

inline Rgb finishColor(Rgb rgb, const Adjustments& edits) {
    if (edits.contrast != 0) {
        float factor = std::exp2(edits.contrast);
        for (auto& c : rgb) c = std::clamp((c - .5f) * factor + .5f, 0.f, 1.f);
    }
    float y = luminance(rgb);
    float shift = .25f * (edits.whites * y * y + edits.blacks * (1 - y) * (1 - y));
    if (shift != 0) for (auto& c : rgb) c = std::clamp(c + shift, 0.f, 1.f);
    if (edits.saturation != 0 || edits.vibrance != 0) {
        y = luminance(rgb);
        float maximum = std::max({rgb[0], rgb[1], rgb[2]});
        float minimum = std::min({rgb[0], rgb[1], rgb[2]});
        float muted = maximum > 0 ? 1 - (maximum - minimum) / maximum : 1;
        float amount = (1 + edits.saturation) * (1 + edits.vibrance * muted);
        for (auto& c : rgb) c = std::clamp(y + (c - y) * amount, 0.f, 1.f);
    }
    return rgb;
}

inline Rgb renderPixel(Rgb linear, const Lut& lut, const Adjustments& edits, float baseline) {
    for (auto& c : linear) c = std::max(0.f, c * baseline);
    float luminance = .2126f * linear[0] + .7152f * linear[1] + .0722f * linear[2];
    float shadowWeight = std::pow(std::max(0.f, 1.f - luminance / .5f), 2.f);
    float highlightWeight = std::clamp((luminance - .25f) / .75f, 0.f, 1.f);
    float gain = std::exp2(edits.exposure + edits.shadows * 2.f * shadowWeight +
                          edits.highlights * 2.f * highlightWeight);
    for (auto& c : linear) c = srgb(c * gain);
    return finishColor(lut.apply(linear), edits);
}
} // namespace fuji
