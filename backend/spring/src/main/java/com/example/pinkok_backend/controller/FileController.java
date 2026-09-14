package com.example.pinkok_backend.controller;

import com.example.pinkok_backend.dto.FileUploadResponse;
import com.example.pinkok_backend.service.FileService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 사진·영상을 한 번에 1~10개 올리고 주소 목록을 돌려받는다.
     * 요청: multipart/form-data, 필드 이름 "file" 로 파일을 여러 개 담아 보낸다. (1개만 보내도 된다)
     * 응답: 올린 순서대로 담긴 배열
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<FileUploadResponse> upload(@RequestParam("file") List<MultipartFile> files) {
        return fileService.uploadAll(files);
    }
}
