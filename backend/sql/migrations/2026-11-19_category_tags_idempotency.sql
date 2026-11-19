-- 이미 운영 중인 DB(로컬 MySQL / 배포 Cloud SQL)에 이번 변경을 반영하는 SQL.
-- 새로 만드는 DB 는 pinkok_schema.sql 에 이미 들어 있으므로 실행할 필요 없다.
-- ddl-auto=update 로 서버가 이미 만들었다면 "Duplicate column / Table already exists" 오류가 나는데, 그러면 이미 반영된 것이니 무시하면 된다.

-- 1) 핀 단위 장소 종류 변경 (NULL 이면 장소 기본 분류)
ALTER TABLE itinerary_items
    ADD COLUMN category_override VARCHAR(10) NULL
        COMMENT '이 핀에서만 바꾼 장소 종류 (FD6/CE7/AD5/AT4/NATURE/SEA/CT1/MT1/ETC). NULL 이면 장소 기본 분류'
        AFTER memo;

-- 2) 여행 자유 해시태그
CREATE TABLE trip_tags (
    trip_id    BIGINT      NOT NULL COMMENT '여행 ID',
    sort_order INT         NOT NULL COMMENT '태그 순서 (0부터)',
    tag        VARCHAR(20) NOT NULL COMMENT '자유 해시태그 (# 없이)',
    PRIMARY KEY (trip_id, sort_order),
    CONSTRAINT fk_trip_tags_trip FOREIGN KEY (trip_id) REFERENCES trips (id)
) COMMENT='여행 자유 해시태그';

-- 3) 기록 저장 멱등 키
ALTER TABLE diaries
    ADD COLUMN client_request_id VARCHAR(64) NULL
        COMMENT '앱이 저장 요청마다 붙인 고유 값 (재시도 시 같은 기록을 돌려주기 위한 멱등 키)'
        AFTER rating;
