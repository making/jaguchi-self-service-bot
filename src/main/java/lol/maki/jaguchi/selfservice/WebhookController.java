package lol.maki.jaguchi.selfservice;

import java.util.Map;

import am.ik.yavi.core.ConstraintViolation;
import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.fn.Validation;
import lol.maki.jaguchi.selfservice.PullRequestReviewService.ReviewInput;
import lol.maki.jaguchi.selfservice.PullRequestReviewService.ReviewOutput;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {
	private final Logger log = LoggerFactory.getLogger(WebhookController.class);

	private final PullRequestReviewService pullRequestReviewService;

	public WebhookController(PullRequestReviewService pullRequestReviewService) {
		this.pullRequestReviewService = pullRequestReviewService;
	}


	@PostMapping(path = "/webhook")
	public ResponseEntity<?> webhook(RequestEntity<JsonNode> request) {
		final JsonNode body = request.getBody();
		if (body == null || !body.has("pull_request")) {
			return ResponseEntity.noContent().build();
		}
		final String repo = body.get("pull_request").get("base").get("repo").get("full_name").asText();
		final int number = body.get("number").asInt();
		final String action = body.get("action").asText();
		log.info("Received a webhook: {}\t{}", action, body.get("pull_request").get("html_url").asText());
		if ("closed".equals(action)) {
			return ResponseEntity.ok(Map.of("status", "CLOSED"));
		}
		final ReviewInput input = new ReviewInput(repo, number);
		final Validation<ConstraintViolation, ReviewOutput> reviewed = this.pullRequestReviewService.review(input);
		return reviewed.fold(
				violations -> ResponseEntity.badRequest().body(Map.of("status", "ERROR", "details", ConstraintViolations.of(violations).details())),
				output -> ResponseEntity.ok(Map.of("status", output.status(), "users", output.users())));
	}
}