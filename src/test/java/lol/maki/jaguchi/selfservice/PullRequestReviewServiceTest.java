package lol.maki.jaguchi.selfservice;

import java.util.List;
import java.util.Set;

import am.ik.yavi.core.ConstraintViolation;
import am.ik.yavi.fn.Validation;
import lol.maki.jaguchi.selfservice.PullRequestReviewService.ReviewInput;
import lol.maki.jaguchi.selfservice.PullRequestReviewService.ReviewOutput;
import lol.maki.jaguchi.selfservice.PullRequestReviewService.ReviewOutput.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PullRequestReviewServiceTest {

	@Test
	void reviewDeletions() {
		final MockPullRequestRepository mockPullRequestRepository = new MockPullRequestRepository("""
				""", """
				--- a/jaguchi/config/platform/tap-users/tap-users-data-values.yaml
				+++ b/jaguchi/config/platform/tap-users/tap-users-data-values.yaml
				""", 1, 1);
		final PullRequestReviewService pullRequestReviewService = new PullRequestReviewService(mockPullRequestRepository);
		final Validation<ConstraintViolation, ReviewOutput> validated = pullRequestReviewService.review(new ReviewInput("foo", 1));
		assertThat(validated.isValid()).isFalse();
		final List<ConstraintViolation> errors = validated.errors();
		assertThat(errors).hasSize(1);
		assertThat(errors.get(0).message()).isEqualTo("\"deletions\" must be equal to 0");
		assertThat(mockPullRequestRepository.getComment()).isEqualTo("""
				🚨 **ERROR**
				* "deletions" must be equal to 0
				""");
	}

	@Test
	void reviewNoAdditions() {
		final MockPullRequestRepository mockPullRequestRepository = new MockPullRequestRepository("""
				""", """
				--- a/jaguchi/config/platform/tap-users/tap-users-data-values.yaml
				+++ b/jaguchi/config/platform/tap-users/tap-users-data-values.yaml
				""", 0, 0);
		final PullRequestReviewService pullRequestReviewService = new PullRequestReviewService(mockPullRequestRepository);
		final Validation<ConstraintViolation, ReviewOutput> validated = pullRequestReviewService.review(new ReviewInput("foo", 1));
		assertThat(validated.isValid()).isFalse();
		final List<ConstraintViolation> errors = validated.errors();
		assertThat(errors).hasSize(1);
		assertThat(errors.get(0).message()).isEqualTo("\"additions\" must be greater than 0");
		assertThat(mockPullRequestRepository.getComment()).isEqualTo("""
				🚨 **ERROR**
				* "additions" must be greater than 0
				""");
	}

	@Test
	void reviewDifferentFile() {
		final MockPullRequestRepository mockPullRequestRepository = new MockPullRequestRepository("""
				""", """
				--- a/jaguchi/test.yaml
				+++ b/jaguchi/test.yaml
				""", 1, 0);
		final PullRequestReviewService pullRequestReviewService = new PullRequestReviewService(mockPullRequestRepository);
		final Validation<ConstraintViolation, ReviewOutput> validated = pullRequestReviewService.review(new ReviewInput("foo", 1));
		assertThat(validated.isValid()).isTrue();
		final ReviewOutput reviewOutput = validated.value();
		assertThat(reviewOutput.status()).isEqualTo(Status.SKIPPED);
	}

	@Test
	void review() {
		final MockPullRequestRepository mockPullRequestRepository = new MockPullRequestRepository("""
				users:
				- name: foo
				  email: foo@example.com
				  clusterroles:
				  - edit
				- name: bar
				  email: bar@example.com
				  clusterroles:
				  - edit
				""", """
				--- a/jaguchi/config/platform/tap-users/tap-users-data-values.yaml
				+++ b/jaguchi/config/platform/tap-users/tap-users-data-values.yaml
				+ - name: demo
				+   email: demo@example.com
				+   clusterroles:
				+   - edit
				+   - view
				""", 1, 0);
		final PullRequestReviewService pullRequestReviewService = new PullRequestReviewService(mockPullRequestRepository);
		final Validation<ConstraintViolation, ReviewOutput> validated = pullRequestReviewService.review(new ReviewInput("foo", 1));
		assertThat(validated.isValid()).isTrue();
		final ReviewOutput reviewOutput = validated.value();
		assertThat(reviewOutput.status()).isEqualTo(Status.SUCCESS);
		assertThat(reviewOutput.users()).hasSize(1);
		final User user = reviewOutput.users().get(0);
		assertThat(user.name()).isEqualTo("demo");
		assertThat(user.email()).isEqualTo("demo@example.com");
		assertThat(user.clusterRoles()).containsExactly("edit", "view");
		assertThat(mockPullRequestRepository.getComment()).isEqualTo("✅ **LGTM**");
	}

	@Test
	void existingUsernames() {
		final MockPullRequestRepository mockPullRequestRepository = new MockPullRequestRepository("""
				users:
				- name: foo
				  email: foo@example.com
				  clusterroles: []
				- name: bar
				  email: bar@example.com
				  clusterroles: []
				""", """
				""", 1, 0);
		final PullRequestReviewService pullRequestReviewService = new PullRequestReviewService(mockPullRequestRepository);
		final Set<String> usernames = pullRequestReviewService.existingUsernames();
		assertThat(usernames).containsExactlyInAnyOrder("foo", "bar");
	}

}