package com.example.pinkok_backend.storage;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.Locale;
import java.util.Map;

/**
 * Google Cloud Storage 버킷에 파일을 저장한다. 배포 서버(Cloud Run)용.
 *
 * <p>Cloud Run 의 디스크는 임시라서 서버가 재시작되면 uploads/ 폴더가 비워진다.
 * 그래서 배포 환경에서는 사진·영상을 버킷에 둔다.
 *
 * <p>버킷은 공개 읽기로 두고, 앱은 {@code https://storage.googleapis.com/버킷/경로} 주소로 파일을 바로 받는다.
 * 서버를 거치지 않으니 1GB 서버에 부담이 없고, 영상 구간 이동(탐색)도 Cloud Storage 가 알아서 처리한다.
 * 경로에 무작위 UUID 가 들어가서 주소를 추측할 수는 없다 (로컬의 /files/** 도 로그인 없이 열리는 것과 같은 수준).
 *
 * <p>어떤 저장소를 쓸지는 {@code file.storage} 설정으로 고른다 ({@link StorageConfig}).
 */
public class GcsFileStorage implements FileStorage {

    /** 한 번 올린 파일은 바뀌지 않으므로(경로가 매번 새 UUID) 폰이 오래 캐시해도 된다 */
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("heic", "image/heic"),
            Map.entry("heif", "image/heif"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("m4v", "video/x-m4v"),
            Map.entry("3gp", "video/3gpp"),
            Map.entry("webm", "video/webm")
    );

    private final Storage storage;
    private final String bucket;
    /** save 가 돌려주는 주소의 앞부분. 예: https://storage.googleapis.com/pinkok-files/ */
    private final String publicBaseUrl;

    public GcsFileStorage(Storage storage, String bucket, String publicBaseUrl) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("file.storage=gcs 이면 gcs.bucket 을 설정해야 합니다.");
        }
        this.storage = storage;
        this.bucket = bucket.trim();
        String base = (publicBaseUrl == null || publicBaseUrl.isBlank())
                ? "https://storage.googleapis.com/" + this.bucket
                : publicBaseUrl.trim();
        this.publicBaseUrl = base.endsWith("/") ? base : base + "/";
    }

    @Override
    public String save(InputStream in, String path) throws IOException {
        BlobInfo info = BlobInfo.newBuilder(blobId(path))
                .setContentType(contentTypeOf(path))
                .setCacheControl(CACHE_CONTROL)
                .build();
        // 영상처럼 큰 파일도 메모리에 통째로 올리지 않고 조금씩 흘려보낸다
        try (WriteChannel writer = storage.writer(info);
             OutputStream out = Channels.newOutputStream(writer)) {
            in.transferTo(out);
        } catch (StorageException e) {
            throw new IOException("Cloud Storage 에 저장하지 못했습니다: " + path, e);
        }
        return publicBaseUrl + path;
    }

    @Override
    public void delete(String path) throws IOException {
        try {
            storage.delete(blobId(path)); // 없는 파일이면 false 를 돌려줄 뿐 예외는 없다
        } catch (StorageException e) {
            throw new IOException("Cloud Storage 에서 지우지 못했습니다: " + path, e);
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            return blobOrNull(path) != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public long size(String path) throws IOException {
        return requireBlob(path).getSize();
    }

    @Override
    public byte[] read(String path) throws IOException {
        try {
            return storage.readAllBytes(blobId(path));
        } catch (StorageException e) {
            throw notFoundOr(e, path);
        }
    }

    @Override
    public InputStream openStream(String path) throws IOException {
        requireBlob(path);
        try {
            return Channels.newInputStream(storage.reader(blobId(path)));
        } catch (StorageException e) {
            throw notFoundOr(e, path);
        }
    }

    @Override
    public String pathOf(String url) {
        if (url == null || !url.startsWith(publicBaseUrl) || url.length() == publicBaseUrl.length()) {
            return null;
        }
        return url.substring(publicBaseUrl.length());
    }

    // ------------------------------------------------------------------

    private BlobId blobId(String path) throws IOException {
        // 로컬 저장소처럼 "../" 등으로 엉뚱한 위치를 가리키는 경로는 받지 않는다
        if (path == null || path.isBlank() || path.startsWith("/") || path.contains("..") || path.contains("\\")) {
            throw new IOException("허용되지 않은 저장 경로입니다: " + path);
        }
        return BlobId.of(bucket, path);
    }

    private Blob blobOrNull(String path) throws IOException {
        try {
            return storage.get(blobId(path));
        } catch (StorageException e) {
            throw new IOException("Cloud Storage 에서 파일 정보를 읽지 못했습니다: " + path, e);
        }
    }

    private Blob requireBlob(String path) throws IOException {
        Blob blob = blobOrNull(path);
        if (blob == null) {
            throw new FileNotFoundException("파일이 없습니다: " + path);
        }
        return blob;
    }

    private IOException notFoundOr(StorageException e, String path) {
        if (e.getCode() == 404) {
            return new FileNotFoundException("파일이 없습니다: " + path);
        }
        return new IOException("Cloud Storage 에서 읽지 못했습니다: " + path, e);
    }

    static String contentTypeOf(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return "application/octet-stream";
        }
        String extension = path.substring(dot + 1).toLowerCase(Locale.ROOT);
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }
}
