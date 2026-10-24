#include "../../main/cpp/raw_color.h"
#include "../../main/cpp/raw_detail.h"
#include "../../main/cpp/raw_frame.h"
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

    constexpr int width = 32, height = 256;
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
    edits.clarity = .6f;
    edits.texture = -.4f;
    edits.dehaze = .5f;
    edits.defringePurple = .8f;
    edits.defringeGreen = .7f;
    auto fringeScene = noisy;
    for (int y = 0; y < height; ++y) fringeScene[y * width + 16] = y % 2 ? fuji::Rgb{.7f,.25f,.7f} : fuji::Rgb{.2f,.65f,.2f};
    auto whole = fringeScene;
    fuji::applyDetails(whole, width, height, edits, 1);
    int halo = fuji::detailHalo(edits, 1);
    for (int top = 0; top < height; top += 7) {
        int bottom = std::min(top + 7, height);
        int first = std::max(0, top - halo), last = std::min(height, bottom + halo);
        std::vector<fuji::Rgb> strip(fringeScene.begin() + first * width, fringeScene.begin() + last * width);
        fuji::applyDetails(strip, width, last - first, edits, 1);
        for (int y = top; y < bottom; ++y) for (int x = 0; x < width; ++x)
            for (int c = 0; c < 3; ++c) assert(std::abs(strip[(y - first) * width + x][c] - whole[y * width + x][c]) < .00001f);
    }
    for (bool texture : {false, true}) {
        auto stronger = noisy, softer = noisy;
        fuji::localContrast(stronger, width, height, .7f, 1, texture);
        fuji::localContrast(softer, width, height, -.7f, 1, texture);
        assert(stronger[10 * width + 16][0] > noisy[10 * width + 16][0]);
        assert(softer[10 * width + 16][0] < noisy[10 * width + 16][0]);
        std::vector<fuji::Rgb> flat(width * height, {.5f,.5f,.5f});
        fuji::localContrast(flat, width, height, 1, 1, texture);
        for (const auto& pixel : flat) assert(std::abs(pixel[0] - .5f) < .00001f);
    }
    auto haze = noisy, clear = noisy;
    fuji::dehaze(haze, width, height, -1, 1, {1,1,1});
    fuji::dehaze(clear, width, height, 1, 1, {1,1,1});
    assert(haze[0][0] > noisy[0][0] && clear[0][0] < noisy[0][0]);
    for (const auto& image : {whole, haze, clear}) for (const auto& pixel : image)
        for (float c : pixel) assert(std::isfinite(c) && c >= 0 && c <= 1);

    for (auto fringe : {fuji::Rgb{.7f,.25f,.7f}, fuji::Rgb{.2f,.65f,.2f}}) {
        std::vector<fuji::Rgb> edge(81);
        for (int y = 0; y < 9; ++y) for (int x = 0; x < 9; ++x) {
            float gray = x < 4 ? .1f : .9f;
            edge[y * 9 + x] = x == 4 ? fringe : fuji::Rgb{gray,gray,gray};
        }
        edits = {};
        edits.defringePurple = edits.defringeGreen = 1;
        fuji::defringe(edge, 9, 9, edits, 1);
        auto corrected = edge[4 * 9 + 4];
        assert(std::abs(corrected[0] - corrected[1]) < .00001f && std::abs(corrected[1] - corrected[2]) < .00001f);
        assert(std::abs(fuji::luminance(corrected) - fuji::luminance(fringe)) < .00001f);
        std::vector<fuji::Rgb> flat(81, fringe);
        fuji::defringe(flat, 9, 9, edits, 1);
        for (const auto& pixel : flat) assert(pixel == fringe);
    }

    edits = {};
    fuji::Frame originalFrame(120, 80, edits);
    assert(originalFrame.width == 120 && originalFrame.height == 80);
    for (int y = 0; y < 80; ++y) for (int x = 0; x < 120; ++x)
        assert(originalFrame.source(x, y) == std::make_pair(float(x), float(y)));
    edits.quarterTurns = 1;
    fuji::Frame clockwise(120, 80, edits);
    assert(clockwise.width == 80 && clockwise.height == 120);
    assert(clockwise.source(0, 0) == std::make_pair(0.f, 79.f));
    edits.quarterTurns = 2;
    assert(fuji::Frame(120, 80, edits).source(0, 0) == std::make_pair(119.f, 79.f));
    edits.quarterTurns = 3;
    assert(fuji::Frame(120, 80, edits).source(0, 0) == std::make_pair(119.f, 0.f));
    edits = {};
    edits.cropLeft = .25f; edits.cropTop = .125f; edits.cropRight = .75f; edits.cropBottom = .875f;
    fuji::Frame cropped(120, 80, edits);
    assert(cropped.width == 60 && cropped.height == 60);
    assert(cropped.source(0, 0) == std::make_pair(30.f, 10.f));
    assert(cropped.source(59, 59) == std::make_pair(89.f, 69.f));
    edits.straighten = 15;
    fuji::Frame straightened(120, 80, edits);
    assert(straightened.width < cropped.width && straightened.height < cropped.height);
    auto [sx, sy] = straightened.source(straightened.width / 2, straightened.height / 2);
    assert(std::abs(sx - 60) < 2 && std::abs(sy - 40) < 2);
    // Presets use the full-resolution upright canvas, not the rounded 1600 px preview.
    for (int turns = 0; turns < 4; ++turns) for (float angle : {-15.f, -3.5f, 0.f, 3.5f, 15.f}) {
        edits = {};
        edits.quarterTurns = turns;
        edits.straighten = angle;
        fuji::Frame uncropped(7728, 5149, edits);
        float aspect = float(uncropped.width) / uncropped.height;
        float w = std::min(1.f, 1 / aspect), h = w * aspect;
        edits.cropLeft = (1 - w) / 2; edits.cropRight = (1 + w) / 2;
        edits.cropTop = (1 - h) / 2; edits.cropBottom = (1 + h) / 2;
        fuji::Frame square(7728, 5149, edits);
        assert(std::abs(square.width - square.height) <= 1);
    }
    edits = {};
    assert(fuji::vignette({.5f,.5f,.5f}, 0, 0, 101, 101, edits)[0] == .5f);
    edits.vignette = -1;
    assert(fuji::vignette({.5f,.5f,.5f}, 50, 50, 101, 101, edits)[0] == .5f);
    assert(fuji::vignette({.5f,.5f,.5f}, 0, 0, 101, 101, edits)[0] < .5f);
    edits.vignette = 1;
    assert(fuji::vignette({.5f,.5f,.5f}, 0, 0, 101, 101, edits)[0] > .5f);
    edits.vignetteMidpoint = .2f;
    auto vignette = fuji::vignette({.5f,.5f,.5f}, 15, 20, 150, 100, edits)[0];
    edits.vignetteMidpoint = .8f;
    assert(vignette != fuji::vignette({.5f,.5f,.5f}, 15, 20, 150, 100, edits)[0]);
    edits.vignette = -.5f;
    edits.vignetteMidpoint = .5f;
    vignette = fuji::vignette({.5f,.5f,.5f}, 15, 20, 150, 100, edits)[0];
    edits.vignetteRoundness = 1;
    assert(vignette != fuji::vignette({.5f,.5f,.5f}, 15, 20, 150, 100, edits)[0]);
    edits.vignetteRoundness = 0;
    edits.vignetteFeather = 1;
    assert(vignette != fuji::vignette({.5f,.5f,.5f}, 15, 20, 150, 100, edits)[0]);

    // Every supported correction/rotation combination stays within the image before clamping.
    for (int turns = 0; turns < 4; ++turns) for (float angle : {-15.f, 0.f, 15.f})
    for (float distortion : {-1.f, 0.f, 1.f}) for (float vertical : {-1.f, 0.f, 1.f}) for (float horizontal : {-1.f, 0.f, 1.f}) {
        edits = {};
        edits.quarterTurns = turns; edits.straighten = angle; edits.distortion = distortion;
        edits.perspectiveVertical = vertical; edits.perspectiveHorizontal = horizontal;
        edits.chromaticRed = 1; edits.chromaticBlue = -1;
        fuji::Frame corrected(7728, 5149, edits);
        for (int y = 0; y <= 8; ++y) for (int x = 0; x <= 8; ++x) for (int channel = 0; channel < 3; ++channel) {
            auto [sx, sy] = corrected.unclampedSource(x * corrected.width / 8. - .5, y * corrected.height / 8. - .5, channel);
            assert(std::isfinite(sx) && std::isfinite(sy));
            assert(sx >= -.501 && sy >= -.501 && sx <= 7727.501 && sy <= 5148.501);
        }
    }
    edits = {};
    edits.perspectiveVertical = .8f;
    fuji::Frame verticalFrame(401, 301, edits);
    auto topLeft = verticalFrame.unclampedSource(0, 0), topRight = verticalFrame.unclampedSource(verticalFrame.width - 1, 0);
    auto bottomLeft = verticalFrame.unclampedSource(0, verticalFrame.height - 1), bottomRight = verticalFrame.unclampedSource(verticalFrame.width - 1, verticalFrame.height - 1);
    assert(topRight.first - topLeft.first > bottomRight.first - bottomLeft.first);
    for (float amount : {-1.f, 1.f}) {
        edits = {}; edits.distortion = amount;
        fuji::Frame curved(401, 301, edits);
        auto middle = curved.unclampedSource(curved.width * .75, (curved.height - 1) / 2.);
        auto edge = curved.unclampedSource(curved.width * .75, 0);
        assert(amount < 0 ? edge.first < middle.first : edge.first > middle.first);
    }
    edits = {}; edits.chromaticRed = 1; edits.chromaticBlue = -1;
    fuji::Frame aligned(4001, 3001, edits);
    auto colorEdge = [&](int x, int) {
        return fuji::Rgb{float(((x - 2000) / aligned.redScale + 2000) / 4000), x / 4000.f,
                         float(((x - 2000) / aligned.blueScale + 2000) / 4000)};
    };
    auto matched = fuji::sampleCamera(aligned, aligned.width * 4 / 5, aligned.height / 2, colorEdge);
    assert(std::abs(matched[0] - matched[1]) < .000001f && std::abs(matched[2] - matched[1]) < .000001f);
    auto [gx, gy] = aligned.source(aligned.width * 4 / 5, aligned.height / 2);
    auto unaligned = fuji::sampleBilinear(gx, gy, 4001, 3001, colorEdge);
    assert(std::abs(unaligned[0] - unaligned[1]) > .0001f && std::abs(unaligned[2] - unaligned[1]) > .0001f);

    float invalid[61] = {};
    invalid[37] = 3; invalid[38] = .5f; invalid[39] = 1;
    for (int i = 40; i < 60; i += 5) { invalid[i] = 2; invalid[i+3] = invalid[i+4] = 1; }
    invalid[11] = 1;
    invalid[21] = invalid[22] = 1;
    invalid[4] = std::nanf("");
    rejected = false;
    try { fuji::Adjustments::read(invalid, 61); } catch (const std::exception&) { rejected = true; }
    assert(rejected);
    invalid[4] = 0;
    assert(fuji::Adjustments::read(invalid, 61).cropRight == 1);
    for (int field : {16, 18, 19, 23, 24, 26, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 43, 60}) {
        float original = invalid[field];
        invalid[field] = field == 23 ? .5f : 99;
        rejected = false;
        try { fuji::Adjustments::read(invalid, 61); } catch (const std::exception&) { rejected = true; }
        assert(rejected);
        invalid[field] = original;
    }
    // Curves bypass identity exactly, stay inside their endpoints, and isolate RGB channels.
    fuji::ToneCurve curve;
    curve.points = {{0,0},{.3f,.1f},{.7f,.9f},{1,1}}; curve.prepare();
    float previous = 0;
    for (int i = 0; i <= 1000; ++i) {
        float value = curve.apply(i/1000.f); assert(value >= previous && value <= 1); previous = value;
    }
    assert(std::abs(curve.apply(.3f)-.1f) < .001f);
    edits = {}; edits.curves[1] = curve;
    auto redCurve = fuji::renderPixel({.02f,.02f,.02f},identity,edits,1);
    auto plain = fuji::renderPixel({.02f,.02f,.02f},identity,{},1);
    assert(redCurve[0] != plain[0] && redCurve[1] == plain[1] && redCurve[2] == plain[2]);
    fuji::ToneCurve diagonal; diagonal.points = {{0,0},{.4f,.4f},{1,1}}; diagonal.prepare();
    assert(diagonal.apply(.123456f) == .123456f);
    curve.points = {{0,.8f},{.3f,.1f},{.7f,.9f},{1,.2f}}; curve.prepare();
    for (int i = 0; i <= 1024; ++i) assert(curve.apply(i/1024.f) >= .099f && curve.apply(i/1024.f) <= .901f);

    fuji::Rgb neutral{.5f,.5f,.5f};
    assert(fuji::addGrain(neutral,3,7,100,100,100,100,0,3,.5f) == neutral);
    auto grain = fuji::addGrain(neutral,3,7,100,100,100,100,.8f,3,.5f);
    assert(grain == fuji::addGrain(neutral,3,7,100,100,100,100,.8f,3,.5f));
    assert(grain != neutral && grain[0] == grain[1] && grain[1] == grain[2]);
    assert(grain != fuji::addGrain(neutral,3,7,100,100,100,100,.8f,6,.5f));
    assert(grain != fuji::addGrain(neutral,3,7,100,100,100,100,.8f,3,1));
    for (int y = 0; y < 20; ++y) for (int x = 0; x < 20; ++x)
        for (float c : fuji::addGrain({0,.5f,1},x,y,20,20,8000,6000,1,.5f,1)) assert(std::isfinite(c) && c >= 0 && c <= 1);

    fuji::LocalMask mask; mask.kind = 1; mask.start = {0,0}; mask.end = {1,0}; mask.feather = 1;
    assert(mask.coverage(0,.5f,1.5f) == 1 && mask.coverage(1,.5f,1.5f) == 0);
    assert(std::abs(mask.coverage(.5f,.5f,1.5f)-.5f) < .00001f);
    mask.edits.exposure = 1; mask.opacity = .5f;
    assert(fuji::localEdits({mask},0,.5f,1.5f).exposure == .5f);
    auto local = fuji::localEdits({mask},1,.5f,1.5f);
    assert(local.exposure == 0 && fuji::renderPixel({.02f,.02f,.02f},identity,{},1,local) == plain);
    mask.enabled = false; assert(fuji::localEdits({mask},0,.5f,1.5f).exposure == 0); mask.enabled = true;
    mask.inverted = true; assert(mask.coverage(0,.5f,1.5f) == 0 && mask.coverage(1,.5f,1.5f) == 1);
    mask.inverted = false; mask.kind = 2; mask.start = {.5f,.5f}; mask.end = {.8f,.7f};
    assert(mask.coverage(.5f,.5f,1.5f) == 1 && mask.coverage(.9f,.9f,1.5f) == 0);
    mask.kind = 0; mask.feather = .5f;
    mask.strokes = {{.2f,false,{{.2f,.5f},{.8f,.5f}}}}; mask.prepare(120,80);
    assert(mask.coverage(.5f,.5f,1.5f) == 1 && mask.coverage(.5f,.1f,1.5f) == 0);
    mask.strokes.push_back({.08f,true,{{.5f,.5f}}}); mask.prepare(120,80);
    assert(mask.coverage(.5f,.5f,1.5f) == 0 && mask.coverage(.3f,.5f,1.5f) == 1);
    // The same source point retains coverage under crop and rotation.
    edits = {}; edits.quarterTurns = 1; edits.cropLeft = .1f; edits.cropRight = .9f;
    fuji::Frame maskedFrame(120,80,edits);
    auto sourcePoint = maskedFrame.unclampedSource(maskedFrame.width*.5-.5, maskedFrame.height*.3-.5);
    assert(mask.coverage((sourcePoint.first+.5)/120,(sourcePoint.second+.5)/80,1.5f) > .9f);
    for (int length = 0; length < 61; ++length) {
        rejected = false;
        try { fuji::Adjustments::read(invalid,length); } catch (const std::exception&) { rejected = true; }
        assert(rejected);
    }
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
