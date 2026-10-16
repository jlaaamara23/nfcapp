package com.nfcattendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NfcAttendanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NfcAttendanceApplication.class, args);
    }
}
