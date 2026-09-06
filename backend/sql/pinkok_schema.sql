-- =========================================================
-- AI 기반 여행 애플리케이션 - Database Schema
-- Target: MySQL 8.0 / ERDCloud Import
-- =========================================================

-- ---------------------------------------------------------
-- 1. 사용자 도메인
-- ---------------------------------------------------------

CREATE TABLE users (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
    email             VARCHAR(255) NULL COMMENT '이메일',
    password_hash     VARCHAR(255) NULL COMMENT '비밀번호 해시 (소셜 로그인 시 NULL)',
    nickname          VARCHAR(50)  NOT NULL COMMENT '닉네임',
    profile_image_url VARCHAR(500) NULL COMMENT '프로필 이미지 URL',
    provider          VARCHAR(20)  NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL / KAKAO / GOOGLE',
    provider_uid      VARCHAR(100) NULL COMMENT '소셜 로그인 고유 ID',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        DATETIME     NULL COMMENT 'soft delete',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_provider (provider, provider_uid)
) COMMENT='사용자';


CREATE TABLE travel_styles (
    id   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '여행 스타일 ID',
    code VARCHAR(30) NOT NULL COMMENT 'FOOD / SIGHT / NATURE / SHOPPING / CAFE',
    name VARCHAR(50) NOT NULL COMMENT '표시명 (맛집 위주, 관광지, 자연, 쇼핑 등)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_travel_styles_code (code)
) COMMENT='여행 스타일 코드';


