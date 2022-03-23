package io.curity.authenticator.netid.utils;

import java.util.function.Supplier;

public final class ClassLoaderContextUtils
{

    public static final ClassLoader PLUGIN_CLASS_LOADER = ClassLoaderContextUtils.class.getClassLoader();

    public static <T> T withPluginClassLoader(Supplier<T> supplier)
    {
        var thread = Thread.currentThread();

        var oldClassLoader = thread.getContextClassLoader();

        try
        {
            thread.setContextClassLoader(PLUGIN_CLASS_LOADER);

            return supplier.get();
        }
        finally
        {
            thread.setContextClassLoader(oldClassLoader);
        }
    }

}
