package com.radixdlt.client.core.atoms.particles;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.radixdlt.client.core.address.RadixAddress;
import com.radixdlt.client.core.atoms.TokenClassReference;
import com.radixdlt.client.core.atoms.particles.quarks.FungibleQuark;
import org.radix.common.ID.EUID;
import org.radix.serialization2.DsonOutput;
import org.radix.serialization2.SerializerId2;

@SerializerId2("FEEPARTICLE")
public class FeeParticle extends TransferParticle {
	@JsonProperty("service")
	@DsonOutput(DsonOutput.Output.ALL)
	private EUID service;

	private FeeParticle() {
	}

	public FeeParticle(long quantity, RadixAddress address, long nonce, TokenClassReference tokenRef, long planck) {
		super(quantity, FungibleQuark.FungibleType.MINTED, address, nonce, tokenRef, planck);

		this.service = new EUID(1);
	}
}
