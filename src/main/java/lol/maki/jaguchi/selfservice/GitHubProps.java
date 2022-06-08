package lol.maki.jaguchi.selfservice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "github")
@ConstructorBinding
public record GitHubProps(String accessToken, String organization, String team,
						  String originalContentUrl) {
}
