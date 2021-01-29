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

package com.radixdlt.store.mvstore;

import com.radixdlt.identifiers.AID;
import com.radixdlt.store.SearchCursor;
import com.radixdlt.store.StoreIndex.LedgerIndexType;
import org.h2.mvstore.MVMap;
import org.h2.value.VersionedValue;

public class UniqueSearchCursor implements SearchCursor {
	private final MVMap<byte[], VersionedValue> map;
	private final byte[] key;
	private final VersionedValue value;

	public UniqueSearchCursor(MVMap<byte[], VersionedValue> map, byte[] key) {
		this.map = map;
		this.key = key;
		this.value = map.get(key);
	}

	@Override
	public LedgerIndexType getType() {
		return LedgerIndexType.UNIQUE;
	}

	@Override
	public AID get() {
		return AID.from((byte[]) value.getCurrentValue(), Long.BYTES + 1);
	}

	@Override
	public SearchCursor next() {
		return makeCursor(map.higherKey(key));
	}

	@Override
	public SearchCursor previous() {
		return makeCursor(map.lowerKey(key));
	}

	@Override
	public SearchCursor first() {
		return makeCursor(map.firstKey());
	}

	@Override
	public SearchCursor last() {
		return makeCursor(map.lastKey());
	}

	private SearchCursor makeCursor(byte[] nextKey) {
		return nextKey == null ? null : new UniqueSearchCursor(map, nextKey);
	}
}
