/*
 * (C) Copyright 2020 Radix DLT Ltd
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
 */

package com.radixdlt.mempool;

import com.radixdlt.atom.Txn;

import java.util.Objects;

/**
 * Message indicating that a command failed to be added to the mempool
 */
public final class MempoolAddFailure {
	private final Txn txn;
	private final Exception exception;

	private MempoolAddFailure(Txn txn, Exception exception) {
		this.txn = txn;
		this.exception = exception;
	}

	public Txn getTxn() {
		return txn;
	}

	public Exception getException() {
		return exception;
	}

	public static MempoolAddFailure create(Txn txn, Exception exception) {
		Objects.requireNonNull(txn);
		return new MempoolAddFailure(txn, exception);
	}

	@Override
	public int hashCode() {
		return Objects.hash(txn, exception);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MempoolAddFailure)) {
			return false;
		}

		MempoolAddFailure other = (MempoolAddFailure) o;
		return Objects.equals(this.txn, other.txn)
			&& Objects.equals(this.exception, other.exception);
	}

	@Override
	public String toString() {
		return String.format("%s{txn=%s ex=%s}", this.getClass().getSimpleName(), this.txn, this.exception);
	}
}
