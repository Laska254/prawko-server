package pl.prawko.prawko_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class PrawkoServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrawkoServerApplication.class, args);
	}

}
