# 📱 PinKok Frontend

AI 기반 여행 일정 및 장소 추천 서비스 **PinKok**의 React Native 프론트엔드 프로젝트입니다.

---

# 🛠️ 개발 환경

| 항목 | 버전 |
|------|------|
| Node.js | 20.x |
| React Native | 0.79.6 |
| JDK | 17 |
| Android Studio | 최신 버전 |
| Android SDK | 35 |
| NDK | 27.1.12297006 |

---

# 🚀 프로젝트 실행

### 1. 프로젝트 클론

```bash
git clone https://github.com/leesangjae0307/pinkok.git
```

### 2. Frontend 폴더 이동

```bash
cd pinkok/frontend
```

### 3. 라이브러리 설치

```bash
npm install
```

### 4. 서버 주소 설정 (`.env`)

`.env.example` 을 복사해서 `.env` 를 만들고 값을 채웁니다. `.env` 는 커밋되지 않으므로 **각자 자기 PC 에서** 만듭니다.

```bash
cp .env.example .env
```

| 키 | 설명 |
|---|---|
| `API_BASE_URL` | 백엔드 서버 주소. **끝에 `/` 나 `/api` 를 붙이지 않습니다** (서버에 `/api` 접두사가 없어서 붙이면 전부 404). |
| `KAKAO_JS_KEY` | 카카오 개발자 콘솔 > 내 애플리케이션 > 앱 키 > **JavaScript 키**. 서버가 쓰는 REST API 키와는 다른 키이고, 그쪽은 앱에 넣지 않습니다. |
| `KAKAO_SDK_ORIGIN` | 콘솔의 [JavaScript 키 > JavaScript SDK 도메인] 에 등록한 주소 그대로. 포트까지 한 글자라도 다르면 지도가 뜨지 않습니다. |

> **서버 주소를 적는 곳은 `.env` 한 곳뿐입니다.**
> `src/api/axios.ts` 의 `BASE_URL` 은 `.env` 에 값이 없을 때만 쓰이는 폴백이라,
> `.env` 가 있으면 그쪽을 고쳐도 아무 변화가 없습니다.

#### 상황별 주소

| 상황 | 값 |
|---|---|
| 백엔드가 **다른 사람 PC** 에서 cloudflared 터널로 열려 있을 때 | 담당자에게 받은 `https://....trycloudflare.com` |
| 백엔드를 **내 PC** 에서 돌릴 때 | `adb reverse tcp:8080 tcp:8080` 을 걸고 `http://localhost:8080` |
| 에뮬레이터에서 호스트 PC 로 직접 | `http://10.0.2.2:8080` |
| 같은 와이파이의 실기기 | 서버 PC 의 LAN IP (예: `http://192.168.1.53:8080`) |

⚠️ cloudflared 임시 터널 주소는 **터널을 재시작할 때마다 바뀝니다.** 내 PC 에서 서버를 돌릴 수 있다면 주소가 고정되는 `adb reverse` 쪽이 편합니다.

#### `.env` 를 고친 뒤에는 Metro 를 다시 띄웁니다

`.env` 값은 번들을 만들 때 코드에 박히기 때문에, 파일만 고쳐서는 실행 중인 앱에 반영되지 않습니다. **이걸 빼먹는 게 연동이 안 되는 가장 흔한 원인입니다.**

```bash
npx react-native start --reset-cache
```

그리고 앱을 리로드합니다 (Metro 터미널에서 `r`, 또는 에뮬레이터에서 `R` 두 번).
Metro 로그에 `injected env (n) from .env` 가 찍히면 반영된 것입니다.

### 5. Metro Server 실행

```bash
npm start
```

### 6. Android 실행

새 터미널에서

```bash
npx react-native run-android
```

로그인에 성공하면 토큰이 기기에 저장돼서 앱을 껐다 켜도 로그인이 유지되고, 이후 요청에는 `Authorization` 헤더가 자동으로 붙습니다. 토큰이 만료되면 `/auth/refresh` 로 알아서 갱신됩니다.

---

# 🧯 서버 연동이 안 될 때

### 1. 서버가 살아 있는지 먼저 확인합니다

`/avatars` 는 로그인 없이 호출할 수 있어서 확인용으로 쓰기 좋습니다.

```bash
curl <API_BASE_URL>/avatars
```

아바타 목록이 나오면 서버는 정상입니다. 응답이 없다면 (터널이라면) 주소가 만료된 것이므로 백엔드 담당자에게 새 주소를 받습니다. 이건 프론트에서 고칠 수 없습니다.

### 2. 앱에 뜨는 메시지로 원인을 가릅니다

| 메시지 | 원인 |
|---|---|
| 서버에 연결할 수 없습니다. 서버 주소(…)가 살아 있는지 확인해 주세요. | 서버에 닿지 못함 — 주소가 틀렸거나, 터널이 죽었거나, `.env` 가 반영되지 않음 |
| 이메일 또는 비밀번호가 올바르지 않습니다. | 서버까지는 정상 도달. 계정 문제 |

### 3. 그래도 안 되면

