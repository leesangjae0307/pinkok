package com.example.pinkok_backend.dto;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/**
 * 부분 수정. 보내지 않은 필드는 그대로 둔다.
 *
 * <p>{@code dayId}, {@code plannedArrivalTime} 은 <b>"안 보냄"과 "null 로 보냄"을 구분한다.</b>
 * <ul>
 *   <li>{@code {"memo":"..."}} — 일자·시각은 그대로</li>
 *   <li>{@code {"dayId":null}} — 일자 해제 (핀이 "날짜 미정"으로 돌아간다)</li>
 *   <li>{@code {"plannedArrivalTime":null}} — 도착 시각 지우기</li>
 * </ul>
 * JSON 에 키가 있으면 Jackson 이 setter 를 부르고, 없으면 안 부른다. 그래서 setter 가 불렸는지를 기록해 둔다.
 *
 * <p>dayId 로 다른 날짜로 옮기면 그 날짜 맨 뒤로 재배치된다.
 */
@Getter
@Setter
public class ItineraryItemUpdateRequest {

    private Long dayId;
    private LocalTime plannedArrivalTime;
    private Integer stayMinutes;
    private String transportMode;

    @Size(max = 500)
    private String memo;

    /**
     * 이 핀의 장소 종류 바꾸기 (GET /places/categories 의 code). null 을 보내면 장소 기본 분류로 되돌린다.
     * 키를 안 보내면 그대로.
     */
    private String categoryGroupCode;

    // 아래 두 값은 서버가 스스로 기록한다. 앱이 JSON 으로 직접 넣을 수 없게 setter 를 만들지 않는다.

    /** 요청에 dayId 키가 있었는지 (값이 null 이어도 true) */
    @Setter(AccessLevel.NONE)
    private boolean dayIdPresent;
    /** 요청에 plannedArrivalTime 키가 있었는지 (값이 null 이어도 true) */
    @Setter(AccessLevel.NONE)
    private boolean plannedArrivalTimePresent;
    /** 요청에 categoryGroupCode 키가 있었는지 (값이 null 이어도 true) */
    @Setter(AccessLevel.NONE)
    private boolean categoryGroupCodePresent;

    public void setCategoryGroupCode(String categoryGroupCode) {
        this.categoryGroupCode = categoryGroupCode;
        this.categoryGroupCodePresent = true;
    }

    public void setDayId(Long dayId) {
        this.dayId = dayId;
        this.dayIdPresent = true;
    }

    public void setPlannedArrivalTime(LocalTime plannedArrivalTime) {
        this.plannedArrivalTime = plannedArrivalTime;
        this.plannedArrivalTimePresent = true;
    }
}
