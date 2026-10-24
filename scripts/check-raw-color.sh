#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
"${CXX:-c++}" -std=c++17 -O2 app/src/test/cpp/raw_color_test.cpp -o "$work/check"
for profile in app/src/main/assets/raw-profiles/*.gz; do
    gzip -dc "$profile" > "$work/$(basename "$profile" .gz)"
done
"$work/check" "$work"/*.cube
