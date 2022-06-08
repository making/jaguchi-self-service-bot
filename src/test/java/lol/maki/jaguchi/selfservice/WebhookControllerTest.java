package lol.maki.jaguchi.selfservice;

import java.util.List;
import java.util.Locale;

import am.ik.yavi.core.ConstraintViolation;
import am.ik.yavi.message.SimpleMessageFormatter;
import lol.maki.jaguchi.selfservice.PullRequestReviewService.ReviewInput;
import lol.maki.jaguchi.selfservice.PullRequestReviewService.ReviewOutput;
import lol.maki.jaguchi.selfservice.PullRequestReviewService.ReviewOutput.Status;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static am.ik.yavi.fn.Validation.failure;
import static am.ik.yavi.fn.Validation.success;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebhookControllerTest {

	@Test
	void webhookNoContent() throws Exception {
		final PullRequestReviewService pullRequestReviewService = mock(PullRequestReviewService.class);
		final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebhookController(pullRequestReviewService)).build();
		mockMvc.perform(MockMvcRequestBuilders.post("/webhook")
						.contentType(APPLICATION_JSON)
						.content("""
								{}
								"""))
				.andExpect(status().isNoContent());
	}

	@Test
	void webhookClosed() throws Exception {
		final PullRequestReviewService pullRequestReviewService = mock(PullRequestReviewService.class);
		final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebhookController(pullRequestReviewService)).build();
		mockMvc.perform(MockMvcRequestBuilders.post("/webhook")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "pull_request": {
								    "base": {
								      "repo": {
								        "full_name": "demo/foo"
								      }
								    },
								    "html_url": "https://example.com"
								  },
								  "number": 1,
								  "action": "closed"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOSED"));
	}

	@Test
	void webhookSkipped() throws Exception {
		final PullRequestReviewService pullRequestReviewService = mock(PullRequestReviewService.class);
		given(pullRequestReviewService.review(new ReviewInput("demo/foo", 1)))
				.willReturn(success(new ReviewOutput(Status.SKIPPED, List.of())));
		final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebhookController(pullRequestReviewService)).build();
		mockMvc.perform(MockMvcRequestBuilders.post("/webhook")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "pull_request": {
								    "base": {
								      "repo": {
								        "full_name": "demo/foo"
								      }
								    },
								    "html_url": "https://example.com"
								  },
								  "number": 1,
								  "action": "open"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SKIPPED"))
				.andExpect(jsonPath("$.users").isEmpty());
	}

	@Test
	void webhookSuccess() throws Exception {
		final PullRequestReviewService pullRequestReviewService = mock(PullRequestReviewService.class);
		given(pullRequestReviewService.review(new ReviewInput("demo/foo", 1)))
				.willReturn(success(new ReviewOutput(Status.SUCCESS, List.of(new User("foo", "foo@example.com", List.of("edit", "view"))))));
		final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebhookController(pullRequestReviewService)).build();
		mockMvc.perform(MockMvcRequestBuilders.post("/webhook")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "pull_request": {
								    "base": {
								      "repo": {
								        "full_name": "demo/foo"
								      }
								    },
								    "html_url": "https://example.com"
								  },
								  "number": 1,
								  "action": "open"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUCCESS"))
				.andExpect(jsonPath("$.users").isArray())
				.andExpect(jsonPath("$.users.length()").value(1))
				.andExpect(jsonPath("$.users[0].name").value("foo"))
				.andExpect(jsonPath("$.users[0].email").value("foo@example.com"))
				.andExpect(jsonPath("$.users[0].clusterroles").isArray())
				.andExpect(jsonPath("$.users[0].clusterroles.length()").value(2))
				.andExpect(jsonPath("$.users[0].clusterroles[0]").value("edit"))
				.andExpect(jsonPath("$.users[0].clusterroles[1]").value("view"));
	}


	@Test
	void webhookError() throws Exception {
		final PullRequestReviewService pullRequestReviewService = mock(PullRequestReviewService.class);
		given(pullRequestReviewService.review(new ReviewInput("demo/foo", 1)))
				.willReturn(failure(List.of(new ConstraintViolation("name", "notNull", "\"{0}\" must not be blank.", new Object[] { "name", "" }, new SimpleMessageFormatter(), Locale.ENGLISH))));
		final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebhookController(pullRequestReviewService)).build();
		mockMvc.perform(MockMvcRequestBuilders.post("/webhook")
						.contentType(APPLICATION_JSON)
						.content("""
								{
								  "pull_request": {
								    "base": {
								      "repo": {
								        "full_name": "demo/foo"
								      }
								    },
								    "html_url": "https://example.com"
								  },
								  "number": 1,
								  "action": "open"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value("ERROR"))
				.andExpect(jsonPath("$.details").isArray())
				.andExpect(jsonPath("$.details.length()").value(1))
				.andExpect(jsonPath("$.details[0].defaultMessage").value("\"name\" must not be blank."));
	}
}