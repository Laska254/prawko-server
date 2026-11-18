#!/usr/bin/env bash
# Converts every video under DIR to WebM (VP9 + Opus) for the media directory (docs/api.md, "Media").
#
# Each <name>.webm is written next to its original, which stays in place, so the directory tree is unchanged. Names
# are kept byte for byte (spaces, Polish letters, NFD accents); only the extension becomes .webm, as the question
# import expects. Files already converted are skipped, and each one is written to a .part file first, so an
# interrupted run can simply be started again. Non-video files (images) are not touched.
#
# Usage: scripts/convert-videos-to-webm.sh DIR
#   e.g. scripts/convert-videos-to-webm.sh data/multimedia
# Env:   JOBS  parallel ffmpeg processes (default: CPU count / 4)
#        CRF   VP9 quality, 0-63, lower = better and larger (default: 32)
set -euo pipefail

if [[ $# -ne 1 ]]; then
	echo "Usage: $0 DIR" >&2
	exit 2
fi
if ! command -v ffmpeg > /dev/null; then
	echo "ffmpeg not found (sudo apt install ffmpeg)" >&2
	exit 1
fi

dir=$(realpath -- "$1")
default_jobs=$(($(nproc) / 4))
if ((default_jobs < 1)); then
	default_jobs=1
fi
jobs=${JOBS:-$default_jobs}
export CRF=${CRF:-32} dir

convert() {
	local -r input=$1
	local -r relative=${input#"$dir"/}
	local -r output=${input%.*}.webm
	local -r partial=$output.part
	if [[ -e $output ]]; then
		return 0
	fi
	# first video stream, first audio stream if there is one
	if ffmpeg -nostdin -hide_banner -loglevel error -y -i "$input" \
		-map 0:v:0 -map '0:a:0?' \
		-c:v libvpx-vp9 -crf "$CRF" -b:v 0 -deadline good -cpu-used 4 -row-mt 1 -pix_fmt yuv420p \
		-c:a libopus -b:a 96k \
		-f webm "$partial"; then
		mv -- "$partial" "$output"
		echo "ok   $relative"
	else
		rm -f -- "$partial"
		echo "FAIL $relative" >&2
		return 1
	fi
}
export -f convert

echo "Converting videos in $dir ($jobs jobs, CRF $CRF)"
status=0
find "$dir" -type f \( -iname '*.wmv' -o -iname '*.avi' -o -iname '*.mp4' -o -iname '*.mov' \
	-o -iname '*.mpg' -o -iname '*.mpeg' -o -iname '*.mkv' -o -iname '*.flv' \) -print0 \
	| xargs -0 -r -n 1 -P "$jobs" bash -c 'convert "$1"' _ || status=$?
if ((status != 0)); then
	echo "Some files failed (see FAIL lines); run again to retry them." >&2
fi
exit "$status"
