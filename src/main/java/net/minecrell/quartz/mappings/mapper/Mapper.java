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
package net.minecrell.quartz.mappings.mapper;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableTable;
import net.minecrell.quartz.mappings.AccessTransform;
import org.objectweb.asm.tree.MethodNode;

public class Mapper extends ClassMapper {

    protected final ImmutableTable<String, String, String> methods;
    protected final ImmutableTable<String, String, String> fields;

    protected final ImmutableMultimap<String, MethodNode> constructors;
    protected final ImmutableTable<String, String, AccessTransform> accessTransforms;

    public Mapper(ImmutableBiMap<String, String> classes, ImmutableTable<String, String, String> methods,
            ImmutableTable<String, String, String> fields, ImmutableMultimap<String, MethodNode> constructors,
            ImmutableTable<String, String, AccessTransform> accessTransforms) {
        super(classes);
        this.methods = requireNonNull(methods, "methods");
        this.fields = requireNonNull(fields, "fields");
        this.constructors = requireNonNull(constructors, "constructors");
        this.accessTransforms = requireNonNull(accessTransforms, "accessTransforms");
    }

    public ImmutableTable<String, String, String> getMethods() {
        return methods;
    }

    public ImmutableTable<String, String, String> getFields() {
        return fields;
    }

    public ImmutableMultimap<String, MethodNode> getConstructors() {
        return constructors;
    }

    public ImmutableTable<String, String, AccessTransform> getAccessTransforms() {
        return accessTransforms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Mapper)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        Mapper that = (Mapper) o;
        return this.methods.equals(that.methods)
                && this.fields.equals(that.fields)
                && this.constructors.equals(that.constructors)
                && this.accessTransforms.equals(that.accessTransforms);
    }

    @Override
    public int hashCode() {
        return hash(super.hashCode(), this.methods, this.fields, this.constructors, this.accessTransforms);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("classes", super.classes)
                .add("methods", this.methods)
                .add("fields", this.fields)
                .add("constructors", this.constructors)
                .add("accessTransforms", this.accessTransforms)
                .toString();
    }

}
