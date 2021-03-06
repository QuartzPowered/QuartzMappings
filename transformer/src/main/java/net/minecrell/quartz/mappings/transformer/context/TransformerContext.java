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

import net.minecrell.quartz.mappings.transformer.provider.ClassProvider;
import net.minecrell.quartz.mappings.transformer.renamer.ClassRenamer;
import net.minecrell.quartz.mappings.transformer.transform.CoreClassTransformer;
import net.minecrell.quartz.mappings.transformer.transform.TreeClassTransformer;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.List;

public interface TransformerContext {

    ClassProvider getClassProvider();

    ClassRenamer getRenamer();

    List<CoreClassTransformer> getCoreTransformers();

    List<TreeClassTransformer> getTreeTransformers();

    default ClassReader getTransformed(String name) throws IOException {
        if (name == null) {
            return null;
        }

        return getTransformed(getClassProvider().getClass(name));
    }

    default ClassReader getTransformed(byte[] classBytes) {
        if (classBytes == null) {
            return null;
        }
        return getTransformed(new ClassReader(classBytes));
    }

    ClassReader getTransformed(ClassReader reader);


}
