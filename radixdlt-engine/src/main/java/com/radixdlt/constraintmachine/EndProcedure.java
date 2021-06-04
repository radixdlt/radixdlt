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

package com.radixdlt.constraintmachine;

import com.radixdlt.store.ReadableAddrs;
import java.util.function.Function;

public class EndProcedure<S extends ReducerState> implements MethodProcedure {
	private final Class<S> reducerStateClass;
	private final Function<S, PermissionLevel> permissionLevel;
	private final EndAuthorization<S> endAuthorization;
	private final EndReducer<S> endReducer;

	public EndProcedure(
		Class<S> reducerStateClass,
		Function<S, PermissionLevel> permissionLevel,
		EndAuthorization<S> endAuthorization,
		EndReducer<S> endReducer
	) {
		this.reducerStateClass = reducerStateClass;
		this.permissionLevel = permissionLevel;
		this.endAuthorization = endAuthorization;
		this.endReducer = endReducer;
	}

	public ProcedureKey getEndProcedureKey() {
		return ProcedureKey.of(null, reducerStateClass);
	}

	@Override
	public PermissionLevel permissionLevel(Object o) {
		return permissionLevel.apply((S) o);
	}

	@Override
	public void verifyAuthorization(Object o, ReadableAddrs readableAddrs, ExecutionContext context) throws AuthorizationException {
		endAuthorization.verify((S) o, readableAddrs, context);
	}

	@Override
	public ReducerResult call(Object o, ReducerState reducerState, ReadableAddrs readableAddrs) throws ProcedureException {
		endReducer.reduce((S) reducerState, readableAddrs);
		return ReducerResult.complete();
	}
}
