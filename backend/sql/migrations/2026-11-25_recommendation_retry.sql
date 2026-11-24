-- 이미 운영 중인 DB(로컬 MySQL / 배포 Cloud SQL)에 이번 변경을 반영하는 SQL.
-- 새로 만드는 DB 는 pinkok_schema.sql 에 이미 들어 있으므로 실행할 필요 없다.
-- ddl-auto=update 로 서버가 이미 만들었다면 "Duplicate column" 오류가 나는데, 그러면 이미 반영된 것이니 무시하면 된다.

-- 맞춤 추천 다시 시도: 요청 조건(동행·지역·스타일·개수)을 남겨 둔다. 이 칼럼이 비어 있는 옛 추천 요청은 다시 시도할 수 없다.
ALTER TABLE ai_requests
    ADD COLUMN request_options TEXT NULL
        COMMENT '요청 조건 JSON (맞춤 추천 다시 시도용: 동행·지역·스타일·개수)'
        AFTER raw_response;
