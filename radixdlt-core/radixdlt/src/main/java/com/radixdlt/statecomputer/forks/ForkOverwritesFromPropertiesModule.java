/*
 * (C) Copyright 2021 Radix DLT Ltd
 *
 * Radix DLT Ltd licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 */

package com.radixdlt.statecomputer.forks;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.OptionalBinder;
import com.radixdlt.properties.RuntimeProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ForkOverwritesFromPropertiesModule extends AbstractModule {
	private static final Logger logger = LogManager.getLogger();

	private static class ForkOverwrite implements UnaryOperator<Set<ForkConfig>> {
		@Inject
		private RuntimeProperties properties;

		@Override
		public Set<ForkConfig> apply(Set<ForkConfig> forkConfigs) {
			return forkConfigs.stream()
				.map(c -> {
					var epochOverwrite = properties.get("overwrite_forks." + c.getName() + ".epoch", "");
					if (!epochOverwrite.isBlank()) {
						var epoch = Long.parseLong(epochOverwrite);
						logger.warn("Overwriting epoch of " + c.getName() + " from " + c.getEpoch() + " to " + epoch);
						c = c.overrideEpoch(epoch);
					}

					var viewOverwrite = properties.get("overwrite_forks." + c.getName() + ".views", "");
					if (!viewOverwrite.isBlank()) {
						var view = Long.parseLong(viewOverwrite);
						logger.warn("Overwriting views of " + c.getName() + " from " + c.getConfig().getMaxRounds() + " to " + view);
						c = c.overrideConfig(c.getConfig().overrideMaxRounds(view));
					}
					return c;
				})
				.collect(Collectors.toSet());
		}
	}

	@Override
	protected void configure() {
		OptionalBinder.newOptionalBinder(binder(), new TypeLiteral<UnaryOperator<Set<ForkConfig>>>() { })
			.setBinding()
			.to(ForkOverwrite.class);
	}
}
