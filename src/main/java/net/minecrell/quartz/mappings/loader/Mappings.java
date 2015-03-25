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
package net.minecrell.quartz.mappings.loader;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecrell.quartz.mappings.AccessTransform;
import net.minecrell.quartz.mappings.MappedClass;
import net.minecrell.quartz.mappings.mapper.Mapper;
import org.objectweb.asm.tree.MethodNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class Mappings {

    private Mappings() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAPPINGS_TYPE = new TypeToken<Map<String, MappedClass>>(){}.getType();

    public static Map<String, MappedClass> read(String json) {
        return GSON.fromJson(json, MAPPINGS_TYPE);
    }

    public static Map<String, MappedClass> read(Reader reader) {
        return GSON.fromJson(reader, MAPPINGS_TYPE);
    }

    public static Map<String, MappedClass> read(InputStream in) {
        return read(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
    }

    public static Map<String, MappedClass> load(URL resource) throws IOException {
        try (InputStream in = resource.openStream()) {
            return read(in);
        }
    }

    public static String write(Map<String, MappedClass> mappings) {
        return GSON.toJson(mappings, MAPPINGS_TYPE);
    }

    public static void write(Appendable writer, Map<String, MappedClass> mappings) {
        GSON.toJson(mappings, MAPPINGS_TYPE, writer);
    }


    public static Mapper createMapper(Map<String, MappedClass> mappings) {
        ImmutableBiMap.Builder<String, String> classes = ImmutableBiMap.builder();

        ImmutableTable.Builder<String, String, String> methods = ImmutableTable.builder();
        ImmutableTable.Builder<String, String, String> fields = ImmutableTable.builder();

        ImmutableTable.Builder<String, String, AccessTransform> accessTransforms = ImmutableTable.builder();

        ImmutableMultimap.Builder<String, MethodNode> constructors = ImmutableMultimap.builder();

        for (Map.Entry<String, MappedClass> entry : mappings.entrySet()) {
            String internalName = entry.getKey();
            String className = internalName.replace('/', '.');

            MappedClass mapping = entry.getValue();
            String mappedName = mapping.getName();
            classes.put(mappedName, internalName);

            if (mapping.hasMethods()) {
                fillTable(methods, mappedName, mapping.getMethods());
            }

            if (mapping.hasFields()) {
                fillTable(fields, mappedName, mapping.getFields());
            }

            if (mapping.hasAccess()) {
                fillTable(accessTransforms, className, mapping.getAccess());
            }
        }

        return new Mapper(classes.build(), methods.build(), fields.build(), accessTransforms.build());
    }

    private static <R, C, V> void fillTable(ImmutableTable.Builder<R, C, V> builder, R row, Map<C, V> values) {
        for (Map.Entry<C, V> entry : values.entrySet()) {
            builder.put(row, entry.getKey(), entry.getValue());
        }
    }

}
