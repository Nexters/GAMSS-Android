#!/usr/bin/env bash
#
# 온디바이스 모델을 GitHub Release 자산에서 받아 애셋팩 모듈에 배치합니다.
#
# 레포에는 같은 파일이 Git LFS 로도 들어 있습니다. 다만 CI/CD 가 매 실행마다 LFS 에서 받으면
# 레포의 LFS 대역폭 예산이 소진되므로(CI 1회 542MB, CD 1회 542MB) 이 경로를 씁니다. Release 자산
# 다운로드는 LFS 할당량과 무관합니다.
#
# 모델을 교체할 때는 LFS 와 아래 태그의 Release 를 함께 갱신해야 합니다.
#
# 사용법:
#   ./scripts/fetch-models.sh              # 기본 태그(models-v1)
#   MODELS_RELEASE_TAG=models-v2 ./scripts/fetch-models.sh
#
# 필요 조건: gh CLI 인증(로컬) 또는 GH_TOKEN 환경변수(CI).

set -euo pipefail

TAG="${MODELS_RELEASE_TAG:-models-v1}"

# 포인터(약 130바이트)가 남아 있으면 빌드는 통과하고 런타임에만 실패해 발견이 늦습니다.
MIN_BYTES=1000000

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI 가 필요합니다. https://cli.github.com 참고" >&2
  exit 1
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "온디바이스 모델을 받습니다 (release: $TAG)"
gh release download "$TAG" --repo Nexters/GAMSS-Android --dir "$TMP" --clobber

install_model() {
  local asset="$1" dest="$REPO_ROOT/$2" size
  [ -f "$TMP/$asset" ] || { echo "Release 자산에 $asset 이 없습니다" >&2; exit 1; }

  mkdir -p "$(dirname "$dest")"
  cp "$TMP/$asset" "$dest"

  size=$(wc -c < "$dest" | tr -d ' ')
  if [ "$size" -lt "$MIN_BYTES" ]; then
    echo "$2 가 실제 모델이 아닙니다 (${size} bytes)" >&2
    exit 1
  fi
  printf '  %s (%s MB)\n' "$2" "$((size / 1048576))"
}

install_model emotion_int8.tflite      models/emotion-pack/src/main/assets/models/emotion_int8.tflite
install_model kobart_encoder_int8.onnx models/summary-pack/src/main/assets/models/kobart_encoder_int8.onnx
install_model kobart_decoder_int8.onnx models/summary-pack/src/main/assets/models/kobart_decoder_int8.onnx

echo "완료"
