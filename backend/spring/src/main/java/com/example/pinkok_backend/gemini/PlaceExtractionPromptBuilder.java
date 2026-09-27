package com.example.pinkok_backend.gemini;

/**
 * "이 콘텐츠에서 실제 장소를 뽑아줘" 프롬프트를 만든다.
 *
 * <p>주의(위험 요소, PLAN.md 참고): Gemini는 링크를 직접 열어보지 못한다. LINK 입력은
 * URL 문자열과 모델이 이미 아는 지식만으로 추정하는 것이라 정확도가 낮을 수 있다 —
 * 유튜브/인스타 영상 내용을 실제로 "보는" 게 아니다. IMAGE(스크린샷) 입력이 훨씬
 * 신뢰도가 높으니, 링크로 잘 안 되면 스크린샷 업로드로 유도하는 게 현실적이다.
 */
public final class PlaceExtractionPromptBuilder {

    private static final String OUTPUT_FORMAT = """

            아래 JSON 배열 형식으로만 답해. 다른 설명·마크다운 없이 배열만 출력해.
            [
              {"name": "장소 이름", "address": "주소(모르면 null)", "category": "카테고리(모르면 null)", "confidence": 0.0}
            ]
            confidence 는 0.0~1.0 사이 숫자로, 얼마나 확신하는지를 뜻한다.
            실제로 존재할 것 같지 않은 곳은 목록에서 아예 빼라.
            찾은 장소가 없으면 빈 배열 [] 만 반환해.
            """;

    private PlaceExtractionPromptBuilder() {
    }

    public static String forLink(String sourceUrl) {
        return """
                너는 여행 콘텐츠에서 실제로 존재하는 구체적인 장소(가게, 관광지, 숙소 등)를 찾아내는 도우미야.
                다음 링크에서 소개됐을 만한 장소를 추정해줘.
                링크: %s
                주의: 너는 이 링크를 직접 열어볼 수 없어. URL 문자열, 제목에 담긴 단서, 네가 이미 아는 정보만으로
                합리적으로 추정 가능한 경우에만 답하고, 알 수 없으면 빈 배열을 반환해. 억지로 지어내지 마.
                """.formatted(sourceUrl) + OUTPUT_FORMAT;
    }

    public static String forText(String sourceText) {
        return """
                너는 여행 콘텐츠에서 실제로 존재하는 구체적인 장소(가게, 관광지, 숙소 등)를 찾아내는 도우미야.
                다음 글에서 언급된 장소를 뽑아줘.
                ---
                %s
                ---
                """.formatted(sourceText) + OUTPUT_FORMAT;
    }

    public static String forImages(int imageCount) {
        return """
                너는 여행 콘텐츠에서 실제로 존재하는 구체적인 장소(가게, 관광지, 숙소 등)를 찾아내는 도우미야.
                첨부된 스크린샷 %d장에서 보이는 장소 이름을 읽어서 뽑아줘.
                간판, 지도 앱 화면, 영상 자막, 위치 태그처럼 화면에 보이는 글자를 근거로 삼아.
                """.formatted(imageCount) + OUTPUT_FORMAT;
    }
}
