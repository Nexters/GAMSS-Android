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

# tflite 는 FlatBuffer 라 오프셋 4 에 "TFL3" 식별자가, onnx 는 protobuf 라 ir_version 필드(0x08)와
# producer_name 에 "onnx" 문자열이 들어갑니다. 해시 비교로는 양쪽에 똑같이 엉뚱한 파일이 올라간
# 경우를 못 걸러내므로, 최소한 모델 형식인지는 확인합니다.
assert_model_format() {
  local file="$1" label="$2" kind="${2##*.}"
  case "$kind" in
    tflite)
      if [ "$(dd if="$file" bs=1 skip=4 count=4 2>/dev/null)" != "TFL3" ]; then
        echo "실패: $label 이 tflite(FlatBuffer) 형식이 아닙니다" >&2
        exit 1
      fi
      ;;
    onnx)
      if [ "$(dd if="$file" bs=1 count=1 2>/dev/null | od -An -tx1 | tr -d ' \n')" != "08" ]; then
        echo "실패: $label 이 onnx(protobuf) 형식이 아닙니다" >&2
        exit 1
      fi
      if ! head -c 64 "$file" | grep -qa onnx; then
        echo "실패: $label 에서 onnx producer 정보를 찾지 못했습니다" >&2
        exit 1
      fi
      ;;
    *)
      echo "실패: $label 은 검증 규칙이 없는 확장자입니다" >&2
      exit 1
      ;;
  esac
}

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

  # 검증은 받은 파일에 대해 먼저 합니다. 배치한 뒤에 검사하면 실패했을 때 작업 트리의 모델이
  # 망가진 상태로 남습니다.
  assert_model_format "$TMP/$asset" "$path"
  actual="$(sha256_of "$TMP/$asset")"

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

  mkdir -p "$(dirname "$path")"
  cp "$TMP/$asset" "$path"

  printf '  %s (%s MB, 형식·oid 확인)\n' "$path" "$(( $(wc -c < "$path") / 1048576 ))"
}

install_model emotion_int8.tflite      models/emotion-pack/src/main/assets/models/emotion_int8.tflite
install_model kobart_encoder_int8.onnx models/summary-pack/src/main/assets/models/kobart_encoder_int8.onnx
install_model kobart_decoder_int8.onnx models/summary-pack/src/main/assets/models/kobart_decoder_int8.onnx

echo "완료"
