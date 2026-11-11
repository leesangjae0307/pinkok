package com.example.pinkok_backend.push;

import java.util.List;
import java.util.Map;

/**
 * 폰 푸시를 실제로 보내는 일. 지금 구현은 FCM({@link FcmPushSender}) 하나다.
 * 인터페이스로 끊어둔 이유는 테스트에서 진짜 Firebase 를 부르지 않고 가짜로 바꿔 끼우기 위해서다.
 */
public interface PushSender {

    /**
     * @return 더 이상 쓸 수 없는 토큰들(앱 삭제 등). 부른 쪽이 지운다. 보낼 수 없는 상태면 빈 목록.
     */
    List<String> send(List<String> tokens, Message message);

    /**
     * @param data 앱이 알림을 눌렀을 때 화면을 고르는 데 쓰는 값 (type, targetType, targetId 등). 값은 문자열만 된다.
     */
    record Message(String title, String body, Map<String, String> data) {
    }
}
