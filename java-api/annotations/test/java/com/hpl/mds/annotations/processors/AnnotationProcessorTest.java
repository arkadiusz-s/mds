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

package com.hpl.mds.annotations.processors;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.github.javaparser.ParseException;

/**
 * Integration tests for MDS Annotation Processor
 */
public class AnnotationProcessorTest {

    interface AddMethod {
        void add(String methodName, String returnType, String... args);
    }

    private static final String PKG = "com.hpl.mds.test.valid.";
    private static final String PATH = "com/hpl/mds/test/valid/";

    private static final String MNG_ARRAY_BOOLEAN = "com.hpl.mds.prim.container.array.ManagedBooleanArray";
    private static final String MNG_ARRAY_INT = "com.hpl.mds.prim.container.array.ManagedIntArray";
    private static final String MNG_ARRAY = "com.hpl.mds.ManagedArray";
    private static final String MNG_STRING = "com.hpl.mds.string.ManagedString";
    private static final String MNG_SHORT = "com.hpl.mds.prim.ManagedShort";
    private static final String MNG_LONG = "com.hpl.mds.prim.ManagedLong";
    private static final String MNG_INT = "com.hpl.mds.prim.ManagedInt";
    private static final String MNG_FLOAT = "com.hpl.mds.prim.ManagedFloat";
    private static final String MNG_DOUBLE = "com.hpl.mds.prim.ManagedDouble";
    private static final String MNG_BYTE = "com.hpl.mds.prim.ManagedByte";
    private static final String MNG_BOOLEAN = "com.hpl.mds.prim.ManagedBoolean";

    private static final String OBJ_STRING = String.class.getCanonicalName();
    private static final String OBJ_LIST = List.class.getCanonicalName();
    private static final String OBJ_URL = URL.class.getCanonicalName();
    private static final String RECORD_SCHEMA = "com/hpl/mds/test/RecordPlainSimpleSchema.java";
    private static final String RECORD_QNAME = "com.hpl.mds.test.RecordPlainSimple";
    private static final String RECORD_GENERATED = "com/hpl/mds/test/RecordPlainSimple.java";

    private static final JCompiler COMPILER = JCompiler.getInstance();

    private ParserConfig config;
    private Util test = new Util(PATH, PKG);

    @Before
    public void beforeTest() {
        config = new ParserConfig();
        test.setConfig(config);
    }

    @Test
    public void shouldGenerateAttributesWithEmptyParadigms() throws Exception {
        testGeneratedMethodsWithParadigm("RecordEmptyParadigm", "TestEmptyParadigm");
    }

    @Test
    public void shouldGenerateAttributesWithMultiParadigms() throws Exception {
        testGeneratedMethodsWithParadigm("RecordMultiParadigm", "TestEmptyParadigm", "TestParadigmWithParent",
                "TestParadigm");
    }

    @Test
    public void shouldGenerateAttributesWithParadigmsWithParent() throws Exception {
        testGeneratedMethodsWithParadigm("RecordParadigmWithParent", "TestParadigmWithParent", "TestParadigm");
    }

    @Test
    public void shouldGenerateAttributesWithParadigms() throws Exception {
        testGeneratedMethodsWithParadigm("RecordWithParadigm", "TestParadigm");
    }

    @Test
    public void shouldGenerateRecordWithInheritance() throws Exception {
        String className = "Inheritance";
        String parent = "InheritanceParent";
        config.setParent(PKG + parent);
        config.addAmbiguousMethod(new MethodDesc("publicMethod", "void", "int val"));
        config.addAmbiguousMethod(new MethodDesc("protectedMethod", "void", "int val"));
        config.addAmbiguousMethod(new MethodDesc("privateMethod", "void", "int val"));
        COMPILER.compileSchema(PATH + parent + "Schema.java", PATH + className + "Schema.java");
        test.testGeneratedRecord(className);
    }

    @Test
    public void shouldGenerateInstanceMethodsWithExistentComplexTypes() throws Exception {
        test.addPublic("getURL", OBJ_URL);
        test.addPublic("getURL", OBJ_URL, InputStream.class.getCanonicalName() + " in");
        test.addPublic("getURLs", OBJ_LIST + "<" + OBJ_URL + ">", InputStream.class.getCanonicalName() + " in");
        test.addPublic("setURLs", "void", OBJ_LIST + "<" + OBJ_URL + "> urls");
        test.addPublic("print", "void");
        test.addPublic("print", "void", PrintStream.class.getCanonicalName() + " out");
        test.testSchema("MethodsOtherTypes");
    }

