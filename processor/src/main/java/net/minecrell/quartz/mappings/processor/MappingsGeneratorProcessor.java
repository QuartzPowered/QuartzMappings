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
package net.minecrell.quartz.mappings.processor;

import static com.google.common.base.Preconditions.checkArgument;
import static net.minecrell.quartz.mappings.processor.util.Elements.getDescriptor;
import static net.minecrell.quartz.mappings.processor.util.Elements.getInternalName;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableBiMap;
import net.minecrell.quartz.mappings.AccessTransform;
import net.minecrell.quartz.mappings.Accessible;
import net.minecrell.quartz.mappings.MappedClass;
import net.minecrell.quartz.mappings.Mapping;
import net.minecrell.quartz.mappings.loader.Mappings;
import net.minecrell.quartz.mappings.mapper.ClassMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes({
        "net.minecrell.quartz.mappings.Accessible",
        "net.minecrell.quartz.mappings.Mapping"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions("baseJar")
public class MappingsGeneratorProcessor extends AbstractProcessor {

    private Path baseJar;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        /*Map<String, String> options = processingEnv.getOptions();
        checkArgument(options.containsKey("baseJar"), "Missing baseJar argument");
        this.baseJar = Paths.get(options.get("baseJar"));
        checkArgument(Files.exists(baseJar), "Base JAR does not exist");*/
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        List<TypeElement> mappingClasses = new ArrayList<>();

        for (Element element : roundEnv.getElementsAnnotatedWith(Mapping.class)) {
            if (element instanceof TypeElement) {
                mappingClasses.add((TypeElement) element);
            }
        }

        if (mappingClasses.isEmpty()) {
            return true;
        }

        try {
            FileObject file = this.processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "mappings.json");

            Map<String, MappedClass> mappings;
            try (Reader reader = file.openReader(false)) {
                mappings = Mappings.read(reader);
            } catch (IOException ignored) {
                mappings = new HashMap<>();
            }

            ClassMapper classMappings = createMapper(mappingClasses);

            // We need to remap the descriptors of the fields and methods, use ASM for convenience
            Remapper unmapper = classMappings.createUnmapper();

            for (TypeElement mappingClass : mappingClasses) {
                Mapping annotation = mappingClass.getAnnotation(Mapping.class);
                MappedClass mapping = new MappedClass(Strings.emptyToNull(annotation.value()));

                Accessible accessible = mappingClass.getAnnotation(Accessible.class);
                if (accessible != null) {
                    mapping.getAccess().put("", parseAccessible(accessible));
                }

                for (Element element : mappingClass.getEnclosedElements()) {
                    annotation = element.getAnnotation(Mapping.class);
                    if (annotation == null) {
                        continue;
                    }

                    accessible = element.getAnnotation(Accessible.class);

                    String mappedName = annotation.value();
                    checkArgument(!mappedName.isEmpty(), "Mapping detection is not supported yet");

                    switch (element.getKind()) {
                        case METHOD:
                            ExecutableElement method = (ExecutableElement) element;
                            String methodName = method.getSimpleName().toString();
                            String methodDesc = getDescriptor(method);
                            mapping.getMethods().put(mappedName + unmapper.mapMethodDesc(methodDesc), methodName);

                            if (accessible != null) {
                                mapping.getAccess().put(methodName + methodDesc, parseAccessible(accessible));
                            }

                            break;
                        case FIELD:
                        case ENUM_CONSTANT:
                            VariableElement field = (VariableElement) element;
                            String fieldName = field.getSimpleName().toString();
                            mapping.getFields().put(mappedName + ':' + unmapper.mapDesc(getDescriptor(field)), fieldName);

                            if (accessible != null) {
                                mapping.getAccess().put(fieldName, parseAccessible(accessible));
                            }

                            break;
                        default:
                    }
                }

                mappings.put(getInternalName(mappingClass), mapping);
            }

            // Generate JSON output
            file = this.processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "mappings.json");
            try (Writer writer = file.openWriter()) {
                Mappings.write(writer, mappings);
            }

            return true;
        } catch (IOException e) {
            this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("Failed to create mappings.json", e);
        }
    }

    private static ClassMapper createMapper(List<TypeElement> mappingClasses) {
        ImmutableBiMap.Builder<String, String> classes = ImmutableBiMap.builder();

        for (TypeElement element : mappingClasses) {
            String mapping = element.getAnnotation(Mapping.class).value();
            if (!mapping.isEmpty()) {
                classes.put(mapping, getInternalName(element));
            }
        }

        return new ClassMapper(classes.build());
    }

    private static AccessTransform parseAccessible(Accessible accessible) {
        return new AccessTransform(accessible.access(), accessible.removeFinal());
    }

}
