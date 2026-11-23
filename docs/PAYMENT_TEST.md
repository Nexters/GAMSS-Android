# Debug Activity 라이선스 결제 테스트

테스트 Activity는 `app/src/debug`에만 있다. 구매 화면과 구매 결과 처리, Billing SDK는 앱 전체에서 공유하며 release에도 포함된다. release는 `PROD_BASE_URL`을 참조하므로 실제 운영 주소 여부는 로컬/CI 설정을 함께 확인한다.

## 라이선스 결제용 debug APK

현재 로컬 테스트를 위해 debug 패키지에 접미사를 붙이지 않는다. 아래 명령으로 Play 등록 패키지 `com.gamss.android`의 debug APK를 만든다.

```sh
./gradlew :app:assembleDebug -PversionCode=110004
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.gamss.android/com.gamss.android.app.debug.BillingDebugActivity
```

debug APK는 debug 키로 서명한다. 기존 설치 앱과 서명이 다르면 업데이트 설치할 수 없다.

- 빌드 결과는 debug APK이며 개발 API(`DEV_BASE_URL=https://dev-api.gamss.kr/`)를 사용한다.
- 버전 코드는 기기에 설치한 앱 이상으로 지정한다. 라이선스 테스트를 위해 매번 올릴 필요는 없다.
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

구매 검증은 실제 Play 상품 ID와 구매 토큰을 전송한다. 서버에서도 이 ID를 허용해야 지급까지 성공한다. 서버 상품 설정을 수정하면 `PurchaseViewModel`에서 임시 보정을 제거한다.

## 구매 처리와 확인

1. `/api/payments/products` 응답으로 Play 상품·가격을 조회한다.
2. 구매 버튼을 누르면 `/api/payments/account-token`과 새 ProductDetails를 조회하고 계정 토큰을 `setObfuscatedAccountId`에 넣어 결제창을 연다.
3. PURCHASED일 때만 `/api/payments/google`로 실제 상품 ID와 구매 토큰을 전송한다.
4. 서버가 GRANTED를 반환해야 지급 완료로 표시한다. PENDING·취소·오류를 지급 완료로 표시하지 않는다.
5. 앱 시작 후 로그인 완료, 포그라운드 복귀, 테스트 화면 진입 및 Billing 연결 성공 시 미처리 구매를 확인한다. `PendingPurchaseVerifier`가 프로세스 수명 동안 구매 결과를 수신하며, 소비·승인은 서버에서 처리한다. 지급 확정 시 토큰 사용량 갱신을 요청한다.

현재 결제 API에는 별도 유료 잔액 조회가 없다. 화면의 유료 잔액은 구매 검증 응답 시점의 `paidBalance`이며, 이후 사용이나 다른 기기의 구매까지 반영한 최신 잔액을 보장하지 않는다. 사용량은 기존 사용량 API로 별도 갱신한다.

결제 콜백 오류와 복구 조회 오류는 출처와 SDK 상세 메시지를 구분해 표시한다. 다음 Activity 복귀에서 이전 SDK 오류를 초기화하며, 오류 표시가 사라졌다는 것만으로 지급 성공으로 판단하지 않는다.

필요할 때만 `adb shell setprop log.tag.GamssBilling DEBUG`로 SDK 요청 진단 로그를 켤 수 있다. 상품·offer 일치 여부, 계정 토큰 길이·UUID 형식, 메인 스레드·연결 상태, SDK 반환값만 기록하며 토큰 원문은 기록하지 않는다. 확인 후 `adb shell setprop log.tag.GamssBilling ""`로 해제한다.

단위 테스트는 요청·응답 매핑, PENDING, 중복 콜백, 재검증, 로그아웃 차단과 오류 출처를 확인한다. 실제 결제·서버 지급·중복 지급 방지는 실기기에서 확인한다.
