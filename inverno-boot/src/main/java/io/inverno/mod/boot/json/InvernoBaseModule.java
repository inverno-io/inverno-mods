/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.boot.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.Module;
import io.inverno.mod.boot.internal.json.InvernoBaseBeanSerializerModifier;
import io.inverno.mod.boot.internal.json.InvernoBaseSerializers;
import io.inverno.mod.boot.internal.json.InvernoBaseTypeModifier;
import java.lang.module.ModuleDescriptor;
import java.util.Optional;

/**
 * <p>
 *
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class InvernoBaseModule extends Module {

	protected boolean _cfgWriteAbsentAsNull = false;

	@Override
	public String getModuleName() {
		return "base";
	}

	@Override
	public Version version() {
		return Optional.ofNullable(this.getClass().getModule().getDescriptor())
			.flatMap(ModuleDescriptor::version)
			.map(version -> VersionUtil.parseVersion(version.toString(), "io.inverno.mod", "base"))
			.orElse(Version.unknownVersion());
	}

	@Override
	public void setupModule(SetupContext context) {
		context.addSerializers(new InvernoBaseSerializers());
		context.addTypeModifier(new InvernoBaseTypeModifier());

		if(this._cfgWriteAbsentAsNull) {
			context.addBeanSerializerModifier(new InvernoBaseBeanSerializerModifier());
		}
	}

	public InvernoBaseModule configureAbsentsAsNulls(boolean state) {
		_cfgWriteAbsentAsNull = state;
		return this;
	}
}
