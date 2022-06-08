package lol.maki.jaguchi.selfservice.github;

import java.io.IOException;
import java.io.UncheckedIOException;

import lol.maki.jaguchi.selfservice.GitHubProps;
import lol.maki.jaguchi.selfservice.PullRequest;
import lol.maki.jaguchi.selfservice.PullRequestRepository;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GitHubPullRequestRepository implements PullRequestRepository {
	private final Logger log = LoggerFactory.getLogger(GitHubPullRequestRepository.class);

	private final GitHub github;

	private final RestTemplate restTemplate;

	private final GitHubProps gitHubProps;


	public GitHubPullRequestRepository(GitHub github, RestTemplateBuilder restTemplateBuilder, GitHubProps gitHubProps) {
		this.github = github;
		this.restTemplate = restTemplateBuilder.build();
		this.gitHubProps = gitHubProps;
	}

	@Override
	public PullRequest findPullRequest(String repositoryName, int pullRequestNumber) {
		try {
			final GHRepository repository = this.github.getRepository(repositoryName);
			final GHPullRequest pr = repository.getPullRequest(pullRequestNumber);
			return new GitHubPullRequest(pr, this.restTemplate);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public boolean isTeamMember(String username) {
		try {
			final GHTeam team = this.github.getOrganization(this.gitHubProps.organization()).getTeamByName(this.gitHubProps.team());
			final GHUser githubUser = this.github.getUser(username);
			return team.hasMember(githubUser);
		}
		catch (IOException e) {
			log.error("Failed to check 'isTeamMember'", e);
			return false;
		}
	}

	@Override
	public String getOriginalContent() {
		return this.restTemplate.getForObject(this.gitHubProps.originalContentUrl(), String.class);
	}
}
