package com.example.pinkok_backend.gemini;

import java.util.List;

/**
 * "이 여행에 갈 만한 장소를 추천해줘" 프롬프트.
 *
 * <p>장소 추출과 달리 검증된 프로토타입 프롬프트가 없어서 새로 만든 것이다 - 실제 응답을 보면서
 * 문구를 다듬을 것. 응답은 PlaceExtractionPromptBuilder 와 같은 필드명(name/address/category)을
 * 써서, 수락할 때 같은 방식으로 카카오맵 좌표를 찾을 수 있게 한다.
 */
public final class RecommendationPromptBuilder {

    private static final String OUTPUT_FORMAT = """

            반드시 아래 형식의 순수 JSON만 응답하세요 (마크다운 코드 블록 없이):
            {
              "recommendations": [
                {
                  "name": "장소명 (카카오맵에서 검색했을 때 찾을 수 있는 정확한 상호명 또는 지명)",
                  "address": "도로명 주소 또는 지역명. 모르면 빈 문자열",
                  "category": "맛집|카페|관광지|해변|문화재|시장|공원|쇼핑|숙소|기타 중 하나",
                  "reason": "이 여행 조건에 왜 어울리는지 한 문장 (80자 이내)"
                }
              ]
            }

            주의사항:
            - 실제로 존재하고 현재 방문 가능한 장소만 추천하세요 (지어내기 금지)
            - 지역이 지정돼 있으면 반드시 그 지역 안의 장소만 추천하세요
            - 동행과 선호 스타일에 맞게 고르되, 카테고리가 한쪽으로 쏠리지 않게 적당히 섞으세요
            """;

    private RecommendationPromptBuilder() {
    }

    /**
     * @param region         여행 지역 (없으면 null)
     * @param period         여행 기간 설명 (예: "2박 3일", 모르면 null)
     * @param companionLabel 동반자 설명 (예: "커플", 모르면 null)
     * @param styleNames     선호 스타일 표시명들 (비어 있을 수 있음)
     * @param excludeNames   이미 일정에 있거나 예전에 추천한 장소명 - 다시 추천하면 안 됨
     * @param count          추천 개수
     */
    public static String build(String region,
                               String period,
                               String companionLabel,
                               List<String> styleNames,
                               List<String> excludeNames,
                               int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 한국 여행 코스를 짜주는 여행 큐레이터입니다. 아래 여행 조건에 맞는 방문 장소를 추천하세요.\n\n");
        sb.append("[여행 조건]\n");
        sb.append("- 지역: ")
                .append(region == null || region.isBlank() ? "지정 안 됨 (한국 내 인기 여행지에서 고르세요)" : region)
                .append('\n');
        if (period != null) {
            sb.append("- 기간: ").append(period).append('\n');
        }
        if (companionLabel != null) {
            sb.append("- 동행: ").append(companionLabel).append('\n');
        }
        if (styleNames != null && !styleNames.isEmpty()) {
            sb.append("- 선호 스타일: ").append(String.join(", ", styleNames)).append('\n');
        }
        if (excludeNames != null && !excludeNames.isEmpty()) {
            sb.append("\n[이미 있는 장소 - 절대 다시 추천하지 마세요]\n");
            sb.append(String.join(", ", excludeNames)).append('\n');
        }
        sb.append(OUTPUT_FORMAT);
        sb.append("- 최대 ").append(count).append("개까지만 추천하세요");
        return sb.toString();
    }
}
