"""Offline research only: inspect private X-T50 captures without camera I/O.

Run with --check for a synthetic self-check, or pass a private capture directory.
Reports counts only; never prints backup contents, identities or fingerprints.
The firmware checksum is established; earlier candidate exclusions remain rejected.
Checksum coverage alone does not establish which differences are safe to ignore.
"""
import argparse
import json
from pathlib import Path

SIZE = 70524
MAGIC = b"FUJIFILMX-BACKUP"


def firmware_checksum(raw):
    # X-T50 1.32 settings validator: payload +0..0xe968 and +0xf6d4..0x112d4,
    # with the u32 checksum at payload +0x60 cleared. No fitted bias or sum16.
    return sum(raw[168:264]) + sum(raw[268:59920]) + sum(raw[63356:SIZE])


def analyze(backups):
    variants = {}
    short_biases, broad_biases = set(), set()
    unique = set()
    count = checksum_matches = 0
    for raw in backups:
        if (len(raw) != SIZE or raw[:16] != MAGIC or raw[16:20] != b"0100" or raw[20:25] != b"X-T50"
                or int.from_bytes(raw[152:156], "little") != 168
                or int.from_bytes(raw[160:164], "little") != SIZE - 168):
            raise ValueError("Unsupported X-T50 backup size, header or payload window")
        count += 1
        unique.add(raw)
        other_bytes = raw[:63170] + raw[63174:]
        variants.setdefault(other_bytes, set()).add(raw[63170:63174])
        stored = int.from_bytes(raw[264:268], "little")
        checksum_matches += stored == firmware_checksum(raw)
        # Adapted X-T5 hypothesis: skip our checksum word and an 11-byte state block.
        # Count required biases instead of fitting one and calling it a checksum.
        short_sum = sum(raw[168:264]) + sum(raw[268:63164]) + sum(raw[63175:])
        short_biases.add((stored - short_sum) & 0xffff)
        # Even a broader, deliberately unjustified tail exclusion must face holdouts.
        broad_sum = sum(raw[168:264]) + sum(raw[268:63164]) + sum(raw[63356:])
        broad_biases.add((stored - broad_sum) & 0xffff)
    if not count:
        raise ValueError("No backup captures found; absent evidence is not a passing check")
    return {
        "backup_files": count,
        "unique_backups": len(unique),
        "firmware_checksum_matches": checksum_matches,
        "firmware_checksum_mismatches": count - checksum_matches,
        "groups_with_identical_other_bytes_and_different_unknown_bytes": sum(
            len(values) > 1 for values in variants.values()
        ),
        "required_biases_with_11_byte_exclusion": len(short_biases),
        "required_biases_with_192_byte_exclusion": len(broad_biases),
    }


def check():
    raw = bytearray(SIZE)
    for start, value in ((0, MAGIC + b"0100"), (20, b"X-T50"),
                         (152, (168).to_bytes(4, "little")),
                         (160, (SIZE - 168).to_bytes(4, "little"))):
        raw[start:start + len(value)] = value
    original = bytes(raw)
    raw[63170] = 1
    changed_unknown = bytes(raw)
    report = analyze([original, original, changed_unknown])
    assert report["backup_files"] == 3 and report["unique_backups"] == 2
    assert report["groups_with_identical_other_bytes_and_different_unknown_bytes"] == 1
    assert report["required_biases_with_11_byte_exclusion"] == 1
    assert report["firmware_checksum_matches"] == 3
    raw[63268] = 1
    report = analyze([original, bytes(raw)])
    assert report["groups_with_identical_other_bytes_and_different_unknown_bytes"] == 0
    assert report["required_biases_with_11_byte_exclusion"] == 2
    assert report["required_biases_with_192_byte_exclusion"] == 1
    raw[1164] = 1
    assert analyze([original, bytes(raw)])["required_biases_with_192_byte_exclusion"] == 2
    assert analyze([original, bytes(raw)])["firmware_checksum_mismatches"] == 1
    for offset in (168, 263, 264, 265, 266, 267, 268, 59919, 59920, 63355, 63356, SIZE - 1):
        changed = bytearray(original)
        changed[offset] ^= 1
        assert analyze([bytes(changed)])["firmware_checksum_matches"] == (59920 <= offset < 63356)
    for invalid in ([], [original[:-1]], [b"!" + original[1:]],
                    [original[:16] + b"9999" + original[20:]],
                    [original[:152] + bytes(4) + original[156:]],
                    [original[:160] + bytes(4) + original[164:]]):
        try:
            analyze(invalid)
        except ValueError:
            continue
        raise AssertionError("Missing or malformed evidence was accepted")
    print("Offline integrity self-check passed")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", nargs="?", type=Path,
                        default=Path(__file__).resolve().parents[1] / "captures/bluetooth")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.check:
        check()
    else:
        captures = (path.read_bytes() for path in sorted(args.directory.rglob("*.dat")))
        report = analyze(raw for raw in captures if raw.startswith(MAGIC))
        print(json.dumps(report, indent=2))
        if report["firmware_checksum_mismatches"]:
            raise SystemExit("Firmware checksum mismatches found")
