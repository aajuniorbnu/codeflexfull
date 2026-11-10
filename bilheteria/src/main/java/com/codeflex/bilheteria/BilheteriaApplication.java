package com.codeflex.bilheteria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BilheteriaApplication {
  public static void main(String[] args) {
    SpringApplication.run(BilheteriaApplication.class, args);
  }
}
