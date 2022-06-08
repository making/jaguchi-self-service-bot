package lol.maki.jaguchi.selfservice.github;

import java.io.IOException;
import java.io.UncheckedIOException;

import lol.maki.jaguchi.selfservice.PullRequest;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequest.MergeMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.client.RestTemplate;

public class GitHubPullRequest implements PullRequest {
	private final Logger log = LoggerFactory.getLogger(GitHubPullRequest.class);

	private final GHPullRequest pullRequest;

	private final RestTemplate restTemplate;

	public GitHubPullRequest(GHPullRequest pullRequest, RestTemplate restTemplate) {
		this.pullRequest = pullRequest;
		this.restTemplate = restTemplate;
	}

	@Override
	public int getAdditions() {
		try {
			return this.pullRequest.getAdditions();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public int getDeletions() {
		try {
			return this.pullRequest.getDeletions();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void comment(String message) {
		try {
			this.pullRequest.comment(message);
		}
		catch (IOException e) {
			log.warn("Failed to comment", e);
		}
	}

	@Override
	public void mergeWithComment(String message) {
		try {
			this.pullRequest.comment(message);
			this.pullRequest.merge(null, null, MergeMethod.SQUASH);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to merge", e);
		}
	}

	@Override
	public String getDiff() {
		return this.restTemplate.getForObject(pullRequest.getDiffUrl().toString(), String.class);
	}
}
