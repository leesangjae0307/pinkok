/**
 * 카카오맵 (WebView + JavaScript SDK)
 *
 * 카카오는 React Native 용 지도 SDK 를 내지 않는다. 공식 패키지 @react-native-kakao/core 도
 * 2025-03 에 지도 모듈을 뺐고, 남은 것은 웹용 래퍼나 Flutter 플러그인뿐이다. 그래서 SDK 를
 * WebView 안에서 돌리고, 그릴 핀 목록만 RN 에서 밀어 넣는다. 핀 선택은 바텀시트
 * 목록에서 하고, 지도는 그 결과를 색으로만 보여준다.
 *
 * 주의: JS SDK 는 호출 출처(origin)로 키를 검증한다. 로컬 HTML 은 출처가 없어 인증이 막히므로
 * `source.baseUrl` 로 카카오 개발자 콘솔에 등록한 도메인을 지정해야 한다.
 */
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, Pressable, Text, View } from 'react-native';
import { WebView, type WebViewMessageEvent } from 'react-native-webview';

import { KAKAO_JS_KEY, KAKAO_SDK_ORIGIN } from '../../constants/kakao';

export interface MapMarker {
  id: number;
  /** 마커 위에 띄울 짧은 이름 */
  label: string;
  latitude: number;
  longitude: number;
  /** 방문 순서. 마커에 번호로 찍는다. */
  order: number;
}

/** 지도에 그릴 내용을 WebView 로 보낼 때 쓰는 모양 */
interface MapPayload {
  markers: MapMarker[];
  selectedId: number | null;
  /** 바텀시트가 덮는 아래쪽 높이(px). 핀이 시트 뒤로 숨지 않게 띄울 때 쓴다. */
  bottomInset: number;
  /** 상단 플로팅 바가 덮는 높이(px) */
  topInset: number;
}

/**
 * 지도 HTML. 마커와 선택 상태는 `window.renderMap(payload)` 로 들어오고,
 * 준비·실패만 `{type:'ready'|'error'}` 로 RN 에 알린다.
 */
function buildHtml(): string {
  return `<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" />
  <style>
    html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; }
    body { background: #B4FAF0; }
    .label {
      padding: 3px 7px; background: #0f172a; color: #fff;
      border-radius: 6px; font-size: 11px; font-weight: 700;
      white-space: nowrap; font-family: sans-serif;
    }
  </style>
  <script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false"></script>
</head>
<body>
  <div id="map"></div>
  <script>
    var map = null;
    var overlays = [];

    function post(msg) {
      if (window.ReactNativeWebView) window.ReactNativeWebView.postMessage(JSON.stringify(msg));
    }

    function clearOverlays() {
      overlays.forEach(function (o) { o.setMap(null); });
      overlays = [];
    }

    window.renderMap = function (payload) {
      if (!map) return;
      clearOverlays();
      var markers = payload.markers || [];
      if (markers.length === 0) return;

      var bounds = new kakao.maps.LatLngBounds();
      markers.forEach(function (m) {
        var pos = new kakao.maps.LatLng(m.latitude, m.longitude);
        bounds.extend(pos);

        var marker = new kakao.maps.Marker({ position: pos, map: map });
        overlays.push(marker);

        var selected = payload.selectedId === m.id;
        var label = new kakao.maps.CustomOverlay({
          position: pos,
          yAnchor: 2.2,
          content: '<div class="label" style="background:'
            + (selected ? '#FF5A87' : '#0f172a') + '">'
            + m.order + '. ' + m.label + '</div>',
          map: map,
        });
        overlays.push(label);
      });

      // 지도의 아래쪽은 바텀시트가 덮는다. 그 높이만큼 여백을 더 줘야 핀이
      // 시트 뒤로 숨지 않는다.
      var inset = payload.bottomInset || 0;
      var top = payload.topInset || 0;

      // 핀이 하나면 bounds 가 점이라 과하게 확대된다. 그때는 중심만 맞춘다.
      if (markers.length === 1) {
        map.setCenter(new kakao.maps.LatLng(markers[0].latitude, markers[0].longitude));
        map.setLevel(4);
        // 중심이 화면 한가운데라 시트에 가린다. 보이는 영역의 가운데로 끌어올린다.
        if (inset > 0) map.panBy(0, inset / 2);
      } else {
        map.setBounds(bounds, top + 40, 40, inset + 40, 40);
      }
    };

    if (!window.kakao || !window.kakao.maps) {
      post({ type: 'error', message: 'SDK_LOAD_FAILED' });
    } else {
      kakao.maps.load(function () {
        map = new kakao.maps.Map(document.getElementById('map'), {
          center: new kakao.maps.LatLng(37.5665, 126.9780),
          level: 8,
        });
        post({ type: 'ready' });
      });
    }
  </script>
</body>
</html>`;
}

