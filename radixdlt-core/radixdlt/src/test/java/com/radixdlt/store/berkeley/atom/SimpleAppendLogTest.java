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

package com.radixdlt.store.berkeley.atom;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.radixdlt.counters.SystemCounters;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;

import static com.radixdlt.store.berkeley.atom.AppendLog.openCompressed;
import static com.radixdlt.store.berkeley.atom.AppendLog.openSimple;

public class SimpleAppendLogTest {
	private final SystemCounters systemCounters = mock(SystemCounters.class);

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void appendLogCanBeCreated() throws IOException {
		String path = createTempPath();

		readAfterWrite(openSimple(path));
	}

	@Test
	public void appendLogCanBeReadFromTheBeginning() throws IOException {
		var path = createTempPath();

		writeLogEntriesAndClose(openSimple(path));

		readSequentially(openSimple(path));
	}

	@Test
	public void compressedAppendLogCanBeCreated() throws IOException {
		String path = createTempPath();

		readAfterWrite(openCompressed(path, systemCounters));
	}

	@Test
	public void compressedAppendLogCanBeReadFromTheBeginning() throws IOException {
		var path = createTempPath();

		writeLogEntriesAndClose(openCompressed(path, systemCounters));

		readSequentially(openCompressed(path, systemCounters));
	}

	private String createTempPath() throws IOException {
		return folder.newFile().getAbsolutePath();
	}

	private void readSequentially(final AppendLog newAppendLog) throws IOException {
		long pos;

		pos = checkSingleChunk(newAppendLog, 0L, new byte[]{0x01});
		pos = checkSingleChunk(newAppendLog, pos, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05});
		pos = checkSingleChunk(newAppendLog, pos, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x0C, 0x7F, -1});
	}

	private void writeLogEntriesAndClose(final AppendLog appendLog) throws IOException {
		var s0 = appendLog.write(new byte[]{0x01}, 0);
		var s1 = appendLog.write(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05}, s0);
		appendLog.write(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x0C, 0x7F, -1}, s0 + s1);
		appendLog.close();
	}

	private void readAfterWrite(final AppendLog appendLog) throws IOException {
		checkReadAfterWrite(appendLog, new byte[]{0x01});
		checkReadAfterWrite(appendLog, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05});
		checkReadAfterWrite(appendLog, new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x0C, 0x7F, -1});
	}

	private void checkReadAfterWrite(AppendLog appendLog, byte[] data) throws IOException {
		long pos = appendLog.position();
		appendLog.write(data, pos);

		assertArrayEquals(data, appendLog.read(pos));
	}

	private long checkSingleChunk(AppendLog appendLog, long offset, byte[] expect) throws IOException {
		var result = appendLog.readChunk(offset);

		assertArrayEquals(expect, result.getFirst());

		return offset + result.getSecond() + Integer.BYTES;
	}
}