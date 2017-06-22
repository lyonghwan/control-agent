package xyz.elidom.control.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import de.codecentric.boot.admin.config.EnableAdminServer;

@EnableAdminServer
@EnableScheduling
@SpringBootApplication
@ImportResource({ "classpath:/WEB-INF/application-context.xml" })
public class ElidomControlAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ElidomControlAgentApplication.class, args);
	}
}
