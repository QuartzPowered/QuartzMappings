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
package net.minecrell.quartz.mappings.processor.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public final class Elements {

    private Elements() {}

    public static void getInternalName(TypeElement element, StringBuilder builder) {
        int pos = builder.length();
        builder.append(element.getSimpleName());

        Element parent = element.getEnclosingElement();
        while (parent != null) {
            if (parent instanceof TypeElement) {
                builder.insert(pos, '$').insert(pos, parent.getSimpleName());
            } else if (parent instanceof PackageElement) {
                builder.insert(pos, '/').insert(pos, ((PackageElement) parent).getQualifiedName().toString().replace('.', '/'));
            }

            parent = parent.getEnclosingElement();
        }
    }

    public static void getInternalName(DeclaredType type, StringBuilder builder) {
        getInternalName((TypeElement) type.asElement(), builder);
    }

    public static String getInternalName(TypeElement element) {
        StringBuilder result = new StringBuilder();
        getInternalName(element, result);
        return result.toString();
    }

    public static void getDescriptor(TypeMirror type, StringBuilder builder) {
        switch (type.getKind()) {
            case BOOLEAN:
                builder.append('Z');
                break;
            case BYTE:
                builder.append('B');
                break;
            case SHORT:
                builder.append('S');
                break;
            case INT:
                builder.append('I');
                break;
            case LONG:
                builder.append('J');
                break;
            case CHAR:
                builder.append('C');
                break;
            case FLOAT:
                builder.append('F');
                break;
            case DOUBLE:
                builder.append('D');
                break;
            case VOID:
                builder.append('V');
                break;
            case DECLARED:
                builder.append('L');
                getInternalName((DeclaredType) type, builder);
                builder.append(';');
                break;
            case ARRAY:
                builder.append('[');
                getDescriptor(((ArrayType)type).getComponentType(), builder);
                break;
            default:
                throw new IllegalArgumentException("Unable to parse type symbol " + type + " with " + type.getKind() + " to equivalent "
                    + "bytecode type");
        }
    }

    public static void getDescriptor(VariableElement element, StringBuilder builder) {
        getDescriptor(element.asType(), builder);
    }

    public static String getDescriptor(ExecutableElement method) {
        StringBuilder result = new StringBuilder();

        result.append('(');
        for (VariableElement parameter : method.getParameters()) {
            getDescriptor(parameter, result);
        }
        result.append(')');

        getDescriptor(method.getReturnType(), result);
        return result.toString();
    }

    public static String getDescriptor(VariableElement element) {
        StringBuilder result = new StringBuilder();
        getDescriptor(element, result);
        return result.toString();
    }

}
