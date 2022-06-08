package lol.maki.jaguchi.selfservice;

import java.io.IOException;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JaguchiSelfServiceBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(JaguchiSelfServiceBotApplication.class, args);
	}

	@Bean
	public GitHub gitHub(GitHubProps props) throws IOException {
		return new GitHubBuilder().withOAuthToken(props.accessToken()).build();
	}
}
