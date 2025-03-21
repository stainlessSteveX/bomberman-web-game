package com.bomberman;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.WebSocketHandler;

@SpringBootApplication
public class BombermanServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BombermanServerApplication.class, args);
    }

    @Bean
    public WebSocketHandler myWebSocketHandler() {
        return new GameSocketHandler();
    }

}


