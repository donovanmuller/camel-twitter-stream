package io.switchbit;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TweetProcessorRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("scst:input")
                .setHeader("screenName", simple("${body}"))
                .to("mustache:tweet.mustache")
                .to("scst:output");
    }
}