    @Test
    public void shouldGenerateMethodsWithCollectionsReturnTypes() throws Exception {
        test.addPublic("listRecordReturnMethod", OBJ_LIST + "<" + RECORD_QNAME + ">");
        test.testWithOffPkgClasses("MethodsCollectionsReturn", RECORD_SCHEMA);
    }

    @Test
    public void shouldGenerateMethodsWithArrayParams() throws Exception {
        String className = "MethodsArray";
        test.addPublic("booleanArrayMethod", "void", MNG_ARRAY_BOOLEAN + " array");
        test.addPublic("booleanArrayMethod2", "void", MNG_ARRAY + "<" + MNG_BOOLEAN + ">" + " array");
        String arrayParam = MNG_ARRAY_INT + " array";
        test.addPublic("intArrayMethod", "void", arrayParam);
        String intArrayType = MNG_ARRAY + "<" + MNG_INT + ">";
        test.addPublic("intArrayMethod2", "void", intArrayType + " array");
        test.addPublic("returnArrayMethod", MNG_ARRAY_INT);
        test.addPublic("returnArrayMethod2", intArrayType);
        test.addPublic("returnRecordArrayMethod", MNG_ARRAY + "<" + RECORD_QNAME + ">");
        addConstructor(arrayParam);
        addPublicCreationMethod(className, arrayParam);
        addStaticPublic("staticMethod", "void", arrayParam);
        test.testWithOffPkgClasses(className, RECORD_SCHEMA);
    }

    @Test
    public void shouldGenerateAbstractMethods() throws Exception {
        config.addPublic(new MethodDesc("publicMethod", "void", "int val"));
        config.addProtected(new MethodDesc("protectedMethod", "void", "int val"));
        test.testSchema("MethodsAbstract");
    }

    @Test
    public void shouldGenerateStaticMethods() throws Exception {
        addStaticPublic("publicMethod", "void", "int val");
        addStaticProtected("protectedMethod", "void", "int val");
        config.addStaticPrivate(new MethodDesc("privateMethod", "void", "int val"));
        addStaticPublic("staticMethod", "void", "int val");
        addStaticPublic("instanceLikeMethod", "void", "MethodsStatic i1", "MethodsStatic i2");
        addStaticPublic("instanceLikeMethod", "void", "MethodsStatic.Private i1", "MethodsStatic i2");
        addStaticPublic("instanceLikeMethod", "void", "MethodsStatic i1", "MethodsStatic.Private i2");
        addStaticPublic("instanceLikeMethod", "void", "MethodsStatic.Private i1", "MethodsStatic.Private i2");
        addStaticPublic("methodWithManagedArgs", "void", "int val");
        addStaticPublic("methodWithManagedArgs", "void", MNG_INT + " val");
        test.testSchema("MethodsStatic");
    }

    @Test
    public void shouldGenerateAbstractMethodsWithArrayParams() throws Exception {
        config.addPublic(new MethodDesc("abstractMethod", "void", MNG_ARRAY_INT + " array"));
        test.testSchema("MethodsArrayAbstract");
    }

    @Test
    public void shouldGenerateAbstractImpl() throws Exception {
        config.setAbstractRecord(true);
        test.testSchema("RecordAbstract");
    }

    @Test
    public void shouldGenerateConstructorWithRecordArgsTypes() throws Exception {
        addPublicCreationMethod("ConstructorRecord", RECORD_QNAME + " record");
        addConstructor(RECORD_QNAME + " record");
        test.testWithOffPkgClasses("ConstructorRecord", RECORD_SCHEMA);
    }

    @Test
    public void shouldGenerateDefaultConstructor() throws Exception {
        addPublicCreationMethod("RecordPlainSimple");
        addConstructor();
        testPlainSimpleRecord();
    }

    private void testPlainSimpleRecord() throws ClassNotFoundException, ParseException, IOException {
        config.setMdsType(RECORD_QNAME);
        config.setClassName("RecordPlainSimple");
        assertEquals(0, COMPILER.compileSchema(RECORD_SCHEMA));
        COMPILER.compileLoadRecord(RECORD_GENERATED);
        COMPILER.parse(RECORD_GENERATED, config);
    }

    @Test
    public void shouldGenerateConstructors() throws Exception {
        addConstructor("int public1");
        addPublicCreationMethod("Constructor", "int public1");
        addConstructor("long protected1");
        addProtectedCreationMethod("Constructor", "long protected1");
        addConstructor(OBJ_STRING + " private1");
        addPrivateCreationMethod("Constructor", OBJ_STRING + " private1");
        addPrivateCreationMethod("Constructor", MNG_STRING + " private1");
        addConstructor("double managed", "boolean public1");
        addPublicCreationMethod("Constructor", "double managed", "boolean public1");
        addPublicCreationMethod("Constructor", MNG_DOUBLE + " managed", "boolean public1");
        test.testSchema("Constructor");
    }

