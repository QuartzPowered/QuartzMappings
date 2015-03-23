/*
 * QuartzMappings
 * Copyright (c) 2015, Minecrell <https://github.com/Minecrell>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.minecrell.quartz.mappings;

import static java.util.Objects.hash;

import com.google.common.base.Objects;

public class AccessTransform {

    private final AccessModifier access;
    private final boolean removeFinal;

    public AccessTransform(AccessModifier access, boolean removeFinal) {
        this.access = access;
        this.removeFinal = removeFinal;
    }

    public AccessModifier getAccess() {
        return this.access;
    }

    public boolean removeFinal() {
        return this.removeFinal;
    }

    public int transform(int access) {
        access = this.access.transform(access);
        if (removeFinal) {
            access = AccessModifier.removeFinal(access);
        }

        return access;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccessTransform)) {
            return false;
        }

        AccessTransform that = (AccessTransform) o;
        return this.access == that.access && this.removeFinal == that.removeFinal;
    }

    @Override
    public int hashCode() {
        return hash(this.access, this.removeFinal);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("access", this.access)
                .add("removeFinal", this.removeFinal)
                .toString();
    }

}