CREATE TABLE user_preferences (
    user_id                BIGINT      NOT NULL COMMENT '사용자 ID',
    default_companion_type VARCHAR(20) NULL COMMENT 'SOLO / COUPLE / FAMILY / FRIEND',
    updated_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='사용자 기본 취향';


CREATE TABLE user_travel_styles (
    user_id  BIGINT NOT NULL COMMENT '사용자 ID',
    style_id BIGINT NOT NULL COMMENT '여행 스타일 ID',
    PRIMARY KEY (user_id, style_id),
    CONSTRAINT fk_user_travel_styles_user  FOREIGN KEY (user_id)  REFERENCES users (id),
    CONSTRAINT fk_user_travel_styles_style FOREIGN KEY (style_id) REFERENCES travel_styles (id)
) COMMENT='사용자-여행 스타일 매핑';


-- ---------------------------------------------------------
-- 2. 장소 마스터
-- ---------------------------------------------------------

CREATE TABLE places (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '장소 ID',
    kakao_place_id      VARCHAR(50)   NULL COMMENT '카카오맵 장소 ID',
    name                VARCHAR(200)  NOT NULL COMMENT '장소명',
    road_address        VARCHAR(300)  NULL COMMENT '도로명 주소',
    lot_address         VARCHAR(300)  NULL COMMENT '지번 주소',
    latitude            DECIMAL(10,7) NOT NULL COMMENT '위도',
    longitude           DECIMAL(10,7) NOT NULL COMMENT '경도',
    category_group_code VARCHAR(20)   NULL COMMENT '카카오 카테고리 그룹 코드',
    category_name       VARCHAR(200)  NULL COMMENT '카테고리명',
    phone               VARCHAR(30)   NULL,
    place_url           VARCHAR(500)  NULL COMMENT '카카오맵 상세 URL',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_places_kakao (kakao_place_id),
    KEY idx_places_coord (latitude, longitude)
) COMMENT='장소 마스터 (카카오맵 기준)';


-- ---------------------------------------------------------
-- 3. 여행 도메인
-- ---------------------------------------------------------

CREATE TABLE trips (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '여행 ID',
    user_id         BIGINT       NOT NULL COMMENT '사용자 ID',
    title           VARCHAR(100) NOT NULL COMMENT '여행 제목',
    region          VARCHAR(100) NULL COMMENT '여행 지역 (예: 제주도)',
    start_date      DATE         NULL COMMENT '시작일',
    end_date        DATE         NULL COMMENT '종료일',
    companion_type  VARCHAR(20)  NULL COMMENT 'SOLO / COUPLE / FAMILY / FRIEND',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PLANNING' COMMENT 'PLANNING / ONGOING / COMPLETED',
    cover_image_url VARCHAR(500) NULL,
    is_public       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '공개 여부',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      DATETIME     NULL COMMENT 'soft delete',
    PRIMARY KEY (id),
    KEY idx_trips_user (user_id),
    CONSTRAINT fk_trips_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='여행';


CREATE TABLE trip_travel_styles (
    trip_id  BIGINT NOT NULL COMMENT '여행 ID',
    style_id BIGINT NOT NULL COMMENT '여행 스타일 ID',
    PRIMARY KEY (trip_id, style_id),
    CONSTRAINT fk_trip_travel_styles_trip  FOREIGN KEY (trip_id)  REFERENCES trips (id),
    CONSTRAINT fk_trip_travel_styles_style FOREIGN KEY (style_id) REFERENCES travel_styles (id)
) COMMENT='여행-여행 스타일 매핑';


-- ---------------------------------------------------------
-- 4. AI 요청 / 장소 추출
-- ---------------------------------------------------------

CREATE TABLE ai_requests (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'AI 요청 ID',
    user_id         BIGINT        NOT NULL COMMENT '요청 사용자 ID',
    trip_id         BIGINT        NULL COMMENT '여행 ID (미지정 가능)',
    request_type    VARCHAR(20)   NOT NULL COMMENT 'PLACE_EXTRACT / ROUTE_OPTIMIZE / RECOMMEND / RANDOM_PLAN',
    source_type     VARCHAR(20)   NULL COMMENT 'YOUTUBE_SHORTS / YOUTUBE_LONG / INSTAGRAM_IMAGE / KEYWORD',
    source_url      VARCHAR(1000) NULL COMMENT '유튜브 링크 등 원본 URL',
    source_file_url VARCHAR(500)  NULL COMMENT '업로드 이미지 저장 경로',
    prompt_text     TEXT          NULL COMMENT '실제 전송 프롬프트 (정확도 개선 분석용)',
    model_name      VARCHAR(50)   NULL COMMENT '예: gemini-2.0-flash',
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / PROCESSING / SUCCESS / FAILED',
    error_code      VARCHAR(50)   NULL COMMENT '예: 503 SERVICE_UNAVAILABLE',
    retry_count     INT           NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    input_tokens    INT           NULL COMMENT '입력 토큰 수',
    output_tokens   INT           NULL COMMENT '출력 토큰 수',
    cost_usd        DECIMAL(10,6) NULL COMMENT '요청당 비용(USD)',
    raw_response    JSON          NULL COMMENT 'AI 원본 응답',
    requested_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    DATETIME      NULL,
    PRIMARY KEY (id),
    KEY idx_ai_requests_user (user_id),
    KEY idx_ai_requests_trip (trip_id),
    KEY idx_ai_requests_status (status, requested_at),
    CONSTRAINT fk_ai_requests_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ai_requests_trip FOREIGN KEY (trip_id) REFERENCES trips (id)
) COMMENT='AI 호출 이력 (오류 추적 및 토큰/비용 집계)';


CREATE TABLE place_candidates (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '장소 후보 ID',
    ai_request_id BIGINT       NOT NULL COMMENT 'AI 요청 ID',
    place_id      BIGINT       NULL COMMENT '좌표 변환 성공 시 매핑된 장소 ID',
    raw_name      VARCHAR(200) NOT NULL COMMENT 'AI가 추출한 장소명',
    raw_address   VARCHAR(300) NULL COMMENT 'AI가 추출한 주소',
    category_text VARCHAR(100) NULL COMMENT 'AI가 분류한 카테고리',
    description   TEXT         NULL COMMENT '추출 근거 / 설명',
    confidence    DECIMAL(4,3) NULL COMMENT '신뢰도 0.000~1.000',
    is_selected   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '사용자 체크리스트 선택 여부',
    display_order INT          NULL COMMENT '노출 순서',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_place_candidates_request (ai_request_id),
    CONSTRAINT fk_place_candidates_request FOREIGN KEY (ai_request_id) REFERENCES ai_requests (id),
    CONSTRAINT fk_place_candidates_place   FOREIGN KEY (place_id)      REFERENCES places (id)
) COMMENT='AI 추출 장소 후보 (사용자 검수 대상)';


-- ---------------------------------------------------------
-- 5. 일정 / 동선
-- ---------------------------------------------------------

CREATE TABLE itinerary_days (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '일자 ID',
    trip_id    BIGINT   NOT NULL COMMENT '여행 ID',
    day_number INT      NOT NULL COMMENT '1일차, 2일차 ...',
    visit_date DATE     NULL COMMENT '실제 날짜',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_itinerary_days_trip_day (trip_id, day_number),
    CONSTRAINT fk_itinerary_days_trip FOREIGN KEY (trip_id) REFERENCES trips (id)
) COMMENT='여행 일자';


CREATE TABLE itinerary_items (
    id                   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '일정 항목 ID',
    trip_id              BIGINT      NOT NULL COMMENT '여행 ID',
    day_id               BIGINT      NULL COMMENT '일자 ID (NULL = 날짜 미배정 핀)',
    place_id             BIGINT      NOT NULL COMMENT '장소 ID',
    candidate_id         BIGINT      NULL COMMENT '출처가 된 AI 후보 ID',
    visit_order          INT         NULL COMMENT '해당 일자 내 방문 순서',
    planned_arrival_time TIME        NULL COMMENT '예상 도착 시각',
    stay_minutes         INT         NULL COMMENT '예상 체류 시간(분)',
    memo                 VARCHAR(500) NULL COMMENT '계획 단계 메모',
    added_by             VARCHAR(10) NOT NULL DEFAULT 'AI' COMMENT 'AI / USER',
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_itinerary_items_order (day_id, visit_order),
    KEY idx_itinerary_items_trip (trip_id),
    CONSTRAINT fk_itinerary_items_trip      FOREIGN KEY (trip_id)      REFERENCES trips (id),
    CONSTRAINT fk_itinerary_items_day       FOREIGN KEY (day_id)       REFERENCES itinerary_days (id),
    CONSTRAINT fk_itinerary_items_place     FOREIGN KEY (place_id)     REFERENCES places (id),
    CONSTRAINT fk_itinerary_items_candidate FOREIGN KEY (candidate_id) REFERENCES place_candidates (id)
) COMMENT='확정 일정 항목 (지도 핀)';


CREATE TABLE route_optimizations (
    id                 BIGINT   NOT NULL AUTO_INCREMENT COMMENT '동선 최적화 ID',
    trip_id            BIGINT   NOT NULL COMMENT '여행 ID',
    ai_request_id      BIGINT   NULL COMMENT 'AI 요청 ID',
    total_distance_m   INT      NULL COMMENT '총 이동 거리(m)',
    total_duration_min INT      NULL COMMENT '총 이동 시간(분)',
    result_json        JSON     NULL COMMENT '날짜별 동선 결과',
    is_applied         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '일정에 반영 여부',
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_route_optimizations_trip (trip_id),
    CONSTRAINT fk_route_optimizations_trip    FOREIGN KEY (trip_id)       REFERENCES trips (id),
    CONSTRAINT fk_route_optimizations_request FOREIGN KEY (ai_request_id) REFERENCES ai_requests (id)
) COMMENT='AI 동선 최적화 이력';


CREATE TABLE recommendations (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '추천 ID',
    trip_id        BIGINT       NOT NULL COMMENT '여행 ID',
    ai_request_id  BIGINT       NULL COMMENT 'AI 요청 ID',
    place_id       BIGINT       NULL COMMENT '좌표 변환된 장소 ID',
    suggested_name VARCHAR(200) NOT NULL COMMENT '추천 장소명',
    reason         VARCHAR(500) NULL COMMENT '추천 사유',
    status         VARCHAR(20)  NOT NULL DEFAULT 'SUGGESTED' COMMENT 'SUGGESTED / ACCEPTED / REJECTED',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_recommendations_trip (trip_id),
    CONSTRAINT fk_recommendations_trip    FOREIGN KEY (trip_id)       REFERENCES trips (id),
    CONSTRAINT fk_recommendations_request FOREIGN KEY (ai_request_id) REFERENCES ai_requests (id),
    CONSTRAINT fk_recommendations_place   FOREIGN KEY (place_id)      REFERENCES places (id)
) COMMENT='맞춤형 장소 추천';


-- ---------------------------------------------------------
-- 6. 기록 (일기장) / 미니 브이로그
-- ---------------------------------------------------------

CREATE TABLE diaries (
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '기록 ID',
    itinerary_item_id BIGINT      NOT NULL COMMENT '일정 항목 ID',
    user_id           BIGINT      NOT NULL COMMENT '작성자 ID',
    content           TEXT        NULL COMMENT '메모 본문',
    visited_at        DATETIME    NULL COMMENT '실제 방문 시각',
    rating            TINYINT     NULL COMMENT '만족도 1~5',
    weather           VARCHAR(30) NULL COMMENT '날씨',
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_diaries_item (itinerary_item_id),
    CONSTRAINT fk_diaries_item FOREIGN KEY (itinerary_item_id) REFERENCES itinerary_items (id),
    CONSTRAINT fk_diaries_user FOREIGN KEY (user_id)           REFERENCES users (id)
) COMMENT='장소별 일기장 기록';


CREATE TABLE diary_media (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '미디어 ID',
    diary_id      BIGINT       NOT NULL COMMENT '기록 ID',
    media_type    VARCHAR(10)  NOT NULL COMMENT 'PHOTO / VIDEO',
    file_url      VARCHAR(500) NOT NULL COMMENT '파일 URL',
    thumbnail_url VARCHAR(500) NULL COMMENT '썸네일 URL',
    duration_ms   INT          NULL COMMENT '영상 길이(ms), 2000~5000',
    width         INT          NULL,
    height        INT          NULL,
    file_size     BIGINT       NULL COMMENT '파일 크기(byte)',
    display_order INT          NULL COMMENT '노출 순서',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_diary_media_diary (diary_id),
    CONSTRAINT fk_diary_media_diary FOREIGN KEY (diary_id) REFERENCES diaries (id)
) COMMENT='기록 첨부 사진/영상';


CREATE TABLE vlogs (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '브이로그 ID',
    trip_id       BIGINT       NOT NULL COMMENT '여행 ID',
    user_id       BIGINT       NOT NULL COMMENT '사용자 ID',
    title         VARCHAR(100) NULL COMMENT '브이로그 제목',
    status        VARCHAR(20)  NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED / PROCESSING / DONE / FAILED',
    video_url     VARCHAR(500) NULL COMMENT '합성 결과 영상 URL',
    thumbnail_url VARCHAR(500) NULL,
    duration_ms   INT          NULL COMMENT '총 길이(ms)',
    resolution    VARCHAR(20)  NULL COMMENT '예: 1080x1920',
    error_message VARCHAR(500) NULL COMMENT '합성 실패 사유',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at  DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_vlogs_trip (trip_id),
    CONSTRAINT fk_vlogs_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_vlogs_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='미니 브이로그';


CREATE TABLE vlog_clips (
    id             BIGINT NOT NULL AUTO_INCREMENT COMMENT '클립 ID',
    vlog_id        BIGINT NOT NULL COMMENT '브이로그 ID',
    diary_media_id BIGINT NOT NULL COMMENT '원본 미디어 ID',
    clip_order     INT    NOT NULL COMMENT '동선 순서 (1,2,3,4...)',
    start_ms       INT    NOT NULL DEFAULT 0 COMMENT '시작 지점(ms)',
    end_ms         INT    NULL COMMENT '종료 지점(ms)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_vlog_clips_order (vlog_id, clip_order),
    CONSTRAINT fk_vlog_clips_vlog  FOREIGN KEY (vlog_id)        REFERENCES vlogs (id),
    CONSTRAINT fk_vlog_clips_media FOREIGN KEY (diary_media_id) REFERENCES diary_media (id)
) COMMENT='브이로그 구성 클립 (원본 미디어 참조)';


-- ---------------------------------------------------------
-- 7. 공유
-- ---------------------------------------------------------

CREATE TABLE shares (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '공유 ID',
    user_id     BIGINT       NOT NULL COMMENT '공유자 ID',
    target_type VARCHAR(10)  NOT NULL COMMENT 'TRIP / DIARY / VLOG',
    target_id   BIGINT       NOT NULL COMMENT '대상 ID (target_type 기준)',
    share_token VARCHAR(64)  NOT NULL COMMENT '공유 링크 토큰',
    visibility  VARCHAR(20)  NOT NULL DEFAULT 'LINK' COMMENT 'PUBLIC / LINK / PRIVATE',
    expires_at  DATETIME     NULL COMMENT '만료 시각',
    view_count  INT          NOT NULL DEFAULT 0 COMMENT '조회수',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_shares_token (share_token),
    KEY idx_shares_target (target_type, target_id),
    CONSTRAINT fk_shares_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='공유 링크';
