package io.switchbit;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ExpressionNode;
import org.apache.camel.spi.IdempotentRepository;
import org.springframework.stereotype.Component;

import io.switchbit.configuration.TwitterProperties;

@Component
public class TwitterSourceRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {
		// if you retweet this tweet, we'll get it here
		fromRetweets();

		// then we'll send it over the 'output' channel
		// using Spring Cloud Stream.
		// With the new Camel Spring Cloud Stream component!
		from("direct:retweets").to("scst:output");

	}






















	private TwitterProperties properties;
	private IdempotentRepository idempotentRepository;

	public TwitterSourceRoute(TwitterProperties properties,
							  IdempotentRepository idempotentRepository) {
		this.properties = properties;
		this.idempotentRepository = idempotentRepository;
	}


	private ExpressionNode fromRetweets() {
		return from("timer:pollRetweets?fixedRate=true&period={{twitter.pollPeriod}}")
				.setHeader("statusId", simple("{{twitter.statusId}}"))
				.to("bean:retweetService").split(body())
				.idempotentConsumer(simple("${body}"), idempotentRepository)
				.skipDuplicate(properties.getSkipDuplicate())
				.log("Received retweet from: @${body}").to("direct:retweets");
	}
}