    @Test
    public void shouldGenerateInstanceMethodsWithRecordArgsTypes() throws Exception {
        test.addPublic("recordMethod", "void", RECORD_QNAME + " val");
        test.addPublic("recordMethod", "void", RECORD_QNAME + ".Protected val");
        test.testWithOffPkgClasses("MethodsRecord", RECORD_SCHEMA);
    }

    @Test
    public void shouldGenerateInstanceMethodsWithALotOfArguments() throws Exception {
        test.addPublic("oneParam", "void", "int arg0");
        test.addPublic("oneParam", "void", MNG_INT + " arg0");
        test.addPublic("twoParams", "void", "int arg0", "int arg1");
        test.addPublic("twoParams", "void", "int arg0", MNG_INT + " arg1");
        test.addPublic("threeParams", "void", "int arg0", "int arg1", "int arg2");
        test.addPublic("threeParams", "void", MNG_INT + " arg0", "int arg1", "int arg2");
        test.addPublic("threeParams", "void", "int arg0", "int arg1", MNG_INT + " arg2");
        test.addPublic("threeParams", "void", MNG_INT + " arg0", "int arg1", MNG_INT + " arg2");
        test.addPublic("fourParams", "void", "int arg0", "int arg1", "int arg2", "int arg3");
        test.addPublic("fourParams", "void", "int arg0", MNG_INT + " arg1", "int arg2", "int arg3");
        test.addPublic("fourParams", "void", "int arg0", "int arg1", MNG_INT + " arg2", "int arg3");
        test.addPublic("fourParams", "void", "int arg0", MNG_INT + " arg1", MNG_INT + " arg2", "int arg3");
        test.testSchema("MethodsLotOfArgs");
    }

    @Test
    public void shouldGenerateInstanceMethodsWithManagedReturnMngTypes() throws Exception {
        test.addPublic("primIntReturnMethod", "int");
        test.addPublic("intReturnMethod", MNG_INT);
        test.addPublic("primShortReturnMethod", "short");
        test.addPublic("shortReturnMethod", MNG_SHORT);
        test.addPublic("primLongReturnMethod", "long");
        test.addPublic("longReturnMethod", MNG_LONG);
        test.addPublic("primFloatReturnMethod", "float");
        test.addPublic("floatReturnMethod", MNG_FLOAT);
        test.addPublic("primDoubleReturnMethod", "double");
        test.addPublic("doubleReturnMethod", MNG_DOUBLE);
        test.addPublic("primBooleanReturnMethod", "boolean");
        test.addPublic("booleanReturnMethod", MNG_BOOLEAN);
        test.addPublic("primByteReturnMethod", "byte");
        test.addPublic("byteReturnMethod", MNG_BYTE);
        test.addPublic("primStringReturnMethod", OBJ_STRING);
        test.addPublic("stringReturnMethod", MNG_STRING);
        test.testSchema("MethodsMngManagedReturns");
    }

    @Test
    public void shouldGenerateInstanceMethodsWithManagedReturnPrimTypes() throws Exception {
        test.addPublic("intReturnMethod", "int");
        test.addPublic("managedIntReturnMethod", MNG_INT);
        test.addPublic("shortReturnMethod", "short");
        test.addPublic("managedShortReturnMethod", MNG_SHORT);
        test.addPublic("longReturnMethod", "long");
        test.addPublic("managedLongReturnMethod", MNG_LONG);
        test.addPublic("floatReturnMethod", "float");
        test.addPublic("managedFloatReturnMethod", MNG_FLOAT);
        test.addPublic("doubleReturnMethod", "double");
        test.addPublic("managedDoubleReturnMethod", MNG_DOUBLE);
        test.addPublic("booleanReturnMethod", "boolean");
        test.addPublic("managedBooleanReturnMethod", MNG_BOOLEAN);
        test.addPublic("byteReturnMethod", "byte");
        test.addPublic("managedByteReturnMethod", MNG_BYTE);
        test.addPublic("stringReturnMethod", OBJ_STRING);
        test.addPublic("managedStringReturnMethod", MNG_STRING);
        test.testSchema("MethodsMngPrimitiveReturns");
    }

    @Test
    public void shouldGenerateInstanceMethodsWithManagedManagedTypes() throws Exception {
        testInstanceMethodsOfPrimitives("MethodsManagedManaged");
    }

    @Test
    public void shouldGenerateInstanceMethodsWithManagedPrimitiveTypes() throws Exception {
        testInstanceMethodsOfPrimitives("MethodsManagedPrimitive");
    }

