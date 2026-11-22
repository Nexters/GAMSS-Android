현재 구글 플레이스토어 구매를 위해 서버 api 연동과 테스트 화면을 생성하려고 합니다
아래는 연동이 필요한 api 내역과 사양입니다. 현재 프로젝트는 멀티모듈로 정의가 되어있으며 data 모듈에 네트워크 요청 및 호출을 정의합니다. 형식은 다른 api 호출 형식을 따라갑니다
연동하려는 api는 총 4개입니다

1. Google play 구매 검증과 지급: POST /api/payments/google
   Google Play 에서 산 토큰 상품을 서버가 Google 에 직접 확인한 뒤 유료 토큰을 지급합니다.

구매할 때 setObfuscatedAccountId 에 계정 토큰(GET /api/payments/account-token)을 넣어야 합니다.
앱은 consumeAsync·acknowledgePurchase 를 부르지 않습니다. 서버가 지급한 뒤 consume 합니다.
같은 구매를 다시 보내도 추가로 지급하지 않고 같은 grantedTokens 로 성공합니다. paidBalance 는 응답 시점의 잔액이라 처음 응답과 다를 수 있습니다.
결제가 아직 끝나지 않았으면 200 에 status: PENDING 으로 응답하고 지급하지 않습니다. 지급 완료로 표시하지 말고, 결제가 끝난 뒤 다시 보냅니다.
앱을 켤 때(onResume) queryPurchasesAsync 로 남아 있는 구매를 이 API 로 다시 보냅니다. 403·409 는 바로 다시 보내도 결과가 같으니 즉시 재시도하지 않습니다. 다만 로그인한 회원이나 서버의 상품 목록이 바뀌면 지급될 수 있으므로, 다음 실행 때 다시 보내는 대상에서는 빼지 않습니다.

실패 응답 - 앱에서 처리가 필요한 에러만 취급합니다

error.code	HTTP	설명
INVALID_INPUT	400	productId·purchaseToken 누락 또는 길이 초과
PURCHASE_INVALID	400	Google 이 모르는 구매 토큰이거나 상태·상품 구성을 알 수 없는 구매
PURCHASE_PRODUCT_MISMATCH	400	구매한 상품이 요청한 productId 와 다름
UNAUTHORIZED	401	인증 필요(토큰 없음·무효)
EXPIRED_TOKEN	401	accessToken 만료 — 재발급 후 재시도
WITHDRAWN_MEMBER	403	탈퇴한 회원
PURCHASE_ACCESS_DENIED	403	구매에 계정 토큰이 없거나 다른 회원의 계정 토큰이 담김
PURCHASE_CANCELLED	409	취소되었거나 환불된 구매
PURCHASE_QUANTITY_UNSUPPORTED	409	여러 개를 한 번에 산 구매(다중 수량 미지원)
PAYMENT_PRODUCT_UNAVAILABLE	409	판매 중이 아닌 상품
PAYMENT_STORE_UNAVAILABLE	503	Google 과 통신할 수 없음 — 잠시 후 다시 보냄

Request body - 예시 입니다
{
"productId": "token_300000",
"purchaseToken": "string"
}

Responses
{
"success": true,
"data": {
"status": "GRANTED",
"productId": "token_300000",
"grantedTokens": 300000,
"paidBalance": 300000
},
"error": {
"code": "INVALID_SOCIAL_TOKEN",
"message": "유효하지 않은 소셜 토큰입니다."
}
}

2. 판매 상품 목록 조회 - GET /api/payments/products
   지금 판매하는 토큰 상품을 지급량 오름차순으로 반환합니다.

가격은 내려주지 않습니다. 각 스토어 SDK 에서 이 productId 로 현지 통화 가격을 가져옵니다.
지급량은 서버가 정합니다. 구매 검증 요청에 수량이나 금액을 보내도 쓰지 않습니다.

실패 응답

error.code	HTTP	설명
UNAUTHORIZED	401	인증 필요(토큰 없음·무효)
EXPIRED_TOKEN	401	accessToken 만료 — 재발급 후 재시도

Request body - No parameters

Responses
{
"success": true,
"data": [
{
"productId": "token_300000",
"tokenAmount": 300000
}
],
"error": {
"code": "INVALID_SOCIAL_TOKEN",
"message": "유효하지 않은 소셜 토큰입니다."
}
}

3. 구매 계정 토큰 조회 - GET /api/payments/account-token
   구매할 때 스토어에 함께 넘길 계정 토큰(UUID)을 반환합니다. 없으면 이 요청에서 만듭니다.

Apple 은 appAccountToken, Google 은 obfuscatedAccountId 에 이 값을 넣습니다. 넣지 않은 구매는 서버가 주인을 확인할 수 없어 지급하지 않습니다.
같은 회원에게는 항상 같은 값이고, 탈퇴 후 같은 소셜 계정으로 재가입해도 같습니다.
구매 직전마다 호출해도 안전합니다.


실패 응답

error.code	HTTP	설명
UNAUTHORIZED	401	인증 필요(토큰 없음·무효)
EXPIRED_TOKEN	401	accessToken 만료 — 재발급 후 재시도
WITHDRAWN_MEMBER	403	탈퇴한 회원

Request body - No parameters

Responses
{
"success": true,
"data": {
"accountToken": "3f2b8c1e-7a4d-4e6b-9c0a-1d2e3f4a5b6c"
},
"error": {
"code": "INVALID_SOCIAL_TOKEN",
"message": "유효하지 않은 소셜 토큰입니다."
}
}

## 추가적 설명
### 작업 설명(dev 스웨거 참고)

1. `GET /api/payments/products`로 상품 ID 조회 → Billing SDK로 가격 조회
2. 결제 직전 `GET /api/payments/account-token` 호출 → `setObfuscatedAccountId`에 설정
3. 결제 완료 후 `POST /api/payments/google`로 `productId`, `purchaseToken` 전송 (consume은 서버에서 처리)
4. 앱 실행 시 `queryPurchasesAsync`로 남은 구매를 3번으로 재전송

- 패키지명: `com.gamss.android` (`.dev`는 Play Console에 미등록이라 결제 불가)
- API 서버: `https://dev-api.gamss.kr`

## 화면 구성
테스트 화면을 만들어서 테스트를 진행합니다. prod 서버에서는 진행하지 못하고 play에 등록되어있는 bundle name도 release bundle name 이므로 테스트 화면은 임의의 화면이어야 합니다.
일단 임의로 홈화면에 버튼을 하나 생성해 구매 화면에 진입할 수 있도록 합니다.

