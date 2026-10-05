package com.example.pinkok_backend.dto;

import com.example.pinkok_backend.entity.PlaceCandidate;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * AI가 뽑은 장소 후보 하나 (사용자 검수용 체크리스트 항목).
 *
 * <p>{@code place} 가 채워져 있으면 카카오맵에서 좌표를 찾은 것이고, 비어 있으면 못 찾은 것이다.
 * 못 찾은 후보도 목록에는 남기되({@code mapped=false}) 여행에 핀으로는 추가할 수 없다.
 */
@Getter
public class PlaceCandidateResponse {

    private final Long id;
    private final Long aiRequestId;
    /** AI가 추출한 원래 이름 (카카오맵에서 찾은 이름과 다를 수 있다) */
    private final String rawName;
    private final String rawAddress;
    private final String categoryText;
    /** 추출 근거 / 설명 */
    private final String description;
    /** 0.000~1.000. 카카오맵 검색 결과가 AI 이름과 얼마나 맞아떨어지는지 */
    private final BigDecimal confidence;
    /** 좌표를 찾았는지. false면 핀으로 추가할 수 없다 */
    private final boolean mapped;
    /**
     * 사람이 한 번 확인하는 게 좋은 후보인지 (신뢰도 0.6 이하).
     * 지역까지 확인되지 않았거나 이름이 애매하게 맞은 경우라, 앱에서 자동 선택하지 말고
     * "확인 필요"로 표시하는 용도다.
     */
    private final boolean needsReview;
    /** 좌표를 찾았을 때만 채워짐 */
    private final PlaceResponse place;
    /** 이 후보가 나온 스크린샷 주소 (스크린샷 1장으로 요청했을 때만) */
    private final String sourceImageUrl;
    /** 이미 여행에 추가한 적이 있는지 */
    private final boolean selected;
    private final Integer displayOrder;

    private PlaceCandidateResponse(PlaceCandidate candidate) {
        this.id = candidate.getId();
        this.aiRequestId = candidate.getAiRequest().getId();
        this.rawName = candidate.getRawName();
        this.rawAddress = candidate.getRawAddress();
        this.categoryText = candidate.getCategoryText();
        this.description = candidate.getDescription();
        this.confidence = candidate.getConfidence();
        this.mapped = candidate.getPlace() != null;
        this.needsReview = candidate.getPlace() != null
                && candidate.getConfidence() != null
                && candidate.getConfidence().compareTo(new BigDecimal("0.600")) <= 0;
        this.place = candidate.getPlace() == null ? null : PlaceResponse.from(candidate.getPlace());
        this.sourceImageUrl = candidate.getSourceImage() == null ? null : candidate.getSourceImage().getFileUrl();
        this.selected = Boolean.TRUE.equals(candidate.getIsSelected());
        this.displayOrder = candidate.getDisplayOrder();
    }

    public static PlaceCandidateResponse from(PlaceCandidate candidate) {
        return new PlaceCandidateResponse(candidate);
    }
}
