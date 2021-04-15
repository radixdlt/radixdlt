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

package com.radixdlt.atom.actions;

import com.radixdlt.atom.TxAction;
import com.radixdlt.atom.TxBuilder;
import com.radixdlt.atom.TxBuilderException;
import com.radixdlt.atommodel.tokens.TokensParticle;
import com.radixdlt.atomos.RriId;
import com.radixdlt.identifiers.RRI;
import com.radixdlt.utils.UInt256;

public final class BurnToken implements TxAction {
	private final RRI rri;
	private final UInt256 amount;

	public BurnToken(RRI rri, UInt256 amount) {
		this.rri = rri;
		this.amount = amount;
	}

	public RRI rri() {
		return rri;
	}

	public UInt256 amount() {
		return amount;
	}

	@Override
	public void execute(TxBuilder txBuilder) throws TxBuilderException {
		var user = txBuilder.getAddressOrFail("Must have an address to burn.");
		var rriId = RriId.fromRri(rri);
		txBuilder.deallocateFungible(
			TokensParticle.class,
			p -> p.getRriId().equals(rriId) && p.getAddress().equals(user),
			TokensParticle::getAmount,
			amt -> new TokensParticle(user, amt, rriId),
			amount,
			"Not enough balance to for fee burn."
		);
	}
}

