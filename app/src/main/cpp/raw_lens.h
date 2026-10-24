#pragma once
#include <algorithm>
#include <cmath>
#include <vector>

namespace fuji {
/** Fujifilm f00b: [scale, N source radii, N radial displacements in percent]. */
struct LensDistortion {
    std::vector<double> radii, factors;
    double referenceRadius = 0, zoom = 1;
    bool applied = false;

    void read(const std::vector<double>& values) {
        *this = {};
        const int n = int(values.size()) / 2;
        if ((n != 8 && n != 9 && n != 11) || values.size() != size_t(1+2*n) ||
            !std::all_of(values.begin(),values.end(),[](double x) { return std::isfinite(x); }) || values[0] <= 0) return;
        LensDistortion next;
        next.referenceRadius = values[0]*n;
        if (values[1] > 0) { next.radii.push_back(0); next.factors.push_back(1); }
        for (int i = 0; i < n; ++i) {
            double radius = values[1+i], factor = 1+values[1+n+i]/100;
            if (radius < 0 || radius > 1.5 || factor < .25 || factor > 1.75 ||
                (!next.radii.empty() && (radius <= next.radii.back() || radius/factor <= next.radii.back()/next.factors.back()))) return;
            next.radii.push_back(radius); next.factors.push_back(factor);
        }
        if (next.radii.front() != 0 || next.radii.back() < .99) return;
        *this = std::move(next);
    }

    double atSource(double radius) const {
        auto upper = std::upper_bound(radii.begin(),radii.end(),radius);
        if (upper == radii.begin()) return factors.front();
        if (upper == radii.end()) return factors.back();
        int i = int(upper-radii.begin());
        double t = (radius-radii[i-1])/(radii[i]-radii[i-1]);
        return factors[i-1]+t*(factors[i]-factors[i-1]);
    }

    void prepare(int width, int height) {
        applied = false;
        if (radii.empty()) return;
        double diagonal = std::hypot(width,height);
        // ponytail: only calibrated full-image tables; cropped/mismatched units need separate validation.
        if (std::abs(referenceRadius/(diagonal/2)-1) > .01) return;
        double firstBorder = std::min(width,height)/diagonal;
        double largest = std::max(atSource(firstBorder),atSource(1));
        for (double radius : radii) if (radius >= firstBorder && radius <= 1) largest = std::max(largest,atSource(radius));
        zoom = 1/largest;
        applied = true;
    }

    double mappingFactor(double radius) const {
        if (!applied) return 1;
        double output = radius*zoom;
        if (output <= 0) return zoom*factors.front();
        // Invert the piecewise-linear source-radius table analytically, not by a guessed polynomial.
        for (size_t i = 1; i < radii.size(); ++i) if (output <= radii[i]/factors[i]) {
            double slope = (factors[i]-factors[i-1])/(radii[i]-radii[i-1]);
            double intercept = factors[i-1]-slope*radii[i-1];
            return zoom*intercept/(1-output*slope);
        }
        return zoom*factors.back();
    }
};
}
