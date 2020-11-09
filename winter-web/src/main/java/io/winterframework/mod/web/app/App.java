package io.winterframework.mod.web.app;

import java.io.IOException;
import java.util.function.Supplier;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.v1.Application;
import io.winterframework.mod.configuration.ConfigurationSource;
import io.winterframework.mod.configuration.source.ApplicationConfigurationSource;
import io.winterframework.mod.web.Web;

public class App {
	
	@Bean
	public static interface AppConfigurationsource extends Supplier<ConfigurationSource<?, ?, ?>> {}

	public static void main(String[] args) throws IllegalStateException, IOException {
		Application.with(new Web.Builder()
			.setAppConfigurationsource(new ApplicationConfigurationSource(App.class.getModule(), args))
		).run();
	}
}
