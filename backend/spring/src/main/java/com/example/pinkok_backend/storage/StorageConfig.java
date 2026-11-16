package com.example.pinkok_backend.storage;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 배포 서버에서 Cloud Storage 를 쓰도록 하는 설정. {@code file.storage=gcs} 일 때만 켜진다.
 *
 * <p>설정하지 않으면(로컬 개발) 지금처럼 {@link LocalFileStorage} 가 uploads/ 폴더에 저장한다.
 *
 * <p>Cloud Run 에서는 서버에 붙은 서비스 계정으로 자동 로그인되므로 키 파일이 필요 없다.
 * 그 서비스 계정에 버킷 쓰기 권한(Storage 개체 관리자)만 주면 된다.
 */
@Configuration
@ConditionalOnProperty(name = "file.storage", havingValue = "gcs")
public class StorageConfig {

    /** 실제로 쓰일 때 만든다 (테스트에서 가짜 저장소로 바꿔 끼우면 아예 만들어지지 않는다). */
    @Bean
    @Lazy
    public Storage googleCloudStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    @Bean
    public FileStorage gcsFileStorage(Storage storage,
                                      @Value("${gcs.bucket:}") String bucket,
                                      @Value("${gcs.public-base-url:}") String publicBaseUrl) {
        return new GcsFileStorage(storage, bucket, publicBaseUrl);
    }
}
