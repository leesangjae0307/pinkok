package com.example.pinkok_backend.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 파일을 "어디에" 저장할지 정해두는 약속.
 * 지금은 LocalFileStorage(서버 폴더), 배포할 때 클라우드용 구현체로 갈아끼운다.
 */
public interface FileStorage {

    /**
     * @param in   저장할 파일 내용
     * @param path 저장 경로 (예: 2026/09/abc.jpg)
     * @return 앱에서 파일을 열 수 있는 주소
     */
    String save(InputStream in, String path) throws IOException;

    /**
     * 저장해둔 파일을 지운다. 없는 파일이면 아무 일도 하지 않는다.
     *
     * @param path save 할 때 넘긴 저장 경로
     */
    void delete(String path) throws IOException;

    /** 저장해둔 파일이 실제로 있는지 확인한다. */
    boolean exists(String path);

    /**
     * save 가 돌려준 주소를 다시 저장 경로로 바꾼다.
     * 우리 저장소 주소가 아니면(외부 사이트 주소 등) null 을 돌려준다.
     */
    String pathOf(String url);
}
