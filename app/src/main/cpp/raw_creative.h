#pragma once
#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <stdexcept>
#include <vector>

namespace fuji {
using Rgb = std::array<float, 3>;
struct EditPoint { float x, y; };

struct ToneCurve {
    std::vector<EditPoint> points;
    std::array<float, 1025> table{};
    bool active = false;

    void prepare() {
        active = false;
        for (auto p : points) active |= p.x != p.y;
        if (!active) return;
        auto slope = [&](int i) { return (points[i+1].y - points[i].y) / (points[i+1].x - points[i].x); };
        auto tangent = [&](int i) {
            if (i == 0) return slope(0);
            if (i == int(points.size()) - 1) return slope(i - 1);
            float a = slope(i - 1), b = slope(i);
            return a * b <= 0 ? 0.f : std::copysign(std::min(std::abs(a), std::abs(b)), a);
        };
        int segment = 0;
        for (int k = 0; k <= 1024; ++k) {
            float x = k / 1024.f;
            while (segment + 2 < int(points.size()) && points[segment + 1].x < x) ++segment;
            auto a = points[segment], b = points[segment + 1];
            float h = b.x - a.x, t = (x - a.x) / h;
            table[k] = std::clamp((2*t*t*t - 3*t*t + 1)*a.y + (t*t*t - 2*t*t + t)*h*tangent(segment) +
                (-2*t*t*t + 3*t*t)*b.y + (t*t*t - t*t)*h*tangent(segment + 1), 0.f, 1.f);
        }
    }

    float apply(float value) const {
        if (!active) return value;
        float p = std::clamp(value, 0.f, 1.f) * 1024;
        int i = std::min(1023, int(p));
        return table[i] + (table[i+1] - table[i]) * (p - i);
    }
};

struct LocalEdits {
    float exposure = 0, shadows = 0, highlights = 0, temperature = 0, tint = 0, contrast = 0, saturation = 0;
};
struct BrushStroke { float radius; bool erase; std::vector<EditPoint> points; };

inline float smoothStep(float value) {
    float t = std::clamp(value, 0.f, 1.f);
    return t * t * (3 - 2 * t);
}

struct LocalMask {
    int kind = 0;
    bool enabled = true, inverted = false;
    float opacity = 1, feather = .5f;
    EditPoint start{.5f,.5f}, end{.8f,.75f};
    LocalEdits edits;
    std::vector<BrushStroke> strokes;
    int width = 0, height = 0;
    std::vector<float> brush;

    bool hasEdits() const {
        return edits.exposure != 0 || edits.shadows != 0 || edits.highlights != 0 || edits.temperature != 0 ||
            edits.tint != 0 || edits.contrast != 0 || edits.saturation != 0;
    }

    void prepare(int w, int h) {
        if (kind != 0 || strokes.empty()) return;
        // ponytail: brush coverage at preview resolution; use tiled full-resolution masks for subpixel brush work.
        width = w; height = h;
        brush.assign(w * h, 0);
        std::vector<float> stroke(w * h);
        for (const auto& path : strokes) {
            std::fill(stroke.begin(), stroke.end(), 0);
            float radius = std::max(.5f, path.radius * std::min(w, h));
            for (size_t i = 0; i < path.points.size(); ++i) {
                auto p = path.points[i == 0 ? 0 : i - 1], q = path.points[i];
                float ax = p.x * w, ay = p.y * h, bx = q.x * w, by = q.y * h;
                float dx = bx - ax, dy = by - ay, length = dx*dx + dy*dy;
                int left = std::max(0, int(std::floor(std::min(ax,bx) - radius)));
                int right = std::min(w-1, int(std::ceil(std::max(ax,bx) + radius)));
                int top = std::max(0, int(std::floor(std::min(ay,by) - radius)));
                int bottom = std::min(h-1, int(std::ceil(std::max(ay,by) + radius)));
                for (int y = top; y <= bottom; ++y) for (int x = left; x <= right; ++x) {
                    float t = length == 0 ? 0 : std::clamp(((x+.5f-ax)*dx + (y+.5f-ay)*dy) / length, 0.f, 1.f);
                    float distance = std::hypot(x+.5f-ax-t*dx, y+.5f-ay-t*dy) / radius;
                    float coverage = feather == 0 ? float(distance <= 1) : 1 - smoothStep((distance - (1-feather)) / feather);
                    stroke[y*w+x] = std::max(stroke[y*w+x], coverage);
                }
            }
            for (size_t i = 0; i < brush.size(); ++i)
                brush[i] = path.erase ? brush[i] * (1-stroke[i]) : std::max(brush[i], stroke[i]);
        }
    }

