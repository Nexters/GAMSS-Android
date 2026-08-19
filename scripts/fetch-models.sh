#!/usr/bin/env bash
#
# 온디바이스 모델을 GitHub Release 자산에서 받아 애셋팩 모듈에 배치합니다.
#
# 레포에는 같은 파일이 Git LFS 로도 들어 있습니다. 다만 CI/CD 가 매 실행마다 LFS 에서 받으면
# 레포의 LFS 대역폭 예산이 소진되므로(CI 1회 542MB, CD 1회 542MB) 이 경로를 씁니다. Release 자산
# 다운로드는 LFS 할당량과 무관합니다.
#
# 모델을 교체할 때는 LFS 와 아래 태그의 Release 를 함께 갱신해야 합니다. 한쪽만 갱신하면 이
# 스크립트가 LFS 포인터의 OID 와 받은 파일의 sha256 이 다른 것을 보고 실패합니다.
#
# 사용법:
#   ./scripts/fetch-models.sh              # 기본 태그(models-v1)
#   MODELS_RELEASE_TAG=models-v2 ./scripts/fetch-models.sh
#
# 필요 조건: gh CLI 인증(로컬) 또는 GH_TOKEN 환경변수(CI).

set -euo pipefail

TAG="${MODELS_RELEASE_TAG:-models-v1}"
REPO="Nexters/GAMSS-Android"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI 가 필요합니다. https://cli.github.com 참고" >&2
  exit 1
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

sha256_of() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d' ' -f1
  else
    shasum -a 256 "$1" | cut -d' ' -f1
  fi
}

echo "온디바이스 모델을 받습니다 (repo: $REPO, release: $TAG)"
gh release download "$TAG" --repo "$REPO" --dir "$TMP" --clobber

# LFS 포인터가 기대하는 OID 목록. 여기에 없는 경로면 추적 설정이 바뀐 것이므로 그것도 실패로 본다.
LFS_OIDS="$(git lfs ls-files -l)"

install_model() {
  local asset="$1" path="$2" expected actual

  [ -f "$TMP/$asset" ] || {
    echo "실패: Release($TAG) 자산에 $asset 이 없습니다" >&2
    exit 1
  }

  expected="$(printf '%s\n' "$LFS_OIDS" | awk -v p="$path" '$3 == p { print $1 }')"
  [ -n "$expected" ] || {
    echo "실패: $path 가 LFS 추적 대상이 아닙니다. .gitattributes 를 확인하세요" >&2
    exit 1
  }

  mkdir -p "$(dirname "$path")"
  cp "$TMP/$asset" "$path"
  actual="$(sha256_of "$path")"

  # LFS 와 Release 중 한쪽만 갱신되면 여기서 걸립니다. 그대로 두면 옛 모델로 빌드된 APK 가
  # 조용히 배포되고, 기능은 런타임에야 실패합니다.
  if [ "$expected" != "$actual" ]; then
    cat >&2 <<EOF
실패: $path 가 LFS 포인터와 다릅니다.
  LFS 기대값 : $expected
  Release 실제: $actual
모델을 교체했다면 LFS 와 Release($TAG) 를 함께 갱신해야 합니다.
새 태그로 올렸다면 MODELS_RELEASE_TAG 로 지정하세요.
EOF
    exit 1
  fi

  printf '  %s (%s MB, oid 일치)\n' "$path" "$(( $(wc -c < "$path") / 1048576 ))"
}

install_model emotion_int8.tflite      models/emotion-pack/src/main/assets/models/emotion_int8.tflite
install_model kobart_encoder_int8.onnx models/summary-pack/src/main/assets/models/kobart_encoder_int8.onnx
install_model kobart_decoder_int8.onnx models/summary-pack/src/main/assets/models/kobart_decoder_int8.onnx

echo "완료"
