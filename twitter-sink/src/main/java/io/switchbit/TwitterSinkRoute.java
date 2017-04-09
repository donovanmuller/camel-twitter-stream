package io.switchbit;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TwitterSinkRoute extends RouteBuilder {

    @Override
	public void configure() throws Exception {

        // we'll get the rendered reply content here
        from("scst:input")

                // and we'll tweet that out here.

                // wanna see it in action?
                // just retweet this tweet!

                // thanks!
                .to("twitter:timeline/user");
	}
}
