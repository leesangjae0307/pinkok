package com.example.pinkok_backend.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 서버가 돌아가는 컴퓨터의 폴더(uploads/)에 파일을 저장한다.
 */
@Component
public class LocalFileStorage implements FileStorage {

    public static final String URL_PREFIX = "/files/";

    private final Path rootDir;

    public LocalFileStorage(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.rootDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String save(InputStream in, String path) throws IOException {
        Path target = resolve(path);
        Files.createDirectories(target.getParent());
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        return URL_PREFIX + path;
    }

    @Override
    public void delete(String path) throws IOException {
        Files.deleteIfExists(resolve(path));
    }

    @Override
    public boolean exists(String path) {
        try {
            return Files.isRegularFile(resolve(path));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String pathOf(String url) {
        if (url == null || !url.startsWith(URL_PREFIX) || url.length() == URL_PREFIX.length()) {
            return null;
        }
        return url.substring(URL_PREFIX.length());
    }

    private Path resolve(String path) throws IOException {
        Path target = rootDir.resolve(path).normalize();

        // "../" 같은 경로로 uploads 폴더 밖을 건드리는 것을 막는다
        if (!target.startsWith(rootDir)) {
            throw new IOException("허용되지 않은 저장 경로입니다: " + path);
        }
        return target;
    }

    public Path getRootDir() {
        return rootDir;
    }
}
