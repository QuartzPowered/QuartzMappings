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
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MappedClass {

    private final String name;
    private Map<String, String> methods;
    private Map<String, String> fields;
    private Map<String, AccessTransform> access;

    public MappedClass(String name) {
        this.name = requireNonNull(name, "name");
    }

    public MappedClass(String name, Map<String, String> methods, Map<String, String> fields, Map<String, AccessTransform> access) {
        this(name);
        this.methods = methods;
        this.fields = fields;
        this.access = access;
    }

    public String getName() {
        return this.name;
    }

    public boolean hasMethods() {
        return this.methods != null && !this.methods.isEmpty();
    }

    public Map<String, String> getMethods() {
        if (this.methods == null) {
            this.methods = new HashMap<>();
        }

        return this.methods;
    }

    public boolean hasFields() {
        return this.fields != null && !this.fields.isEmpty();
    }

    public Map<String, String> getFields() {
        if (this.fields == null) {
            this.fields = new HashMap<>();
        }

        return this.fields;
    }

    public boolean hasAccess() {
        return this.access != null && !this.access.isEmpty();
    }

    public Map<String, AccessTransform> getAccess() {
        if (this.access == null) {
            this.access = new HashMap<>();
        }

        return this.access;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MappedClass)) {
            return false;
        }

        MappedClass that = (MappedClass) o;
        return Objects.equals(this.name, that.name)
                && Objects.equals(this.methods, that.methods)
                && Objects.equals(this.fields, that.fields)
                && Objects.equals(this.access, that.access);
    }

    @Override
    public int hashCode() {
        return hash(this.name, this.methods, this.fields, this.access);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("name", this.name)
                .add("methods", this.methods)
                .add("fields", this.fields)
                .add("access", this.access)
                .toString();
    }

}
