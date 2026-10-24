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
};

inline Rgb renderPixel(Rgb linear, const Lut& lut, const Adjustments& edits, float baseline) {
    for (auto& c : linear) c = std::max(0.f, c * baseline);
    float luminance = .2126f * linear[0] + .7152f * linear[1] + .0722f * linear[2];
    float shadowWeight = std::pow(std::max(0.f, 1.f - luminance / .5f), 2.f);
    float highlightWeight = std::clamp((luminance - .25f) / .75f, 0.f, 1.f);
    float gain = std::exp2(edits.exposure + edits.shadows * 2.f * shadowWeight +
                          edits.highlights * 2.f * highlightWeight);
    for (auto& c : linear) c = srgb(c * gain);
    return lut.apply(linear);
}
} // namespace fuji
