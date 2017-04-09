package io.switchbit;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TweetProcessorRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // we'll get the retweeter here
        from("scst:input")

                // and render a reply tweet using a Mustache
                // template
                .to("mustache:tweet.mustache")

                // then, we'll send it out on the 'output' channel
                .to("scst:output");
    }
}
