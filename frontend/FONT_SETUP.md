# 폰트 적용하기 (Plus Jakarta Sans + Press Start 2P)

웹 디자인(`Travel_App_UI_Design`)과 같은 폰트 두 종류를 `src/assets/fonts/`에 넣어뒀어요.
둘 다 SIL OFL 1.1 라이선스로 상업적 이용이 무료예요.

| 용도 | 폰트 | 파일 |
| --- | --- | --- |
| 본문·버튼·제목 전부 (웹의 기본 폰트) | **Plus Jakarta Sans** | `PlusJakartaSans-Medium/SemiBold/Bold/ExtraBold.ttf` |
| 픽셀 배지·라벨 (웹의 `font-pixel`) | **Press Start 2P** | `PressStart2P-Regular.ttf` |

라이선스 전문은 `PlusJakartaSans-OFL.txt`, `PressStart2P-OFL.txt`에 같이 넣어뒀어요.

## 화면에서 쓰는 방법

평소처럼 Tailwind 굵기 클래스만 쓰면 돼요. `tailwind.config.js`에서 `font-*` 유틸을
**굵기별 Plus Jakarta Sans 파일**로 바꿔두었기 때문이에요.

```tsx
<Text className="font-bold text-lg">제목</Text>       {/* PlusJakartaSans-Bold */}
<Text className="font-medium text-sm">본문</Text>      {/* PlusJakartaSans-Medium */}
<Text className="font-pixel text-[9px]">BADGE</Text>  {/* PressStart2P-Regular */}
```

| 클래스 | 실제 폰트 |
| --- | --- |
| `font-normal`, `font-medium` | PlusJakartaSans-Medium |
| `font-semibold` | PlusJakartaSans-SemiBold |
| `font-bold` | PlusJakartaSans-Bold |
| `font-extrabold`, `font-black` | PlusJakartaSans-ExtraBold |
| `font-pixel` | PressStart2P-Regular |

RN 안드로이드는 굵기마다 폰트 파일이 따로 필요해서 `fontWeight` 숫자만으로는 굵어지지 않아요.
그래서 위 유틸들은 "굵기별 파일 + `fontWeight: 400`" 조합으로 되어 있어요
(400으로 두는 건 안드로이드가 굵은 폰트 위에 한 번 더 굵게 덧칠하는 걸 막기 위해서예요).

> 두 폰트 모두 한글 글자가 없어서, **한글은 자동으로 시스템 폰트(안드로이드 Roboto·iOS 애플 SD 산돌고딕)로 대체**돼요.
> 웹 디자인도 같은 방식이라 보이는 결과는 같아요. 영문·숫자만 지정한 폰트로 보여요.

## 네이티브 링크 (한 번만)

폰트 파일을 넣는 것만으로는 적용되지 않고, 네이티브 프로젝트에 링크 + 재빌드가 한 번 필요해요.

- **Android**: `android/app/src/main/assets/fonts/`에 `.ttf` 를 이미 복사해뒀어요. 추가 작업 없이 재빌드만 하면 돼요.
- **iOS**: 아직 링크되어 있지 않아요. `react-native.config.js`에 폰트 폴더를 등록해뒀으니 아래 명령을 한 번 실행하면 `Info.plist`의 `UIAppFonts`와 Xcode 프로젝트에 자동으로 등록돼요.

```bash
npx react-native-asset
```

(`react-native-asset`이 없다는 오류가 나면 `npm install react-native-asset --save-dev` 로 한 번 설치한 뒤 다시 실행해주세요.)

## 재빌드

폰트 링크는 JS 핫 리로드로는 반영되지 않고, **네이티브 앱을 다시 빌드**해야 보여요.

```bash
npx react-native run-android
```

```bash
# iOS (Mac 전용, 최초 1회는 pod install도 필요해요)
cd ios && pod install && cd ..
npx react-native run-ios
```

## 확인

영문 글씨(예: 로그인 화면의 `PINKOK`, 프로필의 `PIXEL TASTE` 배지)가 시스템 기본 폰트와
다르게 보이면 성공이에요. 여전히 기본 폰트로 보인다면:

1. (iOS) `npx react-native-asset` 실행 여부 확인
2. (Android) `android/app/src/main/assets/fonts/` 폴더에 `.ttf` 가 실제로 들어있는지 확인
3. `npx react-native start --reset-cache` 로 Metro 캐시 초기화
4. 앱을 완전히 삭제한 뒤 다시 빌드 (캐시 문제일 수 있어요)
