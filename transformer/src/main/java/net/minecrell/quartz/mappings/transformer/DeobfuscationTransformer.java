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
package net.minecrell.quartz.mappings.transformer;

import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecrell.quartz.mappings.mapper.Mapper;
import net.minecrell.quartz.mappings.transformer.provider.ClassProvider;
import net.minecrell.quartz.mappings.transformer.renamer.ClassRenamer;
import net.minecrell.quartz.mappings.transformer.transform.CoreClassTransformer;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DeobfuscationTransformer extends Remapper implements CoreClassTransformer, ClassRenamer {

    private final Mapper mapper;
    private final ClassProvider provider;

    private final Map<String, Map<String, String>> methods;
    private final Map<String, Map<String, String>> fields;

    private final Set<String> failedMethods = new HashSet<>();
    private final Set<String> failedFields = new HashSet<>();

    public DeobfuscationTransformer(Mapper mapper, ClassProvider provider) {
        this.mapper = requireNonNull(mapper, "mapper");
        this.provider = requireNonNull(provider, "provider");

        this.methods = Maps.newHashMapWithExpectedSize(mapper.getMethods().size());
        this.fields = Maps.newHashMapWithExpectedSize(mapper.getFields().size());
    }

    @Override
    public String map(String className) {
        return this.mapper.map(className);
    }

    @Override
    public String unmap(String className) {
        return this.mapper.unmap(className);
    }

    @Override
    public String mapFieldName(String owner, String fieldName, String desc) {
        Map<String, String> fields = getFieldMap(owner);
        if (fields != null) {
            String name = fields.get(fieldName + ':' + desc);
            if (name != null) {
                return name;
            }
        }

        return fieldName;
    }

    private Map<String, String> getFieldMap(String owner) {
        Map<String, String> result = this.fields.get(owner);
        if (result != null) {
            return result;
        }

        if (!this.failedFields.contains(owner)) {
            loadSuperMaps(owner);
            if (!this.fields.containsKey(owner)) {
                this.failedFields.add(owner);
            }
        }

        return this.fields.get(owner);
    }

    @Override
    public String mapMethodName(String owner, String methodName, String desc) {
        Map<String, String> methods = getMethodMap(owner);
        if (methods != null) {
            String name = methods.get(methodName + desc);
            if (name != null) {
                return name;
            }
        }

        return methodName;
    }

    private Map<String, String> getMethodMap(String owner) {
        Map<String, String> result = this.methods.get(owner);
        if (result != null) {
            return result;
        }

        if (!this.failedMethods.contains(owner)) {
            loadSuperMaps(owner);
            if (!this.methods.containsKey(owner)) {
                this.failedMethods.add(owner);
            }
        }

        return this.methods.get(owner);
    }

    private void loadSuperMaps(String name) {
        ClassReader reader;
        try {
            reader = this.provider.getClass(name);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        if (reader != null) {
            createSuperMaps(name, reader.getSuperName(), reader.getInterfaces());
        }
    }

    private void createSuperMaps(String name, String superName, String[] interfaces) {
        if (Strings.isNullOrEmpty(superName)) {
            return;
        }

        String[] parents = new String[interfaces.length + 1];
        parents[0] = superName;
        System.arraycopy(interfaces, 0, parents, 1, interfaces.length);

        for (String parent : parents) {
            if (!this.fields.containsKey(parent)) {
                loadSuperMaps(parent);
            }
        }

        Map<String, String> methods = new HashMap<>();
        Map<String, String> fields = new HashMap<>();

        Map<String, String> m;
        for (String parent : parents) {
            m = this.methods.get(parent);
            if (m != null) {
                methods.putAll(m);
            }
            m = this.fields.get(parent);
            if (m != null) {
                fields.putAll(m);
            }
        }

        methods.putAll(this.mapper.getMethods().row(name));
        fields.putAll(this.mapper.getFields().row(name));

        this.methods.put(name, ImmutableMap.copyOf(methods));
        this.fields.put(name, ImmutableMap.copyOf(fields));
    }

    @Override
    public int readerFlags() {
        return EXPAND_FRAMES;
    }

    @Override
    public ClassVisitor transform(String name, String transformedName, ClassReader reader, ClassVisitor visitor) {
        return new RemappingAdapter(visitor);
    }

    private class RemappingAdapter extends RemappingClassAdapter {

        public RemappingAdapter(ClassVisitor cv) {
            super(cv, DeobfuscationTransformer.this);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            createSuperMaps(name, superName, interfaces != null ? interfaces : ArrayUtils.EMPTY_STRING_ARRAY);
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if (this.cv != null) {
                name = this.remapper.mapType(name);
                this.cv.visitInnerClass(
                        name,
                        outerName == null ? null : this.remapper.mapType(outerName),
                        getSimpleName(name),
                        access
                );
            }
        }

    }

    private static String getSimpleName(String name) {
        int pos = name.lastIndexOf('$');
        if (pos == -1) {
            pos = name.lastIndexOf('/');
        }

        if (pos >= 0) {
            return name.substring(pos + 1);
        } else {
            return name;
        }
    }

}
