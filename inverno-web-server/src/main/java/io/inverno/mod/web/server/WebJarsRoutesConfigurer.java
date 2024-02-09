/*
 * Copyright 2021 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.web.server;

import io.inverno.mod.base.resource.ModuleResource;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * Web routes configurer used to configure routes exposing WebJars resources deployed on the module path or class path.
 * </p>
 *
 * <p>
 * This configurer defines as many routes as there are WebJars defined on the module path or class path. It assumes a webjar provides static content under
 * <code>/META-INF/resources/webjars/{@literal <name>}/{@literal <version>}</code> as defined by <a href="https://www.webjars.org">WebJars</a>.
 * </p>
 *
 * <p>
 * For instance assuming {@code example-webjar} is on the classpath in version {@code 1.2.3}, its resource can be accessed at {@code /webjars/example-webjar/*}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @param <A> the exchange context type
 */
public class WebJarsRoutesConfigurer<A extends ExchangeContext> implements WebRoutesConfigurer<A> {
	
	private static final Logger LOGGER = LogManager.getLogger(WebJarsRoutesConfigurer.class);
	
	private static final String WEBJARS_MODULE_PREFIX = "org.webjars.";
	private static final int WEBJARS_MODULE_PREFIX_LENGTH = WEBJARS_MODULE_PREFIX.length();
	private static final String NPM_PREFIX = "npm.";
	private static final int NPM_PREFIX_LENGTH = NPM_PREFIX.length();
	private static final String BOWER_PREFIX = "bower.";
	private static final int BOWER_PREFIX_LENGTH = BOWER_PREFIX.length();
	private static final String BOWERGITHUB_PREFIX = "bowergithub.";
	private static final int BOWERGITHUB_PREFIX_LENGTH = BOWERGITHUB_PREFIX.length();

	private static final String BASE_WEBJARS_PATH = "/webjars";
	
	private final ResourceService resourceService;
	
	/**
	 * <p>
	 * Creates a WebJars web routes configurer with the specified resource service.
	 * </p>
	 * 
	 * @param resourceService the resource service
	 */
	public WebJarsRoutesConfigurer(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	@Override
	public void configure(WebRoutable<A, ?> routes) {
		/* 2 possibilities:
		 * - modular webjar
		 *   - /[module_name]/webjars/* -> module://[module_name]/META-INF/resources/webjars/[module_name]/[module_version]/*
		 *   => WE HAVE TO modify the modularized webjars so that the path is correct
		 * - no modular webjar (ie. we haven't found the requested module) fallback to classpath
		 *   - /[module_name]/webjars/* -> classpath:/META-INF/resources/webjars
		 */

		final Set<String> webjarNames = new HashSet<>();

		ModuleLayer moduleLayer = this.getClass().getModule().getLayer();
		if(moduleLayer != null) {
			moduleLayer.modules().stream().filter(module -> module.getName().startsWith(WEBJARS_MODULE_PREFIX) && module.getDescriptor().rawVersion().isPresent()).forEach(module -> {
				String webjarName = module.getDescriptor().name().substring(WEBJARS_MODULE_PREFIX_LENGTH);
				if (webjarName.startsWith(NPM_PREFIX)) {
					// org.webjars.npm.<name>
					webjarName = webjarName.substring(NPM_PREFIX_LENGTH);
				} else if (webjarName.startsWith(BOWER_PREFIX)) {
					// org.webjars.bower.<name>
					webjarName = webjarName.substring(BOWER_PREFIX_LENGTH);
				} else if (webjarName.startsWith(BOWERGITHUB_PREFIX)) {
					// org.webjars.bowergithub.<org|user>.<name>
					webjarName = webjarName.substring(BOWERGITHUB_PREFIX_LENGTH);
					webjarName = webjarName.substring(webjarName.indexOf('.') + 1);
				}
				if(webjarNames.add(webjarName)) {
					String webjarVersion = module.getDescriptor().rawVersion().get();
					Resource baseResource = this.resourceService.getResource(URI.create(ModuleResource.SCHEME_MODULE + "://" + module.getName() + "/META-INF/resources/webjars/" + webjarName + "/" + webjarVersion + "/"));
					String webjarRootPath = WebJarsRoutesConfigurer.BASE_WEBJARS_PATH + "/" + webjarName + "/{path:.*}";
					LOGGER.debug(() -> "Registered Webjar " + webjarRootPath + " -> " + baseResource.getURI());
					routes.route()
						.path(webjarRootPath)
						.method(Method.GET)
						.handler(new StaticHandler<>(baseResource));
				}
			});
		}

		this.resourceService.getResources(URI.create("classpath:/META-INF/resources/webjars"))
			.flatMap(resource -> {
				return this.resourceService.getResources(URI.create(resource.getURI().toString() + "/*/*"));
			})
			.forEach(baseResource -> {
				String spec = baseResource.getURI().getSchemeSpecificPart();
				int versionIndex = spec.lastIndexOf("/");
				int webjarIndex = spec.substring(0, versionIndex).lastIndexOf("/");

				String webjarName = toModuleName(spec.substring(webjarIndex + 1, versionIndex));
				if(webjarNames.add(webjarName)) {
					String webjarRootPath = WebJarsRoutesConfigurer.BASE_WEBJARS_PATH + "/" + webjarName + "/{path:.*}";
					LOGGER.debug(() -> "Registered Webjar " + webjarRootPath + " -> " + baseResource.getURI());
					routes.route()
						.path(webjarRootPath)
						.method(Method.GET)
						.handler(new StaticHandler<>(baseResource));
				}
			});
	}

	private static final Pattern NON_ALPHANUM = Pattern.compile("[^A-Za-z0-9]");
	private static final Pattern REPEATING_DOTS = Pattern.compile("(\\.)(\\1)+");
	private static final Pattern LEADING_DOTS = Pattern.compile("^\\.");
	private static final Pattern TRAILING_DOTS = Pattern.compile("\\.$");

	/*
	 * Borrowed from jdk.internal.module.ModulePath#cleanModuleName
	 */
	private static String toModuleName(String mn) {
		// replace non-alphanumeric
		mn = WebJarsRoutesConfigurer.NON_ALPHANUM.matcher(mn).replaceAll(".");

		// collapse repeating dots
		mn = WebJarsRoutesConfigurer.REPEATING_DOTS.matcher(mn).replaceAll(".");

		// drop leading dots
		if (!mn.isEmpty() && mn.charAt(0) == '.') {
			mn = WebJarsRoutesConfigurer.LEADING_DOTS.matcher(mn).replaceAll("");
		}

		// drop trailing dots
		int len = mn.length();
		if (len > 0 && mn.charAt(len - 1) == '.') {
			mn = WebJarsRoutesConfigurer.TRAILING_DOTS.matcher(mn).replaceAll("");
		}

		return mn;
	}
}
