/*
 *  Copyright 2009 henri.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.commons.jexl;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Helper class to test GC / reference interactions.
 * Dynamically creates a class by compiling generated source Java code and
 * load it through a dedicated class loader.
 */
public class ClassCreator {
    private final File base;
    private File packageDir = null;
    private int seed = 0;
    private String className = null;
    private String sourceName = null;
    private ClassLoader loader = null;

    public ClassCreator(File theBase) throws Exception {
        base = theBase;
    }

    public void clear() {
        seed = 0;
        packageDir = null;
        className = null;
        sourceName = null;
        packageDir = null;
        loader = null;
    }

    public void setSeed(int s) {
        seed = s;
        className = "foo" + s;
        sourceName = className + ".java";
        packageDir = new File(base, seed + "/org/apache/commons/jexl/generated");
        packageDir.mkdirs();
        loader = null;
    }

    public String getClassName() {
        return "org.apache.commons.jexl.generated." + className;
    }

    public Class<?> getClassInstance() throws Exception {
        return getClassLoader().loadClass("org.apache.commons.jexl.generated." + className);
    }

    public ClassLoader getClassLoader() throws Exception {
        if (loader == null) {
            URL classpath = (new File(base, Integer.toString(seed))).toURI().toURL();
            loader = new URLClassLoader(new URL[]{classpath}, null);
        }
        return loader;
    }

    public Class<?> createClass() throws Exception {
        // generate, compile & validate
        generate();
        Class<?> clazz = compile();
        if (clazz == null) {
            throw new Exception("failed to compile foo" + seed);
        }
        Object v = validate(clazz);
        if (v instanceof Integer && ((Integer) v).intValue() == seed) {
            return clazz;
        }
        throw new Exception("failed to validate foo" + seed);
    }

    void generate() throws Exception {
        FileWriter aWriter = new FileWriter(new File(packageDir, sourceName), false);
        aWriter.write("package org.apache.commons.jexl.generated;");
        aWriter.write("public class " + className + "{\n");
        aWriter.write("private int value =");
        aWriter.write(Integer.toString(seed));
        aWriter.write(";\n");
        aWriter.write(" public void setValue(int v) {");
        aWriter.write(" value = v;");
        aWriter.write(" }\n");
        aWriter.write(" public int getValue() {");
        aWriter.write(" return value;");
        aWriter.write(" }\n");
        aWriter.write(" }\n");
        aWriter.flush();
        aWriter.close();
    }

    Class<?> compile() throws Exception {
        String[] source = {packageDir.getPath() + "/" + sourceName};
        if (com.sun.tools.javac.Main.compile(source) >= 0) {
            return getClassLoader().loadClass("org.apache.commons.jexl.generated." + className);
        }
        return null;
    }

    Object validate(Class<?> clazz) throws Exception {
        Class<?> params[] = {};
        Object paramsObj[] = {};
        Object iClass = clazz.newInstance();
        Method thisMethod = clazz.getDeclaredMethod("getValue", params);
        return thisMethod.invoke(iClass, paramsObj);
    }
}