export default function KakaoMapView({
  markers,
  selectedId,
  bottomInset = 0,
  topInset = 0,
}: {
  markers: MapMarker[];
  selectedId: number | null;
  /** 바텀시트가 덮는 아래쪽 높이(px) */
  bottomInset?: number;
  /** 상단 플로팅 바가 덮는 높이(px) */
  topInset?: number;
}) {
  const webRef = useRef<WebView>(null);
  const [isReady, setIsReady] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const html = useMemo(() => buildHtml(), []);
  const payload: MapPayload = useMemo(
    () => ({ markers, selectedId, bottomInset, topInset }),
    [markers, selectedId, bottomInset, topInset],
  );

  /** 핀이나 선택이 바뀌면 다시 그린다. 지도가 준비되기 전에는 보내봐야 소용없다. */
  useEffect(() => {
    if (!isReady) return;
    webRef.current?.injectJavaScript(`window.renderMap(${JSON.stringify(payload)}); true;`);
  }, [isReady, payload]);

  const handleMessage = useCallback(
    (event: WebViewMessageEvent) => {
      // 파싱만 감싼다. 지도에서 온 게 아닌 메시지는 무시하되, 처리 중에 난 오류까지
      // 삼키면 원인을 알 수 없게 된다.
      let msg: { type: string; message?: string };
      try {
        msg = JSON.parse(event.nativeEvent.data);
      } catch {
        return;
      }

      if (msg.type === 'ready') {
        setIsReady(true);
        setLoadError(null);
        webRef.current?.injectJavaScript(`window.renderMap(${JSON.stringify(payload)}); true;`);
        return;
      }
      if (msg.type === 'error') {
        setLoadError(
          msg.message === 'SDK_LOAD_FAILED'
            ? '지도를 불러오지 못했어요. 네트워크와 카카오 키 설정을 확인해 주세요.'
            : '지도를 불러오지 못했어요.',
        );
      }
    },
    [payload],
  );

  const retry = () => {
    setLoadError(null);
    setIsReady(false);
    setReloadKey((k) => k + 1);
  };

  return (
    <View className="flex-1 bg-[#B4FAF0]/40">
      <WebView
        key={reloadKey}
        ref={webRef}
        source={{ html, baseUrl: KAKAO_SDK_ORIGIN }}
        originWhitelist={['*']}
        javaScriptEnabled
        domStorageEnabled
        onMessage={handleMessage}
        onError={() => setLoadError('지도를 불러오지 못했어요. 네트워크를 확인해 주세요.')}
        onHttpError={() => setLoadError('지도를 불러오지 못했어요. 네트워크를 확인해 주세요.')}
        style={{ flex: 1, backgroundColor: 'transparent' }}
      />

      {!isReady && !loadError && (
        <View pointerEvents="none" className="absolute bottom-0 left-0 right-0 top-0 items-center justify-center">
          <ActivityIndicator color="#075953" />
        </View>
      )}

      {loadError && (
        <View className="absolute bottom-0 left-0 right-0 top-0 items-center justify-center gap-3 bg-[#B4FAF0]/90 px-8">
          <Text className="text-center text-xs font-bold text-slate-600">{loadError}</Text>
          <Pressable onPress={retry} className="rounded-xl bg-[#56EADD] px-4 py-2 active:scale-95">
            <Text className="text-xs font-black text-[#075953]">다시 시도</Text>
          </Pressable>
        </View>
      )}
    </View>
  );
}
