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
package net.minecrell.quartz.mappings.transformer.context;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import net.minecrell.quartz.mappings.transformer.ClassRenamer;
import net.minecrell.quartz.mappings.transformer.ClassTransformer;
import net.minecrell.quartz.mappings.transformer.NullClassRenamer;
import net.minecrell.quartz.mappings.transformer.provider.ClassProvider;
import org.objectweb.asm.ClassReader;

import java.util.List;

public class SimpleTransformerContext implements TransformerContext {

    private final ClassProvider classProvider;
    private final ClassRenamer renamer;
    private final ImmutableList<ClassTransformer> transformers;

    public SimpleTransformerContext(ClassProvider classProvider, ClassRenamer renamer, ImmutableList<ClassTransformer> transformers) {
        this.classProvider = requireNonNull(classProvider, "classProvider");
        this.renamer = renamer != null ? renamer : NullClassRenamer.getInstance();
        this.transformers = requireNonNull(transformers, "transformers");
    }

    @Override
    public ClassProvider getClassProvider() {
        return this.classProvider;
    }

    @Override
    public ClassRenamer getRenamer() {
        return this.renamer;
    }

    @Override
    public List<ClassTransformer> getTransformers() {
        return this.transformers;
    }

    @Override
    public byte[] getTransformed(ClassReader reader) {
        if (reader == null) {
            return null;
        }

        String name = reader.getClassName();
        String transformedName = this.renamer.unmap(name);

        byte[] bytes = null;
        for (ClassTransformer transformer : this.transformers) {
            if (transformer.transform(name, transformedName)) {
                if (bytes != null) {
                    reader = new ClassReader(bytes);
                }

                bytes = transformer.transform(name, transformedName, reader);
            }
        }

        return bytes != null ? bytes : reader.b;
    }

}
