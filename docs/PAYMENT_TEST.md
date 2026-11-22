# Debug Activity 라이선스 결제 테스트

구매 화면과 ViewModel·구매 결과 처리는 `app/src/debug`에만 있다. 홈에는 구매 테스트 버튼을 두지 않는다. release는 `PROD_BASE_URL`을 참조하며 결제 테스트 Activity·화면·Billing SDK를 포함하지 않는다. 실제 운영 주소 여부는 로컬/CI의 `PROD_BASE_URL` 값을 함께 확인한다. 서버 구매 API 정의와 domain/data 코드는 유지한다.

## 라이선스 결제용 debug APK

일반 debug 패키지 `com.gamss.android.dev`는 Play에 등록되어 있지 않다. 라이선스 결제를 확인할 때는 아래 명령으로 Play 등록 패키지 `com.gamss.android`의 debug APK를 만든다. 별도 빌드 타입은 추가하지 않는다.

```sh
./gradlew :app:assembleDebug -PbillingDebug=true -PversionCode=110004
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.gamss.android/com.gamss.android.app.debug.BillingDebugActivity
```

`billingDebug=true`이면 로컬에 설정된 기존 릴리스 키가 있을 때 debug APK에도 그 키를 사용하므로, 현재 설치한 release 테스트 앱에 데이터를 유지해 업데이트할 수 있다. 서명 키 정보는 환경변수를 우선 읽고, 없으면 git에서 제외된 루트 `local.properties`의 `RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`를 읽는다. 개인 키와 비밀번호를 저장소에 추가하지 않는다.

- 빌드 결과는 debug APK이며 개발 API(`DEV_BASE_URL=https://dev-api.gamss.kr/`)를 사용한다.
- 버전 코드는 기기에 설치한 앱 이상으로 지정한다. 라이선스 테스트를 위해 매번 올릴 필요는 없다.
- 별도 실행 옵션이 없는 일반 debug는 `.dev` 패키지를 유지하며, Activity 화면은 열 수 있지만 구매는 막는다.
- 기존 로그인 세션을 복원하고, 세션이 없으면 테스트 Activity 안에서 로그인 화면을 표시한다.
- 라이선스 테스트에서 실제 기기 Google 계정이 포함된 테스터 그룹을 선택하고 저장해야 한다.
- 라이선스 테스터는 직접 설치한 APK로 테스트할 수 있다. Play 트랙 업로드나 Play 앱 서명 키와의 일치는 필수 조건이 아니다.
- Google 결제창에서 테스트 구매 표시와 테스트 결제 수단을 확인한다.

## 임시 상품 ID 보정

서버가 이전 ID를 반환하면 debug 화면에서만 아래처럼 보정한다. 지급량은 서버 응답을 유지하며, 이미 올바른 ID이면 그대로 사용한다.

| 서버의 이전 ID | Play 조회·구매 ID |
|---|---|
| `token_300000` | `token_300k` |
| `token_500000` | `token_500k` |
| `token_1000000` | `token_1m` |

구매 검증은 실제 Play 상품 ID와 구매 토큰을 전송한다. 서버에서도 이 ID를 허용해야 지급까지 성공한다. 서버 상품 설정을 수정하면 `src/debug`의 `PurchaseViewModel`에서 임시 보정을 제거한다.

## 구매 처리와 확인

1. `/api/payments/products` 응답으로 Play 상품·가격을 조회한다.
2. 구매 버튼을 누르면 `/api/payments/account-token`과 새 ProductDetails를 조회하고 계정 토큰을 `setObfuscatedAccountId`에 넣어 결제창을 연다.
3. PURCHASED일 때만 `/api/payments/google`로 실제 상품 ID와 구매 토큰을 전송한다.
4. 서버가 GRANTED를 반환해야 지급 완료로 표시한다. PENDING·취소·오류를 지급 완료로 표시하지 않는다.
5. 테스트 Activity의 로그인 완료·복귀 시 미처리 구매를 다시 조회한다. 결과 수신은 `PaymentCoordinator` 한 곳에서 담당하며, 소비·승인은 서버에서 처리한다.

결제 콜백 오류와 복구 조회 오류는 출처와 SDK 상세 메시지를 구분해 표시한다. 다음 Activity 복귀에서 이전 SDK 오류를 초기화하며, 오류 표시가 사라졌다는 것만으로 지급 성공으로 판단하지 않는다.

필요할 때만 `adb shell setprop log.tag.GamssBilling DEBUG`로 SDK 요청 진단 로그를 켤 수 있다. 상품·offer 일치 여부, 계정 토큰 길이·UUID 형식, 메인 스레드·연결 상태, SDK 반환값만 기록하며 토큰 원문은 기록하지 않는다. 확인 후 `adb shell setprop log.tag.GamssBilling ""`로 해제한다.

단위 테스트는 요청·응답 매핑, PENDING, 중복 콜백, 재검증, 로그아웃 차단과 오류 출처를 확인한다. 실제 결제·서버 지급·중복 지급 방지는 실기기에서 확인한다.
