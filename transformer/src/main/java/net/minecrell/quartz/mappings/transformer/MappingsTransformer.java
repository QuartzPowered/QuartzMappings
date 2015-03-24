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

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import net.minecrell.quartz.mappings.mapper.Mapper;
import net.minecrell.quartz.mappings.transformer.context.SimpleTransformerContext;
import net.minecrell.quartz.mappings.transformer.context.TransformerContext;
import net.minecrell.quartz.mappings.transformer.provider.ClassProvider;
import net.minecrell.quartz.mappings.transformer.provider.ZipClassProvider;
import net.minecrell.quartz.mappings.transformer.renamer.ClassRenamer;
import net.minecrell.quartz.mappings.transformer.transform.ClassTransformer;
import net.minecrell.quartz.mappings.transformer.transform.CoreClassTransformer;
import net.minecrell.quartz.mappings.transformer.transform.TreeClassTransformer;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class MappingsTransformer {

    private MappingsTransformer() {}

    public static TransformerContext createContext(ClassProvider provider, ClassRenamer renamer, ClassTransformer... transformers) {
        ImmutableList.Builder<CoreClassTransformer> coreTransformers = ImmutableList.builder();
        ImmutableList.Builder<TreeClassTransformer> treeTransformers = ImmutableList.builder();

        for (ClassTransformer transformer : transformers) {
            if (transformer instanceof CoreClassTransformer) {
                coreTransformers.add((CoreClassTransformer) transformer);
            } else if (transformer instanceof TreeClassTransformer) {
                treeTransformers.add((TreeClassTransformer) transformer);
            } else {
                throw new IllegalArgumentException("Unsupported transformer type: " + transformer.getClass());
            }
        }

        return new SimpleTransformerContext(provider, renamer, coreTransformers.build(), treeTransformers.build());
    }

    public static ClassProvider getProvider(ZipFile zip) {
        return new ZipClassProvider(zip);
    }

    public static TransformerContext createContext(ZipFile zip, ClassRenamer renamer, CoreClassTransformer... transformers) {
        return createContext(getProvider(zip), renamer, transformers);
    }

    public static void transform(ZipFile zip, ZipOutputStream out, TransformerContext context) throws IOException {
        ZipClassProvider provider = (ZipClassProvider) context.getClassProvider();

        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                out.putNextEntry(new ZipEntry(entry));
                ByteStreams.copy(zip.getInputStream(entry), out);
                continue;
            }

            ClassReader reader = provider.getClassFile(entry);
            reader = context.getTransformed(reader);

            ZipEntry entryOut = new ZipEntry(reader.getClassName() + ".class");
            entryOut.setSize(reader.b.length);
            entryOut.setCompressedSize(-1);
            out.putNextEntry(entryOut);
            out.write(reader.b);
        }
    }

    public static void deobfuscate(ZipFile zip, ZipOutputStream out, Mapper mapper) throws IOException {
        ClassProvider provider = getProvider(zip);
        DeobfuscationTransformer transformer = new DeobfuscationTransformer(mapper, provider);
        transform(zip, out, createContext(provider, transformer, transformer, new AccessTransformer(mapper)));
    }

}
