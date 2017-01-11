package io.switchbit.configuration;

import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.spring.processor.idempotent.SpringCacheIdempotentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Twitter configuration taken in part from
 * https://github.com/snicoll/spring-boot-starter-twitter4j
 */
@Configuration
@EnableConfigurationProperties(TwitterProperties.class)
public class TwitterSourceConfiguration {

	private TwitterProperties properties;

	public TwitterSourceConfiguration(TwitterProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public TwitterFactory twitterFactory() {
		if (this.properties.getConsumerKey() == null
				|| this.properties.getConsumerSecret() == null
				|| this.properties.getAccessToken() == null
				|| this.properties.getAccessTokenSecret() == null) {
			throw new RuntimeException(
					"Twitter properties not configured properly. Please check twitter.* properties settings in configuration file.");
		}

		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.setDebugEnabled(false)
				.setOAuthConsumerKey(properties.getConsumerKey())
				.setOAuthConsumerSecret(properties.getConsumerSecret())
				.setOAuthAccessToken(properties.getAccessToken())
				.setOAuthAccessTokenSecret(properties.getAccessTokenSecret());
		return new TwitterFactory(configurationBuilder.build());
	}

	@Bean
	@ConditionalOnMissingBean
	public Twitter twitter(TwitterFactory twitterFactory) {
		return twitterFactory.getInstance();
	}

	/**
	 * {@link IdempotentRepository} backed by Redis. Used to prevent retweets being
	 * processed more than once
	 */
	@Bean
	public IdempotentRepository springCacheIdempotentRepository(
			CacheManager cacheManager) {
		return new SpringCacheIdempotentRepository(cacheManager, "retweets");
	}
}
