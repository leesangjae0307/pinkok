package com.example.pinkok_backend.gemini;

/**
 * "이 콘텐츠에서 실제 장소를 뽑아줘" 프롬프트.
 *
 * <p>지은이 만든 Node.js 프로토타입(travel-app_prototype/src/services/geminiService.js)에서
 * 이미 검증된 프롬프트를 그대로 옮겨왔다 — 문구를 바꾸지 말 것. 응답 스키마(title/places/lat/lng)도
 * geocodeService.js 의 좌표 변환 폴백 로직과 맞춰져 있어서 그대로 맞춘다.
 */
public final class PlaceExtractionPromptBuilder {

    /** 유튜브 링크(fileData)·업로드 이미지에 공통으로 붙이는, 검증된 추출 프롬프트. */
    public static final String EXTRACTION_PROMPT = """
            이 콘텐츠에서 실제 방문 가능한 모든 장소(맛집, 카페, 관광지, 시장, 해변, 문화재, 공원 등)를 추출하세요.

            반드시 아래 형식의 순수 JSON만 응답하세요 (마크다운 코드 블록 없이):
            {
              "title": "콘텐츠 주제 (예: 강릉 맛집 탐방 TOP 10)",
              "places": [
                {
                  "name": "장소명 (검색에 사용할 정확한 상호명 또는 지명. 예: 테라로사 커피공장, 경포해수욕장)",
                  "address": "도로명 주소 또는 지역명 (예: 강원 강릉시 구정면 현천길 25). 모르면 빈 문자열",
                  "category": "맛집|카페|관광지|해변|문화재|시장|공원|쇼핑|숙소|기타 중 하나",
                  "description": "간단한 설명 (1-2문장)",
                  "lat": 0,
                  "lng": 0
                }
              ]
            }

            주의사항:
            - 영상/이미지에 명확히 나오는 장소만 추출하세요 (추측 금지)
            - 장소명은 카카오맵에서 검색했을 때 찾을 수 있는 정확한 이름으로 추출하세요
            - 주소는 알 수 있는 경우에만 입력하고, 모르면 반드시 빈 문자열("")로 두세요
            - 최대 15개까지만 추출하세요""";

    /**
     * 유튜브가 아닌 링크(인스타/블로그 등)는 Gemini가 영상을 직접 못 보므로, URL 문자열과
     * 모델이 아는 지식만으로 추정해야 한다는 걸 명시한다 — 유튜브(fileData)보다 정확도가 낮다.
     */
    public static String forNonYoutubeLink(String sourceUrl) {
        return """
                다음 링크에서 소개됐을 만한 장소를 추정해줘. 링크: %s
                주의: 너는 이 링크를 직접 열어볼 수 없어. URL 문자열과 네가 이미 아는 정보만으로
                합리적으로 추정 가능한 경우에만 답하고, 알 수 없으면 빈 배열을 반환해. 억지로 지어내지 마.

                """.formatted(sourceUrl) + EXTRACTION_PROMPT;
    }

    public static String forText(String sourceText) {
        return """
                다음 글에서 언급된 장소를 뽑아줘.
                ---
                %s
                ---

                """.formatted(sourceText) + EXTRACTION_PROMPT;
    }

    private PlaceExtractionPromptBuilder() {
    }
}
