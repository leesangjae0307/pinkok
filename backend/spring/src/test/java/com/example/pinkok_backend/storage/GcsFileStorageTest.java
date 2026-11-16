package com.example.pinkok_backend.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cloud Storage 저장소. 진짜 버킷 대신 메모리에서 흉내 내는 가짜 저장소(LocalStorageHelper)로 확인한다.
 */
class GcsFileStorageTest {

    private static final String BUCKET = "pinkok-test";

    private Storage storage;
    private GcsFileStorage fileStorage;

    @BeforeEach
    void setUp() {
        storage = LocalStorageHelper.getOptions().getService();
        fileStorage = new GcsFileStorage(storage, BUCKET, "");
    }

    private static InputStream bytes(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("저장하면 버킷 공개 주소를 돌려주고, 같은 경로로 읽고·크기 재고·지울 수 있다")
    void saveReadDelete() throws IOException {
        String url = fileStorage.save(bytes("hello"), "2026/11/abc.jpg");

        assertThat(url).isEqualTo("https://storage.googleapis.com/pinkok-test/2026/11/abc.jpg");
        assertThat(fileStorage.pathOf(url)).isEqualTo("2026/11/abc.jpg");
        assertThat(fileStorage.exists("2026/11/abc.jpg")).isTrue();
        assertThat(fileStorage.read("2026/11/abc.jpg")).asString(StandardCharsets.UTF_8).isEqualTo("hello");
        assertThat(fileStorage.size("2026/11/abc.jpg")).isEqualTo(5);
        try (InputStream in = fileStorage.openStream("2026/11/abc.jpg")) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hello");
        }

        fileStorage.delete("2026/11/abc.jpg");
        assertThat(fileStorage.exists("2026/11/abc.jpg")).isFalse();
        fileStorage.delete("2026/11/abc.jpg"); // 없는 파일을 지워도 문제없다
    }

    @Test
    @DisplayName("파일 종류(Content-Type)와 캐시 설정을 붙여 저장한다 — 앱이 영상을 바로 재생할 수 있도록")
    void contentType() throws IOException {
        fileStorage.save(bytes("video"), "pinlogs/2026/11/v.mp4");
        fileStorage.save(bytes("photo"), "2026/11/p_thumb.jpg");

        Blob video = storage.get(BlobId.of(BUCKET, "pinlogs/2026/11/v.mp4"));
        assertThat(video.getContentType()).isEqualTo("video/mp4");
        assertThat(video.getCacheControl()).contains("max-age");
        assertThat(storage.get(BlobId.of(BUCKET, "2026/11/p_thumb.jpg")).getContentType()).isEqualTo("image/jpeg");
        assertThat(GcsFileStorage.contentTypeOf("a.MOV")).isEqualTo("video/quicktime");
        assertThat(GcsFileStorage.contentTypeOf("noext")).isEqualTo("application/octet-stream");
    }

    @Test
    @DisplayName("없는 파일을 읽으면 FileNotFoundException, 우리 버킷 주소가 아니면 pathOf 는 null")
    void missingAndForeign() {
        assertThat(fileStorage.exists("2026/11/none.jpg")).isFalse();
        assertThatThrownBy(() -> fileStorage.size("2026/11/none.jpg")).isInstanceOf(FileNotFoundException.class);
        assertThatThrownBy(() -> fileStorage.read("2026/11/none.jpg")).isInstanceOf(FileNotFoundException.class);
        assertThatThrownBy(() -> fileStorage.openStream("2026/11/none.jpg")).isInstanceOf(FileNotFoundException.class);

        assertThat(fileStorage.pathOf("https://example.com/a.jpg")).isNull();
        assertThat(fileStorage.pathOf("/files/2026/11/a.jpg")).isNull();
        assertThat(fileStorage.pathOf("https://storage.googleapis.com/other-bucket/a.jpg")).isNull();
        assertThat(fileStorage.pathOf(null)).isNull();
    }

    @Test
    @DisplayName("../ 처럼 엉뚱한 위치를 가리키는 경로는 거부한다")
    void rejectsBadPath() {
        assertThatThrownBy(() -> fileStorage.save(bytes("x"), "../secret.jpg")).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> fileStorage.save(bytes("x"), "/abs.jpg")).isInstanceOf(IOException.class);
        assertThat(fileStorage.exists("../secret.jpg")).isFalse();
    }

    @Test
    @DisplayName("공개 주소 앞부분을 따로 지정할 수 있고(CDN 등), 버킷 이름이 없으면 서버가 켜지지 않는다")
    void configuration() throws IOException {
        GcsFileStorage custom = new GcsFileStorage(storage, BUCKET, "https://cdn.pinkok.example");
        String url = custom.save(bytes("x"), "2026/11/a.png");
        assertThat(url).isEqualTo("https://cdn.pinkok.example/2026/11/a.png");
        assertThat(custom.pathOf(url)).isEqualTo("2026/11/a.png");

        assertThatThrownBy(() -> new GcsFileStorage(storage, " ", ""))
                .isInstanceOf(IllegalStateException.class);
    }
}
