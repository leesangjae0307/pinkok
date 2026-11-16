# Firebase Hosting (도메인 연결)

Hosting 사이트 두 개를 쓴다.

```
api.pinkok-app.com  (target: api, 폴더: public/)
  └─ 모든 요청 → Cloud Run pinkok-backend (asia-northeast3)
pinkok-app.com      (target: web, 폴더: web/)
  ├─ /                 소개
  ├─ /privacy          개인정보처리방침
  └─ /delete-account   계정 삭제 안내
```

- Cloud Run 서울 리전(asia-northeast3)은 사용자 지정 도메인 매핑을 지원하지 않아서,
  `api` 사이트가 앞에서 요청을 받아 Cloud Run 으로 넘긴다. 인증서는 Firebase 가 자동 발급·갱신한다.
- `web` 사이트는 정적 페이지만 있다. 본 도메인으로 API 가 열리지 않도록 사이트를 나눴다.
- `public/` 은 Hosting 이 요구하는 폴더라 둔 것이다. 검색엔진이 API 주소를 수집하지 않도록 `robots.txt` 만 있다.
- 서버 코드를 바꿔 Cloud Run 을 다시 배포해도 Hosting 은 다시 배포할 필요가 없다 (항상 최신 Cloud Run 버전으로 넘긴다).
  이 폴더의 파일을 바꿨을 때만 다시 배포한다.

## 배포

```bash
npm install -g firebase-tools
firebase login
cd backend/hosting
firebase deploy --only hosting            # 두 사이트 모두
firebase deploy --only hosting:web        # 정책 페이지만
```

사이트 ID 와 target 의 연결은 `.firebaserc` 에 들어 있다. 처음 한 번만 아래처럼 연결한다.

```bash
firebase target:apply hosting api <api 사이트 ID>
firebase target:apply hosting web <web 사이트 ID>
```

## 제한

- 요청 하나당 최대 60초. AI 추출·PinLog 합성은 응답 후 뒤에서 도는 방식이라 영향 없다.
- 사진·영상 파일은 Cloud Storage 버킷 주소(storage.googleapis.com)로 바로 받으므로 Hosting 을 거치지 않는다.
