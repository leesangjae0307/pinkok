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

### 4. Metro Server 실행

```bash
npm start
```

### 5. Android 실행

새 터미널에서

```bash
npx react-native run-android
```

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
export const BASE_URL = "...";

export const COLORS = {
    PRIMARY: "#00C853",
};
```

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