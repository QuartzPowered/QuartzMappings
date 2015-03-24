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
import net.minecrell.quartz.mappings.transformer.provider.ClassProvider;
import net.minecrell.quartz.mappings.transformer.renamer.ClassRenamer;
import net.minecrell.quartz.mappings.transformer.renamer.NullClassRenamer;
import net.minecrell.quartz.mappings.transformer.transform.CoreClassTransformer;
import net.minecrell.quartz.mappings.transformer.transform.TreeClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

public class SimpleTransformerContext implements TransformerContext {

    private final ClassProvider classProvider;
    private final ClassRenamer renamer;

    private final ImmutableList<CoreClassTransformer> coreTransformers;
    private final ImmutableList<TreeClassTransformer> treeTransformers;

    public SimpleTransformerContext(ClassProvider classProvider, ClassRenamer renamer, ImmutableList<CoreClassTransformer> coreTransformers,
            ImmutableList<TreeClassTransformer> treeTransformers) {
        this.classProvider = requireNonNull(classProvider, "classProvider");
        this.renamer = renamer != null ? renamer : NullClassRenamer.getInstance();
        this.coreTransformers = requireNonNull(coreTransformers, "coreTransformers");
        this.treeTransformers = requireNonNull(treeTransformers, "treeTransformers");
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
    public List<CoreClassTransformer> getCoreTransformers() {
        return this.coreTransformers;
    }

    @Override
    public List<TreeClassTransformer> getTreeTransformers() {
        return this.treeTransformers;
    }

    @Override
    public ClassReader getTransformed(ClassReader reader) {
        if (reader == null) {
            return null;
        }

        String name = reader.getClassName();
        String transformedName = this.renamer.unmap(name);
        name = name.replace('/', '.');
        transformedName = transformedName.replace('/', '.');

        List<CoreClassTransformer> coreTransformers = new ArrayList<>(this.coreTransformers.size());

        int readerFlags = 0;
        int writerFlags = 0;

        for (CoreClassTransformer transformer : this.coreTransformers) {
            if (transformer.transform(name, transformedName)) {
                readerFlags |= transformer.readerFlags();
                writerFlags |= transformer.writerFlags();
                coreTransformers.add(transformer);
            }
        }

        List<TreeClassTransformer> treeTransformers = new ArrayList<>(this.treeTransformers.size());

        for (TreeClassTransformer transformer : this.treeTransformers) {
            if (transformer.transform(name, transformedName)) {
                readerFlags |= transformer.readerFlags();
                writerFlags |= transformer.writerFlags();
                treeTransformers.add(transformer);
            }
        }

        ClassWriter writer = new ClassWriter(writerFlags);
        ClassVisitor visitor;
        ClassNode classNode = null;

        if (!treeTransformers.isEmpty()) {
            classNode = new ClassNode();
            visitor = classNode;
        } else {
            visitor = writer;
        }

        for (CoreClassTransformer transformer : coreTransformers) {
            visitor = transformer.transform(name, transformedName, reader, visitor);
        }

        reader.accept(visitor, readerFlags);

        if (!treeTransformers.isEmpty()) {
            for (TreeClassTransformer transformer : treeTransformers) {
                classNode = transformer.transform(name, transformedName, classNode);
            }

            classNode.accept(writer);
        }

        return new ClassReader(writer.toByteArray());
    }

}
