package com.example.pinkok_backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 가 준 주소에서 지역 확인용 시/군/구 뽑기.
 * 카카오는 시·도를 "서울", "경기"처럼 줄여서 주므로, 시·도 단계가 아니라 시/군/구로 비교해야 한다.
 */
class AddressRegionTest {

    @Test
    @DisplayName("특별시·광역시: 시·도는 건너뛰고 구를 쓴다 (예전에는 '서울특별시'를 잡아 전부 매칭 실패)")
    void metropolitanCities() {
        assertThat(AddressRegion.extract("서울특별시 강남구 강남대로 390")).isEqualTo("강남구");
        assertThat(AddressRegion.extract("서울시 강남구 테헤란로 152")).isEqualTo("강남구");
        assertThat(AddressRegion.extract("서울 강남구 역삼동 858")).isEqualTo("강남구");
        assertThat(AddressRegion.extract("부산광역시 해운대구 우동")).isEqualTo("해운대구");
        assertThat(AddressRegion.extract("대구광역시 중구 동성로")).isEqualTo("중구");
        assertThat(AddressRegion.extract("인천광역시 연수구 송도동")).isEqualTo("연수구");
        assertThat(AddressRegion.extract("광주광역시 서구 상무대로")).isEqualTo("서구");
        assertThat(AddressRegion.extract("대전광역시 유성구 대학로 99")).isEqualTo("유성구");
        assertThat(AddressRegion.extract("울산광역시 남구 삼산로")).isEqualTo("남구");
    }

    @Test
    @DisplayName("도: 시·군을 쓰고, 구가 있으면 더 구체적인 구를 쓴다")
    void provinces() {
        assertThat(AddressRegion.extract("강원 강릉시 강릉역")).isEqualTo("강릉시");
        assertThat(AddressRegion.extract("강원특별자치도 강릉시 임영로207번길 6-1")).isEqualTo("강릉시");
        assertThat(AddressRegion.extract("제주특별자치도 제주시 애월읍")).isEqualTo("제주시");
        assertThat(AddressRegion.extract("경기도 수원시 팔달구 정조로")).isEqualTo("팔달구");
        assertThat(AddressRegion.extract("경상북도 울릉군 울릉읍")).isEqualTo("울릉군");
        assertThat(AddressRegion.extract("경기 광주시 경안동")).isEqualTo("광주시"); // 경기 광주시는 시·도가 아니다
    }

    @Test
    @DisplayName("시/군/구가 없는 세종은 카카오가 쓰는 짧은 이름 '세종'으로")
    void sejong() {
        assertThat(AddressRegion.extract("세종특별자치시 조치원읍 새내로")).isEqualTo("세종");
        assertThat(AddressRegion.extract("세종시 한누리대로 2130")).isEqualTo("세종");
    }

    @Test
    @DisplayName("도로명·동·번지 뒤의 '출구' 같은 말은 행정구역으로 잡지 않는다")
    void stopsAtNonAdministrativePart() {
        assertThat(AddressRegion.extract("서울 강남구 강남역 1번 출구")).isEqualTo("강남구");
        assertThat(AddressRegion.extract("강남역 출구")).isNull();
        assertThat(AddressRegion.extract("제주시 한림읍")).isEqualTo("제주시");
    }

    @Test
    @DisplayName("주소가 없거나 지역을 알 수 없으면 null (이름이 확실히 비슷할 때만 매칭)")
    void unknown() {
        assertThat(AddressRegion.extract(null)).isNull();
        assertThat(AddressRegion.extract("  ")).isNull();
        assertThat(AddressRegion.extract("바닷가 근처")).isNull();
    }
}
