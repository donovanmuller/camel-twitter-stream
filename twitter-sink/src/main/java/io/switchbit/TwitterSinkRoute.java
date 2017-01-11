package io.switchbit;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TwitterSinkRoute extends RouteBuilder {

    @Override
	public void configure() throws Exception {
        from("scst:input")
                .to("twitter:timeline/user");
	}
}
