#include "../../main/cpp/raw_color.h"
#include "../../main/cpp/raw_detail.h"
#include <cassert>
#include <fstream>
#include <iostream>

int main(int argc, char** argv) {
    fuji::Lut identity("LUT_3D_SIZE 2\n0 0 0\n1 0 0\n0 1 0\n1 1 0\n0 0 1\n1 0 1\n0 1 1\n1 1 1\n");
    auto mapped = identity.apply({.2f, .4f, .7f});
    assert(std::abs(mapped[0] - .2f) < .0001f && std::abs(mapped[2] - .7f) < .0001f);
    assert(identity.apply({-1, 2, 1}) == (fuji::Rgb{0, 1, 1}));
    bool rejected = false;
    try { fuji::Lut broken("LUT_3D_SIZE 2\n0 0 0\n"); }
    catch (const std::exception&) { rejected = true; }
    assert(rejected);
    auto baseline = fuji::renderPixel({.01f, .02f, .03f}, identity, {}, 1);
    auto lifted = fuji::renderPixel({.01f, .02f, .03f}, identity, {0, 1, 0}, 1);
    auto exposed = fuji::renderPixel({.01f, .02f, .03f}, identity, {1, 0, 0}, 1);
    assert(lifted[1] > baseline[1] && exposed[1] > baseline[1]);
    auto highlight = fuji::renderPixel({.9f, .9f, .9f}, identity, {0, 0, -1}, 1);
    assert(highlight[0] < fuji::srgb(.9f));
    fuji::Adjustments edits;
    assert(fuji::whiteBalanceGains(edits) == (fuji::Rgb{1, 1, 1}));
    edits.temperature = 1;
    auto warm = fuji::whiteBalanceGains(edits);
    assert(warm[0] > 1 && warm[2] < 1);
    edits = {};
    edits.tint = 1;
    assert(fuji::whiteBalanceGains(edits)[1] < 1);
    edits = {};
    edits.contrast = .5f;
    auto contrast = fuji::finishColor({.25f, .5f, .75f}, edits);
    assert(contrast[0] < .25f && contrast[1] == .5f && contrast[2] > .75f);
    edits = {};
    edits.blacks = .5f;
    assert(fuji::finishColor({0, 0, 0}, edits)[0] > 0);
    edits = {};
    edits.whites = -.5f;
    assert(fuji::finishColor({1, 1, 1}, edits)[0] < 1);
    edits = {};
    edits.saturation = -1;
    auto gray = fuji::finishColor({.2f, .4f, .8f}, edits);
    assert(gray[0] == gray[1] && gray[1] == gray[2]);
    edits = {};
    edits.vibrance = .5f;
    auto muted = fuji::finishColor({.45f, .5f, .55f}, edits);
    auto vivid = fuji::finishColor({.2f, .5f, .8f}, edits);
    assert((muted[2] - muted[0]) / .1f > (vivid[2] - vivid[0]) / .6f);

    constexpr int width = 32, height = 40;
    std::vector<fuji::Rgb> noisy(width * height);
    for (int y = 0; y < height; ++y) for (int x = 0; x < width; ++x) {
        float level = (x < width / 2 ? .25f : .75f) + ((x + y) % 2 ? .025f : -.025f);
        noisy[y * width + x] = {level, level, level};
    }
    auto unchanged = noisy;
    fuji::applyDetails(unchanged, width, height, {}, 1);
    assert(unchanged == noisy);
    edits = {};
    edits.luminanceNoise = 1;
    edits.noiseDetail = 0;
    auto smooth = noisy;
    fuji::applyDetails(smooth, width, height, edits, 1);
    assert(std::abs(smooth[10 * width + 8][0] - .25f) < .025f);
    assert(smooth[10 * width + 15][0] < .3f && smooth[10 * width + 16][0] > .7f);
    auto detailed = noisy;
    edits.noiseDetail = 1;
    fuji::applyDetails(detailed, width, height, edits, 1);
    assert(std::abs(detailed[10 * width + 8][0] - .25f) > std::abs(smooth[10 * width + 8][0] - .25f));
    std::vector<fuji::Rgb> chroma(width * height);
    for (size_t i = 0; i < chroma.size(); ++i) {
        float noise = i % 2 ? .04f : -.04f;
        chroma[i] = {.5f + noise, .5f - noise * .2126f / .7152f, .5f};
    }
    edits = {};
    edits.colorNoise = 1;
    fuji::applyDetails(chroma, width, height, edits, 1);
    assert(std::abs(chroma[10 * width + 8][0] - .5f) < .04f);
    for (const auto& pixel : chroma) assert(std::abs(fuji::luminance(pixel) - .5f) < .00001f);
    edits = {};
    edits.sharpening = 1;
    auto sharp = noisy;
    fuji::applyDetails(sharp, width, height, edits, 1);
    assert(sharp[10 * width + 15][0] < noisy[10 * width + 15][0]);
    assert(sharp[10 * width + 16][0] > noisy[10 * width + 16][0]);
    edits.sharpenThreshold = 1;
    sharp = noisy;
    fuji::applyDetails(sharp, width, height, edits, 1);
    assert(sharp[10 * width + 8] == noisy[10 * width + 8]);

    // Filtering the whole image and overlapping strips must agree at every seam.
    edits.colorNoise = .7f;
    edits.luminanceNoise = .6f;
    edits.sharpenRadius = 1.8f;
    auto whole = noisy;
    fuji::applyDetails(whole, width, height, edits, 1);
    int halo = fuji::detailHalo(edits, 1);
    for (int top = 0; top < height; top += 7) {
        int bottom = std::min(top + 7, height);
        int first = std::max(0, top - halo), last = std::min(height, bottom + halo);
        std::vector<fuji::Rgb> strip(noisy.begin() + first * width, noisy.begin() + last * width);
        fuji::applyDetails(strip, width, last - first, edits, 1);
        for (int y = top; y < bottom; ++y) for (int x = 0; x < width; ++x)
            for (int c = 0; c < 3; ++c) assert(std::abs(strip[(y - first) * width + x][c] - whole[y * width + x][c]) < .00001f);
    }
    float invalid[16] = {};
    invalid[11] = 1;
    invalid[4] = std::nanf("");
    rejected = false;
    try { fuji::Adjustments::read(invalid, 16); } catch (const std::exception&) { rejected = true; }
    assert(rejected);
    for (int i = 1; i < argc; ++i) {
        std::ifstream input(argv[i]);
        std::stringstream text;
        text << input.rdbuf();
        fuji::Lut profile(text.str());
        auto result = profile.apply({.3f, .5f, .7f});
        assert(profile.size == 32 && result != (fuji::Rgb{.3f, .5f, .7f}));
    }
    std::cout << "RAW color checks passed\n";
}
