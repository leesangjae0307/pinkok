-- =========================================================
-- PinKok - AI 기반 여행 애플리케이션 Database Schema
-- Target: MySQL 8.0 / ERDCloud Import
--
-- v3 (UI 시안 + 팀 논의 반영)
--  - 소셜 기능(팔로우/좋아요/해시태그) 미포함
--  - 브이로그 -> PinLog 로 명칭 변경, 배경음악 기능 없음
--  - 공유 = 링크 공유가 아니라 '여행에 팀원을 추가해서 같이 쓰기'
--    (trip_members / trip_invitations)
--  - 키워드 랜덤 여행 계획 기능 제외
--  - 기본 도트 아바타 / 알림 테이블 추가
--  - AI 입력은 링크 / 스크린샷(여러 장) / 붙여넣은 글 세 가지를 모두 받음
-- =========================================================

-- ---------------------------------------------------------
-- 1. 사용자 도메인
-- ---------------------------------------------------------

CREATE TABLE avatars (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '아바타 ID',
    code          VARCHAR(20)  NOT NULL COMMENT 'M1 / M2 / M3 / F1 / F2 / F3',
    name          VARCHAR(50)  NOT NULL COMMENT '표시명 (예: 모험가, 사진가)',
    image_url     VARCHAR(500) NOT NULL COMMENT '도트 이미지 URL',
    gender        VARCHAR(10)  NOT NULL COMMENT 'MALE / FEMALE',
    display_order INT          NULL COMMENT '노출 순서',
    PRIMARY KEY (id),
    UNIQUE KEY uk_avatars_code (code)
) COMMENT='기본 프로필 도트 아바타 (남3 / 여3)';


CREATE TABLE users (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
    email             VARCHAR(255) NULL COMMENT '이메일',
    password_hash     VARCHAR(255) NULL COMMENT '비밀번호 해시 (소셜 로그인 시 NULL)',
    username          VARCHAR(30)  NOT NULL COMMENT '@아이디 (예: gamjieun)',
    nickname          VARCHAR(50)  NOT NULL COMMENT '닉네임 (화면 표시명)',
    avatar_id         BIGINT       NULL COMMENT '선택한 기본 아바타 ID',
    profile_image_url VARCHAR(500) NULL COMMENT '직접 업로드한 프로필 이미지 URL',
    provider          VARCHAR(20)  NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL / KAKAO / GOOGLE / NAVER / APPLE',
    provider_uid      VARCHAR(100) NULL COMMENT '소셜 로그인 고유 ID',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        DATETIME     NULL COMMENT 'soft delete',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_provider (provider, provider_uid),
    CONSTRAINT fk_users_avatar FOREIGN KEY (avatar_id) REFERENCES avatars (id)
) COMMENT='사용자';


CREATE TABLE travel_styles (
    id   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '여행 스타일 ID',
    code VARCHAR(30) NOT NULL COMMENT 'FOOD / SIGHT / NATURE / SHOPPING / CAFE',
    name VARCHAR(50) NOT NULL COMMENT '표시명 (맛집 위주, 관광지, 자연, 쇼핑 등)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_travel_styles_code (code)
) COMMENT='여행 스타일 코드';


