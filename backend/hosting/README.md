# api.pinkok-app.com 연결 (Firebase Hosting)

Cloud Run 서울 리전(asia-northeast3)은 사용자 지정 도메인 매핑을 지원하지 않는다.
그래서 Firebase Hosting 을 앞에 두고, 들어온 요청을 전부 Cloud Run(`pinkok-backend`)으로 넘긴다.

```
앱 → https://api.pinkok-app.com (Firebase Hosting, 인증서 자동 발급·갱신)
       └─ 모든 경로 → Cloud Run pinkok-backend (asia-northeast3)
```

- `firebase.json` 의 `rewrites` 가 "모든 요청(`**`)을 Cloud Run 으로" 넘기는 설정이다.
- `public/` 은 Firebase Hosting 이 요구하는 폴더라 둔 것이다. 검색엔진이 API 주소를 수집하지 않도록 `robots.txt` 만 있다.
- 서버 코드를 바꿔 Cloud Run 을 다시 배포해도 여기는 다시 배포할 필요가 없다 (항상 최신 Cloud Run 버전으로 넘긴다).
  이 폴더의 설정을 바꿨을 때만 다시 배포한다.

## 배포

```bash
npm install -g firebase-tools
firebase login
cd backend/hosting
firebase use --add        # Cloud Run 이 있는 GCP 프로젝트 선택
firebase deploy --only hosting
```

## 제한

- 요청 하나당 최대 60초. AI 추출·PinLog 합성은 응답 후 뒤에서 도는 방식이라 영향 없다.
- 사진·영상 파일은 Cloud Storage 버킷 주소(storage.googleapis.com)로 바로 받으므로 여기를 거치지 않는다.