    float coverage(float x, float y, float aspect) const {
        float value = 0;
        if (kind == 0 && !brush.empty()) {
            float px = std::clamp(x * width - .5f, 0.f, float(width-1));
            float py = std::clamp(y * height - .5f, 0.f, float(height-1));
            int a = int(px), b = int(py), c = std::min(a+1,width-1), d = std::min(b+1,height-1);
            value = (brush[b*width+a]*(1-(px-a)) + brush[b*width+c]*(px-a))*(1-(py-b)) +
                    (brush[d*width+a]*(1-(px-a)) + brush[d*width+c]*(px-a))*(py-b);
        } else if (kind == 1) {
            float dx = (end.x-start.x)*aspect, dy = end.y-start.y;
            float length = dx*dx + dy*dy;
            float t = length < .000001f ? .5f : ((x-start.x)*aspect*dx + (y-start.y)*dy) / length;
            float transition = std::max(.01f, feather);
            value = 1 - smoothStep((t - (1-transition)/2) / transition);
        } else if (kind == 2) {
            float dx = (x-start.x) / std::max(.01f, std::abs(end.x-start.x));
            float dy = (y-start.y) / std::max(.01f, std::abs(end.y-start.y));
            float distance = std::hypot(dx,dy);
            value = feather == 0 ? float(distance <= 1) : 1 - smoothStep((distance - (1-feather)) / feather);
        }
        return inverted ? 1-value : value;
    }
};

inline LocalEdits localEdits(const std::vector<LocalMask>& masks, float x, float y, float aspect) {
    LocalEdits result;
    for (const auto& mask : masks) {
        if (!mask.enabled || !mask.hasEdits() || mask.opacity == 0) continue;
        float weight = mask.coverage(x,y,aspect) * mask.opacity;
        result.exposure += mask.edits.exposure * weight;
        result.shadows += mask.edits.shadows * weight;
        result.highlights += mask.edits.highlights * weight;
        result.temperature += mask.edits.temperature * weight;
        result.tint += mask.edits.tint * weight;
        result.contrast += mask.edits.contrast * weight;
        result.saturation += mask.edits.saturation * weight;
    }
    return result;
}

struct CreativeReader {
    const float* data; int length, position = 39;
    float next(float low = 0, float high = 1) {
        if (position >= length) throw std::runtime_error("Incomplete creative adjustments.");
        float value = data[position++];
        if (!std::isfinite(value) || value < low || value > high) throw std::runtime_error("Invalid creative adjustment.");
        return value;
    }
    int count(int maximum) {
        float n = next(0, maximum);
        if (n != std::round(n)) throw std::runtime_error("Invalid creative adjustment count.");
        return int(n);
    }
    EditPoint point() { return {next(), next()}; }
    void read(std::array<ToneCurve,4>& curves, std::vector<LocalMask>& masks) {
        if (next() != 1) throw std::runtime_error("Unsupported creative adjustment version.");
        for (auto& curve : curves) {
            int n = count(16);
            if (n < 2) throw std::runtime_error("A curve needs endpoints.");
            for (int i = 0; i < n; ++i) curve.points.push_back(point());
            if (curve.points.front().x != 0 || curve.points.back().x != 1) throw std::runtime_error("Invalid curve endpoints.");
            for (int i = 1; i < n; ++i) if (curve.points[i].x - curve.points[i-1].x < .00999f)
                throw std::runtime_error("Curve points are too close.");
            curve.prepare();
        }
        int n = count(8), totalPoints = 0;
        for (int i = 0; i < n; ++i) {
            LocalMask m;
            m.kind = count(2); m.enabled = count(1); m.inverted = count(1);
            m.opacity = next(); m.feather = next(); m.start = point(); m.end = point();
            m.edits = {next(-3,3), next(-1,1), next(-1,1), next(-1,1), next(-1,1), next(-1,1), next(-1,1)};
            int strokes = count(64);
            for (int j = 0; j < strokes; ++j) {
                BrushStroke s;
                s.radius = next(.0005f,1); s.erase = count(1);
                int points = count(256); totalPoints += points;
                if (points == 0 || totalPoints > 4096) throw std::runtime_error("Brush mask is too large.");
                for (int k = 0; k < points; ++k) s.points.push_back(point());
                m.strokes.push_back(std::move(s));
            }
            masks.push_back(std::move(m));
        }
        if (position != length) throw std::runtime_error("Unexpected creative adjustment data.");
    }
};

inline float grainNoise(int x, int y, uint32_t seed) {
    uint32_t value = uint32_t(x)*0x9e3779b9U ^ uint32_t(y)*0x85ebca6bU ^ seed;
    value ^= value >> 16; value *= 0x7feb352dU; value ^= value >> 15; value *= 0x846ca68bU; value ^= value >> 16;
    return (value & 0xffffffU) / 8388607.5f - 1;
}
inline float grainField(float x, float y, uint32_t seed) {
    int ix = int(std::floor(x)), iy = int(std::floor(y));
    float fx = smoothStep(x-ix), fy = smoothStep(y-iy);
    float a = grainNoise(ix,iy,seed)*(1-fx) + grainNoise(ix+1,iy,seed)*fx;
    float b = grainNoise(ix,iy+1,seed)*(1-fx) + grainNoise(ix+1,iy+1,seed)*fx;
    return a*(1-fy) + b*fy;
}
inline Rgb addGrain(Rgb pixel, int x, int y, int width, int height, int fullWidth, int fullHeight,
                    float amount, float size, float roughness) {
    if (amount == 0) return pixel;
    int samples = fullWidth > width*1.5f || fullHeight > height*1.5f ? 2 : 1;
    float noise = 0;
    for (int sy = 0; sy < samples; ++sy) for (int sx = 0; sx < samples; ++sx) {
        float px = (x+(sx+.5f)/samples)*fullWidth/width/size;
        float py = (y+(sy+.5f)/samples)*fullHeight/height/size;
        float coarse = grainField(px,py,0x243f6a88U), fine = grainField(px*2,py*2,0x85a308d3U);
        noise += coarse*(1-roughness*.5f) + fine*roughness;
    }
    float luma = .2126f*pixel[0] + .7152f*pixel[1] + .0722f*pixel[2];
    float delta = noise/(samples*samples) * amount*.16f*std::sqrt(std::max(0.f,luma*(1-luma)));
    for (auto& c : pixel) c = std::clamp(c+delta,0.f,1.f);
    return pixel;
}
} // namespace fuji
