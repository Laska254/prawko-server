#!/usr/bin/env bash
# Converts the question bank's WMV videos to WebM (VP9 + Opus), which Android and browsers can play.
# Writes <name>.webm next to each <name>.wmv in the given directory and keeps the originals.
# Videos already converted are skipped, so the script can be rerun after adding new files.
#
# Usage: scripts/convert-videos.sh <media directory>
set -euo pipefail

dir=${1:?usage: $0 <media directory>}
command -v ffmpeg >/dev/null || { echo "ffmpeg not found" >&2; exit 1; }

shopt -s nullglob
for src in "$dir"/*.wmv; do
    dst=${src%.wmv}.webm
    [[ -e $dst ]] && continue
    echo "$src -> $dst"
    # yuv420p (VP9 profile 0) for hardware decoders, cues_to_front puts the index first
    # so players can seek before the download ends
    ffmpeg -nostdin -loglevel error -y -i "$src" \
        -c:v libvpx-vp9 -crf 32 -b:v 0 -row-mt 1 -pix_fmt yuv420p \
        -c:a libopus -b:a 96k \
        -cues_to_front 1 \
        -f webm "$dst.part"
    # rename only once complete, so an interrupted run doesn't leave a file that would be skipped
    mv "$dst.part" "$dst"
done
