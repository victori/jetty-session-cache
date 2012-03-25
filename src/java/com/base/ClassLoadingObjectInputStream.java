package com.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * An {@link ObjectInputStream} that can handle Java primitive types.
 */
public class ClassLoadingObjectInputStream extends ObjectInputStream {
    public ClassLoadingObjectInputStream(final InputStream in) throws IOException {
        super(in);
    }

    public ClassLoadingObjectInputStream() throws IOException {
        super();
    }

    @Override
    public Class resolveClass(final java.io.ObjectStreamClass cl) throws IOException, ClassNotFoundException {
        try {
            String classname = cl.getName();
            Class clazz = null;
            if (classname.equals("byte")) {
                clazz = byte.class;
            } else if (classname.equals("short")) {
                clazz = short.class;
            } else if (classname.equals("int")) {
                clazz = int.class;
            } else if (classname.equals("long")) {
                clazz = long.class;
            } else if (classname.equals("float")) {
                clazz = float.class;
            } else if (classname.equals("double")) {
                clazz = double.class;
            } else if (classname.equals("boolean")) {
                clazz = boolean.class;
            } else if (classname.equals("char")) {
                clazz = char.class;
            } else {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                if (loader == null) {
                    loader = ClassLoadingObjectInputStream.class.getClassLoader();
                }
                clazz = Class.forName(classname, false, loader);
            }
            return clazz;
        } catch (ClassNotFoundException e) {
            return super.resolveClass(cl);
        }
    }
}
