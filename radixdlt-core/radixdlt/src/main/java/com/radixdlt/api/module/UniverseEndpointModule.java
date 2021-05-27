/*
 * (C) Copyright 2021 Radix DLT Ltd
 *
 * Radix DLT Ltd licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.radixdlt.api.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.radixdlt.DefaultSerialization;
import com.radixdlt.api.Controller;
import com.radixdlt.api.controller.UniverseController;
import com.radixdlt.api.qualifier.AtNode;
import com.radixdlt.serialization.DsonOutput;
import com.radixdlt.universe.Universe;

public class UniverseEndpointModule extends AbstractModule {
	@AtNode
	@ProvidesIntoSet
	public Controller universeController(Universe universe) {
		return new UniverseController(DefaultSerialization.getInstance().toJson(universe, DsonOutput.Output.API));
	}
}