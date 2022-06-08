package lol.maki.jaguchi.selfservice;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.ApplicativeValidator;
import am.ik.yavi.core.ConstraintViolation;
import am.ik.yavi.fn.Validation;
import lol.maki.jaguchi.selfservice.PullRequestReviewService.ReviewOutput.Status;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableSet;

@Service
public class PullRequestReviewService {
	private final Logger log = LoggerFactory.getLogger(PullRequestReviewService.class);

	private final PullRequestRepository pullRequestRepository;

	private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

	final ApplicativeValidator<User> userValidator;


	final ApplicativeValidator<PullRequest> pullRequestValidator = ValidatorBuilder.<PullRequest>of()
			.constraint(PullRequest::getAdditions, "additions", c -> c.greaterThan(0))
			.constraint(PullRequest::getDeletions, "deletions", c -> c.equalTo(0))
			.build()
			.applicative();


	public PullRequestReviewService(PullRequestRepository pullRequestRepository) {
		this.pullRequestRepository = pullRequestRepository;
		this.userValidator = User.validator("jaguchi-users", pullRequestRepository::isTeamMember, this::existingUsernames, Set.of("edit", "view", "app-editor", "app-viewer"));
	}

	record ReviewInput(String repositoryName, int pullRequestNumber) {

	}

	record ReviewOutput(Status status, List<User> users) {
		enum Status {
			SKIPPED, SUCCESS
		}
	}

	public Validation<ConstraintViolation, ReviewOutput> review(ReviewInput input) {
		final PullRequest pr = this.pullRequestRepository.findPullRequest(input.repositoryName(), input.pullRequestNumber());
		final String diff = pr.getDiff();
		final String a = Arrays.stream(diff.split("\\R"))
				.filter(l -> l.startsWith("--- a/"))
				.map(l -> l.substring(6))
				.limit(1)
				.collect(joining());
		final String b = Arrays.stream(diff.split("\\R"))
				.filter(l -> l.startsWith("+++ b/"))
				.map(l -> l.substring(6))
				.limit(1)
				.collect(joining());
		log.info("a: {}, b: {}", a, b);
		if (!a.equals("jaguchi/config/platform/tap-users/tap-users-data-values.yaml") || !a.equals(b)) {
			return Validation.success(new ReviewOutput(Status.SKIPPED, List.of()));
		}
		return this.pullRequestValidator.validate(pr)
				.flatMap(pullRequest -> {
					final String addition = Arrays.stream(diff.split("\\R"))
							.filter(l -> l.matches("^[+{1}][^+].+"))
							.map(l -> l.substring(1))
							.collect(joining(System.lineSeparator()));
					try {
						final List<User> users = objectMapper.readValue(addition, new TypeReference<>() {
						});
						return this.userValidator.liftList().validate(users);
					}
					catch (IOException e) {
						pullRequest.comment("""
								🚨 **ERROR**
								Failed to parse the diff yaml
																	
								```yaml
								%s
								```
								""".formatted(addition));
						throw new UncheckedIOException("Failed to parse the diff yaml", e);
					}
				})
				.peek(__ -> pr.mergeWithComment("✅ **LGTM**"))
				.peekErrors(violations -> pr.comment("""
						🚨 **ERROR**
						%s
						""".formatted(violations.stream().map(v -> "* " + v.message()).collect(joining(System.lineSeparator())))))
				.map(users -> new ReviewOutput(Status.SUCCESS, users));
	}

	Set<String> existingUsernames() {
		try {
			final String yaml = this.pullRequestRepository.getOriginalContent();
			final Users existingUsers = this.objectMapper.readValue(yaml, Users.class);
			return existingUsers.users().stream().map(User::name).collect(toUnmodifiableSet());
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to retrieve existing usernames.", e);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Users(Set<User> users) {}

}