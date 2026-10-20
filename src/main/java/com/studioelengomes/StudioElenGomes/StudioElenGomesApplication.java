package com.studioelengomes.StudioElenGomes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StudioElenGomesApplication {

	public static void main(String[] args) {
		// O studio fica em São Paulo: "hoje", "amanhã" e os horários seguem esse fuso,
		// mesmo quando o servidor (Render, Railway...) roda em UTC.
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Sao_Paulo"));
		SpringApplication.run(StudioElenGomesApplication.class, args);
	}

}