    @Test
    public void shouldGenerateInstanceMethods() throws Exception {
        String className = "MethodsInstance";
        test.addPublic("twoParams", OBJ_STRING, OBJ_STRING + " prefix", OBJ_STRING + " suffix");
        test.addPublic("primitiveParam", OBJ_STRING, "int val");
        test.addPublic("recordParam", OBJ_STRING, className + " record");
        test.addPublic("voidReturn", "void", "int val");
        test.addPublic("primitiveReturn", "int", "int val");
        test.addPublic("recordReturn", className, "int val");
        test.addProtected("protectedMethod", "int", "int val");
        test.addPrivate("privateMethod", "int", "int val");
        test.testSchema(className);
    }

    @Test
    public void shouldProcessTypedRecord() throws Exception {
        test.testSchema("TypedRecord", "com.hpl.mds.Test");
    }

    @Test
    public void shouldProcessRecordWithSuperInterfaces() throws Exception {
        config.setSuperInterfaces("TestInterface", "TestInterface2");
        assertEquals(0, COMPILER.compileSchema(PATH + "TestInterface.java", PATH + "TestInterface2.java",
                PATH + "RecordWithInterfaceSchema.java"));
    }

    @Test
    public void shouldProcessPlainRecord() throws Exception {
        testPlainSimpleRecord();
    }
    
    @Test
    public void shouldProcessRecordWithNoPackage() throws Exception {
    	Util util = new Util("", "");
    	util.setConfig(new ParserConfig());
		util.testSchema("NoPackage");
    }

    private void addConstructor(String... args) {
        config.addConstructor(new MethodDesc("Impl", null, args));
    }

    private void addStaticPublic(String methodName, String returnType, String... args) {
        MethodDesc methodDesc = new MethodDesc(methodName, returnType, args);
        config.addStaticPublic(methodDesc);
        config.addStaticPrivate(methodDesc);
    }

    private void addStaticProtected(String methodName, String returnType, String... args) {
        MethodDesc methodDesc = new MethodDesc(methodName, returnType, args);
        config.addStaticProtected(methodDesc);
        config.addStaticPrivate(methodDesc);
    }

    private void addPrivateCreationMethod(String recordType, String... args) {
        config.addPrivateCreationMethod(new MethodDesc("record", recordType + ".Private", args));
        addCreationMethodImpl(recordType, args);
    }

    private void addProtectedCreationMethod(String recordType, String... args) {
        config.addProtectedCreationMethod(new MethodDesc("record", recordType + ".Protected", args));
        config.addPrivateCreationMethod(new MethodDesc("record", recordType + ".Private", args));
        addCreationMethodImpl(recordType, args);
    }

    private void addPublicCreationMethod(String recordType, String... args) {
        config.addPublicCreationMethod(new MethodDesc("record", recordType, args));
        config.addProtectedCreationMethod(new MethodDesc("record", recordType + ".Protected", args));
        config.addPrivateCreationMethod(new MethodDesc("record", recordType + ".Private", args));
        addCreationMethodImpl(recordType, args);
    }

    private void addCreationMethodImpl(String recordType, String... args) {
        config.addCreationMethodImpl(new MethodDesc("record", recordType + ".Impl", args));
    }

    private void testGeneratedMethodsWithParadigm(String className, String... paradigms)
            throws Exception {
        addConstructor("int val");
        test.addPrivate("myMethod", OBJ_STRING, "int val");
        addStaticPublic("staticMethod", "void", "int val");
        test.testWithClasses(className, paradigms);
    }

    private void testInstanceMethodsOfPrimitives(String className) throws Exception {
        test.addPublic("intParamMethod", "void", "int val");
        test.addPublic("intParamMethod", "void", MNG_INT + " val");
        test.addPublic("shortParamMethod", "void", "short val");
        test.addPublic("shortParamMethod", "void", MNG_SHORT + " val");
        test.addPublic("longParamMethod", "void", "long val");
        test.addPublic("longParamMethod", "void", MNG_LONG + " val");
        test.addPublic("floatParamMethod", "void", "float val");
        test.addPublic("floatParamMethod", "void", MNG_FLOAT + " val");
        test.addPublic("doubleParamMethod", "void", "double val");
        test.addPublic("doubleParamMethod", "void", MNG_DOUBLE + " val");
        test.addPublic("booleanParamMethod", "void", "boolean val");
        test.addPublic("booleanParamMethod", "void", MNG_BOOLEAN + " val");
        test.addPublic("byteParamMethod", "void", "byte val");
        test.addPublic("byteParamMethod", "void", MNG_BYTE + " val");
        test.addPublic("stringParamMethod", "void", OBJ_STRING + " val");
        test.addPublic("stringParamMethod", "void", MNG_STRING + " val");
        test.testSchema(className);
    }

}
