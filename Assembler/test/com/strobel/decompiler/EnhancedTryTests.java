/*
 * EnhancedTryTests.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler;

import java.io.IOException;
import java.io.StringWriter;

public class EnhancedTryTests {
    public void testEnhancedTryEmpty() throws IOException {
        try (final StringWriter writer = new StringWriter()) {
            writer.write("This is only a test.");
        }
    }

/*
    public void testEnhancedTryMinimal() throws IOException {
        try (final StringWriter writer = new StringWriter()) {
            writer.write("This is only a test.");
        }
    }

    public void testEnhancedTryMinimalEmptyCatch() {
        try (final StringWriter writer = new StringWriter()) {
            writer.write("This is only a test.");
        }
        catch (IOException ignored) {
            return;
        }
    }
*/
}
