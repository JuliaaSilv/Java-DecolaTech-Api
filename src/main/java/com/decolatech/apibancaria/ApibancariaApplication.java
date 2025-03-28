package com.decolatech.apibancaria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(servers = {@Server(url = "/", description = "Default Server URL")})
@SpringBootApplication
public class ApibancariaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApibancariaApplication.class, args);
	}

}
