#include "../../main/cpp/raw_color.h"
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
