package io.switchbit;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Handler;
import org.apache.camel.Header;
import org.springframework.stereotype.Service;

import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Get the retweets of a specific tweet (Status) and return a List of screen names of
 * those retweeters
 */
@Service
public class RetweetService {

	private Twitter twitter;

	public RetweetService(Twitter twitter) {
		this.twitter = twitter;
	}

	@Handler
	public List<String> getRetweetsWithScreenNames(@Header("statusId") Long statusId)
			throws TwitterException {
		return twitter.getRetweets(statusId).stream()
				.map(tweet -> tweet.getUser().getScreenName())
				.collect(Collectors.toList());
	}
}