CREATE TABLE user_settings (
    user_id                BIGINT      NOT NULL COMMENT '사용자 ID',
    default_companion_type VARCHAR(20) NULL COMMENT 'SOLO / COUPLE / FAMILY / FRIEND',
    notification_enabled   TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '알림 수신 여부',
    theme                  VARCHAR(30) NOT NULL DEFAULT 'PIXEL_MINT' COMMENT '테마 설정',
    language               VARCHAR(10) NOT NULL DEFAULT 'ko' COMMENT '언어 설정',
    updated_at             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='사용자 설정 (취향 + 앱 설정)';


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
    cover_image_url VARCHAR(500) NULL COMMENT '여행 카드 대표 이미지',
    is_public       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '링크 공개 여부',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      DATETIME     NULL COMMENT 'soft delete',
    PRIMARY KEY (id),
    KEY idx_trips_user (user_id),
    CONSTRAINT fk_trips_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='여행';


CREATE TABLE trip_members (
    trip_id   BIGINT      NOT NULL COMMENT '여행 ID',
    user_id   BIGINT      NOT NULL COMMENT '참여자 ID',
    role      VARCHAR(10) NOT NULL DEFAULT 'MEMBER' COMMENT 'OWNER(만든 사람) / MEMBER',
    joined_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '합류 시각',
    PRIMARY KEY (trip_id, user_id),
    KEY idx_trip_members_user (user_id),
    CONSTRAINT fk_trip_members_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_trip_members_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='여행 참여자 (같이 보고 같이 쓰는 사람들)';


CREATE TABLE trip_invitations (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '초대 ID',
    trip_id      BIGINT      NOT NULL COMMENT '여행 ID',
    inviter_id   BIGINT      NOT NULL COMMENT '초대한 사람 ID',
    invitee_id   BIGINT      NULL COMMENT '초대받은 사람 ID (@아이디로 지목한 경우)',
    invite_code  VARCHAR(64) NULL COMMENT '초대 링크 코드 (링크로 부른 경우)',
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / ACCEPTED / REJECTED / EXPIRED',
    expires_at   DATETIME    NULL COMMENT '초대 만료 시각',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at DATETIME    NULL COMMENT '수락/거절한 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trip_invitations_code (invite_code),
    KEY idx_trip_invitations_invitee (invitee_id, status),
    KEY idx_trip_invitations_trip (trip_id),
    CONSTRAINT fk_trip_invitations_trip    FOREIGN KEY (trip_id)    REFERENCES trips (id),
    CONSTRAINT fk_trip_invitations_inviter FOREIGN KEY (inviter_id) REFERENCES users (id),
    CONSTRAINT fk_trip_invitations_invitee FOREIGN KEY (invitee_id) REFERENCES users (id)
) COMMENT='여행 초대장';


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
    trip_id         BIGINT        NULL COMMENT '여행 ID (추출 후 나중에 여행 카드 선택 가능)',
    request_type    VARCHAR(20)   NOT NULL COMMENT 'PLACE_EXTRACT / ROUTE_OPTIMIZE / RECOMMEND',
    input_type      VARCHAR(20)   NOT NULL COMMENT '사용자가 넣은 방식 LINK / IMAGE / TEXT',
    source_platform VARCHAR(20)   NULL COMMENT '출처 YOUTUBE / INSTAGRAM / BLOG / OTHER',
    source_url      VARCHAR(1000) NULL COMMENT 'LINK 일 때 원본 링크',
    source_text     TEXT          NULL COMMENT 'TEXT 일 때 사용자가 붙여넣은 글',
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


CREATE TABLE ai_request_images (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '이미지 ID',
    ai_request_id BIGINT       NOT NULL COMMENT 'AI 요청 ID',
    file_url      VARCHAR(500) NOT NULL COMMENT '업로드한 스크린샷 URL',
    display_order INT          NULL COMMENT '올린 순서',
    width         INT          NULL,
    height        INT          NULL,
    file_size     BIGINT       NULL COMMENT '파일 크기(byte)',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_request_images_request (ai_request_id),
    CONSTRAINT fk_ai_request_images_request FOREIGN KEY (ai_request_id) REFERENCES ai_requests (id)
) COMMENT='AI 분석용 업로드 스크린샷 (여러 장 가능)';


CREATE TABLE place_candidates (
    id              BIGINT     NOT NULL AUTO_INCREMENT COMMENT '장소 후보 ID',
    ai_request_id   BIGINT     NOT NULL COMMENT 'AI 요청 ID',
    source_image_id BIGINT     NULL COMMENT '이 후보가 나온 스크린샷 ID (썸네일 표시용)',
    place_id        BIGINT     NULL COMMENT '좌표 변환 성공 시 매핑된 장소 ID',
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
    CONSTRAINT fk_place_candidates_request FOREIGN KEY (ai_request_id)   REFERENCES ai_requests (id),
    CONSTRAINT fk_place_candidates_image   FOREIGN KEY (source_image_id) REFERENCES ai_request_images (id),
    CONSTRAINT fk_place_candidates_place   FOREIGN KEY (place_id)        REFERENCES places (id)
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
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '일정 항목 ID',
    trip_id              BIGINT       NOT NULL COMMENT '여행 ID',
    day_id               BIGINT       NULL COMMENT '일자 ID (NULL = 날짜 미배정 핀)',
    place_id             BIGINT       NOT NULL COMMENT '장소 ID',
    candidate_id         BIGINT       NULL COMMENT '출처가 된 AI 후보 ID',
    visit_order          INT          NULL COMMENT '해당 일자 내 방문 순서',
    planned_arrival_time TIME         NULL COMMENT '예상 도착 시각',
    stay_minutes         INT          NULL COMMENT '예상 체류 시간(분)',
    transport_mode       VARCHAR(20)  NULL COMMENT '직전 장소에서의 이동수단 CAR / WALK / BUS / TRAIN',
    memo                 VARCHAR(500) NULL COMMENT '계획 단계 메모',
    added_by             VARCHAR(10)  NOT NULL DEFAULT 'AI' COMMENT 'AI / USER',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_itinerary_items_order (day_id, visit_order),
    KEY idx_itinerary_items_trip (trip_id),
    CONSTRAINT fk_itinerary_items_trip      FOREIGN KEY (trip_id)      REFERENCES trips (id),
    CONSTRAINT fk_itinerary_items_day       FOREIGN KEY (day_id)       REFERENCES itinerary_days (id),
    CONSTRAINT fk_itinerary_items_place     FOREIGN KEY (place_id)     REFERENCES places (id),
    CONSTRAINT fk_itinerary_items_candidate FOREIGN KEY (candidate_id) REFERENCES place_candidates (id)
) COMMENT='확정 일정 항목 (지도 핀)';


CREATE TABLE route_optimizations (
    id                 BIGINT     NOT NULL AUTO_INCREMENT COMMENT '동선 최적화 ID',
    trip_id            BIGINT     NOT NULL COMMENT '여행 ID',
    ai_request_id      BIGINT     NULL COMMENT 'AI 요청 ID',
    total_distance_m   INT        NULL COMMENT '총 이동 거리(m)',
    total_duration_min INT        NULL COMMENT '총 이동 시간(분)',
    result_json        JSON       NULL COMMENT '날짜별 동선 결과',
    is_applied         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '일정에 반영 여부',
    created_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
-- 6. 기록 (장소별 메모 / 사진 / 영상)
-- ---------------------------------------------------------

CREATE TABLE diaries (
    id                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '기록 ID',
    itinerary_item_id BIGINT      NOT NULL COMMENT '일정 항목 ID',
    user_id           BIGINT      NOT NULL COMMENT '작성자 ID',
    content           TEXT        NULL COMMENT '메모 본문',
    rating            TINYINT     NULL COMMENT '만족도 1~5',
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_diaries_item (itinerary_item_id),
    CONSTRAINT fk_diaries_item FOREIGN KEY (itinerary_item_id) REFERENCES itinerary_items (id),
    CONSTRAINT fk_diaries_user FOREIGN KEY (user_id)           REFERENCES users (id)
) COMMENT='장소별 기록 (메모 / 사진 / 영상)';


CREATE TABLE diary_media (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '미디어 ID',
    diary_id      BIGINT       NOT NULL COMMENT '기록 ID',
    media_type    VARCHAR(10)  NOT NULL COMMENT 'PHOTO / VIDEO',
    file_url      VARCHAR(500) NOT NULL COMMENT '파일 URL',
    thumbnail_url VARCHAR(500) NULL COMMENT '썸네일 URL',
    duration_ms   INT          NULL COMMENT '영상 길이(ms)',
    width         INT          NULL,
    height        INT          NULL,
    file_size     BIGINT       NULL COMMENT '파일 크기(byte)',
    display_order INT          NULL COMMENT '노출 순서',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_diary_media_diary (diary_id),
    CONSTRAINT fk_diary_media_diary FOREIGN KEY (diary_id) REFERENCES diaries (id)
) COMMENT='기록 첨부 사진/영상 (PinLog 클립 원본)';


-- ---------------------------------------------------------
-- 7. PinLog (1초 영상 모아 자동 생성하는 미니 브이로그)
-- ---------------------------------------------------------

CREATE TABLE pinlogs (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PinLog ID',
    trip_id       BIGINT       NOT NULL COMMENT '여행 ID',
    user_id       BIGINT       NOT NULL COMMENT '만든 사람 ID',
    title         VARCHAR(100) NULL COMMENT 'PinLog 제목',
    start_date    DATE         NULL COMMENT '기간 선택 - 시작일',
    end_date      DATE         NULL COMMENT '기간 선택 - 종료일',
    template_code VARCHAR(30)  NULL COMMENT 'TRAVEL_DIARY / CINEMATIC / EMOTIONAL',
    style_code    VARCHAR(30)  NULL COMMENT 'BRIGHT / CALM / RETRO',
    status        VARCHAR(20)  NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED / PROCESSING / DONE / FAILED',
    video_url     VARCHAR(500) NULL COMMENT '합성 결과 영상 URL',
    thumbnail_url VARCHAR(500) NULL,
    duration_ms   INT          NULL COMMENT '총 길이(ms)',
    resolution    VARCHAR(20)  NULL COMMENT '예: 1080x1920',
    error_message VARCHAR(500) NULL COMMENT '합성 실패 사유',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at  DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_pinlogs_trip (trip_id),
    CONSTRAINT fk_pinlogs_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_pinlogs_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='PinLog (미니 브이로그)';


CREATE TABLE pinlog_clips (
    id             BIGINT NOT NULL AUTO_INCREMENT COMMENT '클립 ID',
    pinlog_id      BIGINT NOT NULL COMMENT 'PinLog ID',
    diary_media_id BIGINT NOT NULL COMMENT '원본 미디어 ID (장소 기록의 사진/영상)',
    clip_order     INT    NOT NULL COMMENT '동선 순서 (1,2,3,4...)',
    start_ms       INT    NOT NULL DEFAULT 0 COMMENT '잘라낼 시작 지점(ms)',
    end_ms         INT    NULL COMMENT '잘라낼 종료 지점(ms), 기본 1초',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pinlog_clips_order (pinlog_id, clip_order),
    CONSTRAINT fk_pinlog_clips_pinlog FOREIGN KEY (pinlog_id)      REFERENCES pinlogs (id),
    CONSTRAINT fk_pinlog_clips_media  FOREIGN KEY (diary_media_id) REFERENCES diary_media (id)
) COMMENT='PinLog 구성 클립 (원본 미디어 참조)';


-- ---------------------------------------------------------
-- 8. 알림
-- ---------------------------------------------------------

CREATE TABLE notifications (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '알림 ID',
    user_id     BIGINT       NOT NULL COMMENT '받는 사용자 ID',
    type        VARCHAR(30)  NOT NULL COMMENT 'TRIP_INVITED / AI_EXTRACT_DONE / PINLOG_DONE / TRIP_REMINDER',
    title       VARCHAR(100) NOT NULL COMMENT '알림 제목',
    body        VARCHAR(500) NULL COMMENT '알림 본문',
    target_type VARCHAR(20)  NULL COMMENT '눌렀을 때 이동할 화면 TRIP / PINLOG / AI_REQUEST / INVITATION',
    target_id   BIGINT       NULL COMMENT '이동 대상 ID',
    is_read     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '읽음 여부',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at     DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_user (user_id, is_read, created_at),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='알림';
