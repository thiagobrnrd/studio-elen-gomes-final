package com.studioelengomes.StudioElenGomes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "DATABASE_URL", matches = ".+")
class StudioElenGomesApplicationTests {

	@Test
	void contextLoads() {
	}

}
