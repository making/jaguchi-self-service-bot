package lol.maki.jaguchi.selfservice;

public interface PullRequestRepository {
	PullRequest findPullRequest(String repositoryName, int pullRequestNumber);


	String getOriginalContent();

	boolean isTeamMember(String username);
}
