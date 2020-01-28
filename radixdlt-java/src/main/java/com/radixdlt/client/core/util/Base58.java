/*
 * (C) Copyright 2020 Radix DLT Ltd
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the “Software”),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.radixdlt.client.core.util;

import java.math.BigInteger;

public class Base58 {

	private static final char[] B58 = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

	private static final int[] R58 = new int[256];

	static {
		for (int i = 0; i < 256; ++i) {
			R58[i] = -1;
		}
		for (int i = 0; i < B58.length; ++i) {
			R58[B58[i]] = i;
		}
	}

	private Base58() {
	}


	// Encodes the specified byte array into a String using the Base58 encoding scheme
	public static String toBase58(byte[] b) {
		if (b.length == 0) {
			return "";
		}

		int lz = 0;
		while (lz < b.length && b[lz] == 0) {
			++lz;
		}

		StringBuilder s = new StringBuilder();
		// Set sign to positive to stop BigInteger interpreting high bit as sign
		BigInteger n = new BigInteger(1, b);
		while (n.compareTo(BigInteger.ZERO) > 0) {
			BigInteger[] r = n.divideAndRemainder(BigInteger.valueOf(58));
			n = r[0];
			char digit = B58[r[1].intValue()];
			s.append(digit);
		}
		while (lz > 0) {
			--lz;
			s.append("1");
		}
		return s.reverse().toString();
	}

	// Decodes the specified Base58 encoded String to its byte array representation
	public static byte[] fromBase58(String s) {
		try {
			boolean leading = true;
			int lz = 0;
			BigInteger b = BigInteger.ZERO;
			for (char c : s.toCharArray()) {
				if (leading && c == '1') {
					++lz;
				} else {
					leading = false;
					b = b.multiply(BigInteger.valueOf(58));
					b = b.add(BigInteger.valueOf(R58[c]));
				}
			}
			byte[] encoded = b.toByteArray();
			if (encoded[0] == 0) {
				if (lz > 0) {
					--lz;
				} else {
					byte[] e = new byte[encoded.length - 1];
					System.arraycopy(encoded, 1, e, 0, e.length);
					encoded = e;
				}
			}
			byte[] result = new byte[encoded.length + lz];
			System.arraycopy(encoded, 0, result, lz, encoded.length);

			return result;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Invalid character in address");
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}
}
