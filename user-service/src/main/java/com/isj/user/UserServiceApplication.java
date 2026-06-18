package com.isj.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = {"com.isj.user", "com.isj.common"}) // Bean 자동 등록 및 스캔 범위 지정
@EnableDiscoveryClient
@EnableJpaAuditing // @CreatedDate, @LastModifiedDat 자동 주입 활성화
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
