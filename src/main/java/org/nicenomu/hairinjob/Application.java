package org.nicenomu.hairinjob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Application {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(Application.class);
		// CLI 애플리케이션이므로 웹 서버 비활성화
		app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
		System.exit(SpringApplication.exit(app.run(args)));
	}

}
