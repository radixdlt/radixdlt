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

package org.radix.serialization2.client;

import java.util.Collection;

import org.radix.serialization2.ClassScanningSerializationPolicy;
import org.radix.serialization2.SerializationPolicy;

import com.google.common.annotations.VisibleForTesting;

/**
 * Class that maintains a map of {@link org.radix.serialization2.DsonOutput.Output}
 * values to a set of pairs of classes and field/method names to output for that
 * serialization type.
 * <p>
 * This implementation works by scanning a supplied {@link Collection} for
 * classes annotated with {@code SerializerConstants.SERIALIZER_ID_ANNOTATION}
 * and passing these classes to {@link ClassScanningSerializationPolicy}.
 */
public final class CollectionScanningSerializationPolicy extends ClassScanningSerializationPolicy {

	/**
	 * Create a {@link SerializationPolicy} from a supplied list of classes.
	 * The classes are scanned for appropriate annotations.
	 *
	 * @param classes The classes to scan for annotations
	 * @return A freshly created {@link CollectionScanningSerializationPolicy}
	 */
	public static SerializationPolicy create(Collection<Class<?>> classes) {
		return new CollectionScanningSerializationPolicy(classes);
	}

	@VisibleForTesting
	CollectionScanningSerializationPolicy(Collection<Class<?>> classes) {
		super(classes);
	}
}
