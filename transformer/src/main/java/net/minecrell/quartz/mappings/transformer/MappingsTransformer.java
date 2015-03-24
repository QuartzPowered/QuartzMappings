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
import net.minecrell.quartz.mappings.transformer.context.SimpleTransformerContext;
import net.minecrell.quartz.mappings.transformer.context.TransformerContext;
import net.minecrell.quartz.mappings.transformer.provider.ClassProvider;
import net.minecrell.quartz.mappings.transformer.provider.ZipClassProvider;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class MappingsTransformer {

    private MappingsTransformer() {}

    public static ClassProvider getProvider(ZipFile zip) {
        return new ZipClassProvider(zip);
    }

    public static TransformerContext createContext(ZipFile zip, ClassRenamer renamer, ClassTransformer... transformers) {
        return new SimpleTransformerContext(getProvider(zip), renamer, ImmutableList.copyOf(transformers));
    }

    public static void transform(ZipFile zip, ZipOutputStream out, ClassRenamer renamer, ClassTransformer... transformers) throws IOException {
        TransformerContext context = createContext(zip, renamer, transformers);
        ZipClassProvider provider = (ZipClassProvider) context.getClassProvider();

        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            ZipEntry entryOut = new ZipEntry(entry);

            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                out.putNextEntry(entryOut);
                ByteStreams.copy(zip.getInputStream(entry), out);
                continue;
            }

            ClassReader reader = provider.getClassFile(entry);
            byte[] transformed = context.getTransformed(reader);
            entryOut.setSize(transformed.length);
            entryOut.setCompressedSize(-1);
            out.putNextEntry(entryOut);
            out.write(transformed);
        }
    }

}