- `.env` 의 주소 끝에 `/` 나 `/api` 가 붙어 있지 않은지 확인합니다.
- Metro 를 `--reset-cache` 로 다시 띄웠는지 확인합니다.
- `localhost` 주소를 쓰는 경우 `adb reverse --list` 에 `tcp:8080` 이 있는지 확인합니다.

---

# 📂 프로젝트 구조

```text
frontend
│
├── android/               # Android 프로젝트
├── ios/                   # iOS 프로젝트
├── node_modules/          # 설치 라이브러리(Git 제외)
│
├── src/
│   │
│   ├── api/
│   ├── assets/
│   ├── components/
│   ├── constants/
│   ├── features/
│   ├── hooks/
│   ├── navigation/
│   ├── services/
│   ├── store/
│   ├── theme/
│   ├── types/
│   └── utils/
│
├── App.tsx
├── index.js
└── package.json
```

---

# 📖 폴더 설명

## 📂 api

Spring Boot 서버와 통신하는 API를 관리하는 폴더입니다.

예시

```text
axios.ts
authApi.ts
tripApi.ts
userApi.ts
```

---

## 📂 assets

프로젝트에서 사용하는 이미지, 아이콘, 폰트 등을 저장합니다.

예시

```text
logo.png
defaultProfile.png
icons/
fonts/
```

---

## 📂 components

여러 화면에서 공통으로 사용하는 재사용 가능한 컴포넌트를 관리합니다.

예시

```text
Button
Input
Header
Modal
Loading
```

---

## 📂 constants

변하지 않는 상수들을 관리합니다.

예시

```ts
export const TRAVEL_STYLES = [...];

export const COLORS = {
    PRIMARY: "#00C853",
};
```

※ 서버 주소는 여기가 아니라 `.env` 의 `API_BASE_URL` 에 둡니다.

---

## 📂 features

기능(Feature) 단위로 화면과 로직을 관리하는 핵심 폴더입니다.

```text
features
│
├── auth
├── home
├── map
├── trip
├── diary
├── ai
└── mypage
```

### auth

- 로그인
- 회원가입
- JWT 인증

### home

- 메인 화면
- 추천 여행
- 인기 여행

### map

- 카카오맵
- 장소 검색
- 지도 핀

### trip

- 여행 일정
- 캘린더

### diary

- 여행 기록
- 후기 작성

### ai

- 유튜브 링크 분석
- 인스타그램 링크 분석
- AI 장소 추천

### mypage

- 회원정보
- 프로필
- 설정

---

### auth 예시 구조

```text
auth
│
├── screens
│   ├── LoginScreen.tsx
│   └── SignupScreen.tsx
│
├── components
│   └── LoginForm.tsx
│
├── hooks
│   └── useLogin.ts
│
├── services
│   └── authService.ts
│
└── types
    └── auth.ts
```

---

## 📂 hooks

Custom Hook을 관리합니다.

예시

```text
useAuth
useLocation
useDebounce
```

---

## 📂 navigation

앱의 화면 이동(Navigation)을 관리합니다.

예시

```text
AppNavigator
BottomTabNavigator
AuthNavigator
```

---

## 📂 services

API 외의 공통 기능을 관리합니다.

예시

```text
Storage
JWT
Permission
Location
```

---

## 📂 store

전역 상태 관리를 위한 폴더입니다.

추후 사용할 수 있는 라이브러리

```text
Redux
Zustand
Context API
```

---

## 📂 theme

앱의 디자인 시스템을 관리합니다.

예시

```text
colors.ts
fonts.ts
spacing.ts
theme.ts
```

---

## 📂 types

TypeScript 타입을 관리합니다.

예시

```text
User
Trip
Place
Response
```

---

## 📂 utils

공통으로 사용하는 유틸리티 함수입니다.

예시

```text
formatDate()
distance()
validation()
```

---

# 👨‍💻 Frontend 개발 범위

Frontend에서 담당하는 영역입니다.

- React Native UI 구현
- 화면(Navigation) 구성
- API 연동
- JWT 저장 및 관리
- 카카오맵 연동
- AI 기능 화면 구현
- 상태 관리
- 사용자 경험(UI/UX) 개선

---

# 🌱 Git 브랜치 전략

```
main
└── develop
    ├── feature/login
    ├── feature/map
    ├── feature/trip
    ├── feature/ai
    ├── feature/mypage
    └── ...
```

⚠️ **main 브랜치에서는 직접 개발하지 않습니다.**

### 작업 순서

```bash
git checkout develop
git pull origin develop

git checkout -b feature/login
```

작업 완료 후

```bash
git add .
git commit -m "feat: 로그인 화면 구현"

git push origin feature/login
```

GitHub에서 Pull Request(PR)를 생성하여 `develop` 브랜치로 병합합니다.

---

# 📌 개발 규칙

- 기능별로 `features` 폴더에서 개발합니다.
- 공통 컴포넌트는 `components`에 작성합니다.
- API 요청은 `api` 폴더에서 관리합니다.
- 디자인 관련 코드는 `theme`에서 관리합니다.
- 공통 함수는 `utils`에 작성합니다.
- 타입은 `types`에서 관리합니다.