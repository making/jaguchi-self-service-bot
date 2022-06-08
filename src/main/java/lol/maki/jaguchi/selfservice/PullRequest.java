package lol.maki.jaguchi.selfservice;

public interface PullRequest {
	int getAdditions();

	int getDeletions();

	void comment(String message);

	void mergeWithComment(String message);

	String getDiff();
}
