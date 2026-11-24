package com.example.pinkok_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class PinkokBackendApplication {

    public static void main(String[] args) {
        // 서버가 내려주는 시각(createdAt 등, 시간대 표시 없는 LocalDateTime)은 항상 UTC 다.
        // 이걸 고정하지 않으면 한국 PC 에서 돌릴 때는 KST, Cloud Run 에서는 UTC 로 서로 달라진다.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(PinkokBackendApplication.class, args);
    }

}
