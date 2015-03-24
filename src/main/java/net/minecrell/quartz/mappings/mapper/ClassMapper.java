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

import static java.util.Objects.requireNonNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableBiMap;
import org.objectweb.asm.commons.Remapper;

public class ClassMapper {

    protected final ImmutableBiMap<String, String> classes;

    public ClassMapper(ImmutableBiMap<String, String> classes) {
        this.classes = requireNonNull(classes, "classes");
    }

    public String map(String className) {
        if (className == null) return null;

        String name = this.classes.get(className);
        if (name != null) {
            return name;
        }

        // We may have no name for the inner class directly, but it should be still part of the outer class
        int innerClassPos = className.lastIndexOf('$');
        if (innerClassPos >= 0) {
            return map(className.substring(0, innerClassPos)) + className.substring(innerClassPos);
        }

        return className; // Unknown class
    }

    public Remapper createRemapper() {
        return new Remapper() {

            @Override
            public String map(String typeName) {
                return ClassMapper.this.map(typeName);
            }
        };
    }

    public String unmap(String className) {
        String name = this.classes.inverse().get(className);
        if (name != null) {
            return name;
        }

        // We may have no name for the inner class directly, but it should be still part of the outer class
        int innerClassPos = className.lastIndexOf('$');
        if (innerClassPos >= 0) {
            return unmap(className.substring(0, innerClassPos)) + className.substring(innerClassPos);
        }

        return className; // Unknown class
    }

    public Remapper createUnmapper() {
        return new Remapper() {

            @Override
            public String map(String typeName) {
                return ClassMapper.this.unmap(typeName);
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClassMapper)) {
            return false;
        }

        ClassMapper that = (ClassMapper) o;
        return this.classes.equals(that.classes);
    }

    @Override
    public int hashCode() {
        return this.classes.hashCode();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(this.classes)
                .toString();
    }

}
