/*
 *
 *  Managed Data Structures
 *  Copyright © 2016 Hewlett Packard Enterprise Development Company LP.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  As an exception, the copyright holders of this Library grant you permission
 *  to (i) compile an Application with the Library, and (ii) distribute the 
 *  Application containing code generated by the Library and added to the 
 *  Application during this compilation process under terms of your choice, 
 *  provided you also meet the terms and conditions of the Application license.
 *
 */

package com.hpl.mds.annotations.processor.parser;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import com.hpl.mds.annotations.Emitted;
import com.hpl.mds.annotations.Managed;
import com.hpl.mds.annotations.No;
import com.hpl.mds.annotations.Private;
import com.hpl.mds.annotations.Protected;
import com.hpl.mds.annotations.Public;
import com.hpl.mds.annotations.processor.RecordInfo.DataType;
import com.hpl.mds.annotations.processor.RecordInfo.MethodInfo;
import com.hpl.mds.annotations.processor.RecordInfo.VarInfo;
import com.hpl.mds.annotations.processor.RecordInfo.Visibility;

/**
 * Parses declared methods in a record schema in a generic way, including
 * instance, abstract, static methods and constructors.
 * 
 * The parsed information is returned in the form of a {@link MethodInfo}
 *
 * @author Abraham Alcantara
 */
public class MethodParser {

    /**
     * To log messages for debugging purposes
     */
    private static final Logger LOGGER = Logger.getLogger(MethodParser.class.getName());

    /**
     * To display localized messages to the user
     */
    private final Messager messager;

    /**
     * Parser to extract data types in methods arguments and return value
     */
    private final DataTypeParser dataTypeParser;

    public MethodParser(DataTypeParser dataTypeParser, Messager messager) {
        this.dataTypeParser = dataTypeParser;
        this.messager = messager;
    }

    /**
     * Parses a method
     * 
     * @param method
     *            element to parse
     * @param firstParam
     *            position of the parameter to start parsing
     * @param schemaContext
     * @return {@link MethodInfo} containing all parsed data
     * @throws ProcessingException
     */
    public MethodInfo parse(ExecutableElement method, int firstParam, SchemaContext schemaContext)
            throws ProcessingException {
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.setMethod(method);
        methodInfo.setParameters(parseInstanceMethodParams(method.getParameters(), firstParam, schemaContext));
        methodInfo.setName(method.getSimpleName().toString());
        methodInfo.setReturnType(parseVarInfo(method.getReturnType(), null, method, schemaContext));
        methodInfo.setVisibility(parseMethodVisibility(method));
        return methodInfo;
    }

    /**
     * Parses the given method parameters
     * 
     * @param parameters
     *            elements to parse
     * @param firstParam
     * @param schemaContext
     * @return The associated {@link VarInfo} list of parameters
     * @throws ProcessingException
     */
    private List<VarInfo> parseInstanceMethodParams(List<? extends VariableElement> parameters, int firstParam,
            SchemaContext schemaContext) throws ProcessingException {
        if (parameters.size() > firstParam) {
            List<VarInfo> params = new ArrayList<>(parameters.size());
            for (int i = firstParam; i < parameters.size(); i++) {
                VariableElement parameter = parameters.get(i);
                String name = parameter.getSimpleName().toString();
                params.add(parseVarInfo(parameter.asType(), name, parameter, schemaContext));
            }
            return params;
        }
        return Collections.emptyList();
    }

    /**
     * Parses method parameters and return types
     * 
     * @param typeMirror
     *            data type to parse
     * @param name
     *            name of the parameter, null if return type
     * @param element
     *            {@link Managed} annotated element
     * @param schemaContext
     * @throws ProcessingException
     */
    private VarInfo parseVarInfo(TypeMirror typeMirror, String name, Element element, SchemaContext schemaContext)
            throws ProcessingException {
        VarInfo varInfo = new VarInfo(name);
        if (element.getAnnotation(Managed.class) != null) {
            if (dataTypeParser.parse(typeMirror, varInfo, element, schemaContext.getPkg(),
                    schemaContext.getRecordSimpleName()) && varInfo.getType().twoVersions()) {
                varInfo.setEmittedTwice();
            } else {
                varInfo.setComplexType(typeMirror.toString());
                LOGGER.info("parsed as complex type: " + typeMirror.toString());
                messager.printMessage(Kind.ERROR,
                        "Ignoring annotation, invalid data type for annotation: " + Managed.class, element);
            }
        } else if (TypeKind.VOID.equals(typeMirror.getKind())) {
            varInfo.setType(DataType.VOID);
        } else {
            try {
                if (dataTypeParser.parse(typeMirror, varInfo, element, schemaContext.getPkg(),
                        schemaContext.getRecordSimpleName())) {
                    return varInfo;
                }
            } catch (ProcessingException e) {
            }
            varInfo.setType(null);
            varInfo.setComplexType(typeMirror.toString());
            LOGGER.info("parsed as complex type: " + typeMirror.toString());
        }
        return varInfo;
    }

    /**
     * Parses visibility level of the given method
     * 
     * @param method
     *            instance method to parse
     * @return the parsed visibility level
     */
    private Visibility parseMethodVisibility(ExecutableElement method) {
        Visibility visibility = null;
        if (isPresent(method, Private.class, Private::value)) {
            visibility = Visibility.PRIVATE;
        }
        if (isPresent(method, Protected.class, Protected::value)) {
            if (visibility != null) {
                messager.printMessage(Kind.ERROR,
                        "Ignoring annotation. only one visibility annotation allowed, using: " + visibility, method);
            }
            visibility = Visibility.PROTECTED;
        }
        if (isPresent(method, Public.class, Public::value)) {
            if (visibility != null) {
                messager.printMessage(Kind.ERROR,
                        "Ignoring annotation. only one visibility annotation allowed, using: " + visibility, method);
            }
            visibility = Visibility.PUBLIC;
        }
        if (isPresent(method, No.class, No::value)) {
            messager.printMessage(Kind.ERROR, "Ignoring annotation. illegal visibility value: " + No.class, method);
        }
        return visibility;
    }

    /**
     * Parses visibility values for the given annotation
     * 
     * @param method
     *            the annotated element
     * @param class1
     *            the annotation type
     * @param parser
     *            the parser to get the {@link Emitted} values
     * @return true if the annotation is present
     */
    private <A extends Annotation> boolean isPresent(ExecutableElement method, Class<A> class1,
            Function<A, Emitted[]> parser) {
        A[] annotations = method.getAnnotationsByType(class1);
        if (annotations != null && annotations.length > 0) {
            for (A annotation : annotations) {
                Emitted[] emitteds = parser.apply(annotation);
                for (Emitted emitted : emitteds) {
                    if (!Emitted.DEFAULT.equals(emitted)) {
                        messager.printMessage(Kind.ERROR,
                                "Ignoring annotation. illegal visibility value: " + annotation, method);
                    }
                }
            }
            return true;
        }
        return false;
    }
}
