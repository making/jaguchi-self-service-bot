package lol.maki.jaguchi.selfservice;

class MockPullRequestRepository implements PullRequestRepository {
	private String comment;

	private boolean merged = false;

	private final String originalContent;

	private final String diff;

	private final int additions;

	private final int deletions;

	public MockPullRequestRepository(String originalContent, String diff, int additions, int deletions) {
		this.originalContent = originalContent;
		this.diff = diff;
		this.additions = additions;
		this.deletions = deletions;
	}

	public String getComment() {
		return comment;
	}

	public boolean isMerged() {
		return merged;
	}

	@Override
	public PullRequest findPullRequest(String repositoryName, int pullRequestNumber) {
		return new PullRequest() {
			@Override
			public int getAdditions() {
				return additions;
			}

			@Override
			public int getDeletions() {
				return deletions;
			}

			@Override
			public void comment(String message) {
				System.out.println(message);
				comment = message;
			}

			@Override
			public void mergeWithComment(String message) {
				System.out.println(message);
				comment = message;
				merged = true;
			}

			@Override
			public String getDiff() {
				return diff;
			}
		};
	}

	@Override
	public String getOriginalContent() {
		return originalContent;
	}

	@Override
	public boolean isTeamMember(String username) {
		return true;
	}
}
