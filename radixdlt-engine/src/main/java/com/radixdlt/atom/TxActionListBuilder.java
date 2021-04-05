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

package com.radixdlt.atom;

import com.radixdlt.atom.actions.BurnNativeToken;
import com.radixdlt.atom.actions.RegisterAsValidator;
import com.radixdlt.atom.actions.TransferNativeToken;
import com.radixdlt.atom.actions.UnregisterAsValidator;
import com.radixdlt.identifiers.RRI;
import com.radixdlt.identifiers.RadixAddress;
import com.radixdlt.utils.UInt256;

import java.util.ArrayList;
import java.util.List;

public class TxActionListBuilder {
	private List<TxAction> actions = new ArrayList<>();

	private TxActionListBuilder() {
	}

	public static TxActionListBuilder create() {
		return new TxActionListBuilder();
	}

	public TxActionListBuilder registerAsValidator() {
		var action = new RegisterAsValidator();
		actions.add(action);
		return this;
	}

	public TxActionListBuilder unregisterAsValidator() {
		var action = new UnregisterAsValidator();
		actions.add(action);
		return this;
	}

	public TxActionListBuilder transferNative(RRI rri, RadixAddress to, UInt256 amount) {
		var action = new TransferNativeToken(rri, to, amount);
		actions.add(action);
		return this;
	}

	public TxActionListBuilder burnNative(RRI rri, UInt256 amount) {
		var action = new BurnNativeToken(rri, amount);
		actions.add(action);
		return this;
	}

	public List<TxAction> build() {
		return actions;
	}
}