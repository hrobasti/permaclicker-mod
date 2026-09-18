package com.github.hrobasti.permaclicker.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Generic Minecraft-version-compat reflection helpers used by both loader entrypoints to call
 * methods whose exact shape/name can differ slightly across Minecraft versions.
 */
public final class ReflectionCompat {
    private ReflectionCompat() {
    }

    public static Method getAccessibleMethod(Class<?> owner, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            Method declared = owner.getDeclaredMethod(name, parameterTypes);
            try {
                declared.setAccessible(true);
            } catch (Throwable ignoredSetAccessible) {
                // best effort for stricter access rules
            }
            return declared;
        }
    }

    public static Field getAccessibleField(Class<?> owner, String name) throws NoSuchFieldException {
        try {
            return owner.getField(name);
        } catch (NoSuchFieldException ignored) {
            Field declared = owner.getDeclaredField(name);
            try {
                declared.setAccessible(true);
            } catch (Throwable ignoredSetAccessible) {
                // best effort
            }
            return declared;
        }
    }

    public static boolean invokeCompatibleMethod(Object receiver, String methodName, Object... args) {
        for (Class<?> type = receiver.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                if (!isCompatible(method.getParameterTypes(), args)) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                } catch (Throwable ignored) {
                    // best effort
                }

                try {
                    method.invoke(receiver, args);
                    return true;
                } catch (Throwable ignored) {
                    // try next candidate
                }
            }
        }

        return false;
    }

    public static boolean invokeCompatibleMethodByShape(Object receiver, Object[] args) {
        for (Class<?> type = receiver.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!isCompatible(method.getParameterTypes(), args)) {
                    continue;
                }

                if (method.getReturnType() != void.class) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                } catch (Throwable ignored) {
                    // best effort
                }

                try {
                    method.invoke(receiver, args);
                    return true;
                } catch (Throwable ignored) {
                    // try next candidate
                }
            }
        }

        return false;
    }

    public static boolean isCompatible(Class<?>[] parameterTypes, Object[] args) {
        if (parameterTypes.length != args.length) {
            return false;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            if (!isAssignable(parameterTypes[i], args[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAssignable(Class<?> parameterType, Object arg) {
        if (arg == null) {
            return !parameterType.isPrimitive();
        }

        if (parameterType.isPrimitive()) {
            if (parameterType == int.class) {
                return arg instanceof Integer;
            }
            if (parameterType == long.class) {
                return arg instanceof Long;
            }
            if (parameterType == boolean.class) {
                return arg instanceof Boolean;
            }
            if (parameterType == float.class) {
                return arg instanceof Float;
            }
            if (parameterType == double.class) {
                return arg instanceof Double;
            }
            if (parameterType == byte.class) {
                return arg instanceof Byte;
            }
            if (parameterType == short.class) {
                return arg instanceof Short;
            }
            if (parameterType == char.class) {
                return arg instanceof Character;
            }
            return false;
        }

        return parameterType.isInstance(arg);
    }

    public static Boolean readBooleanFromOption(Object option) {
        if (option == null) {
            return null;
        }

        for (String methodName : new String[] { "get", "value" }) {
            try {
                Method getter = getAccessibleMethod(option.getClass(), methodName);
                Object value = getter.invoke(option);
                if (value instanceof Boolean bool) {
                    return bool;
                }
            } catch (Throwable ignored) {
                // try next getter candidate
            }
        }
        return null;
    }

    public static void writeBooleanToOption(Object option, boolean value) {
        if (option == null) {
            return;
        }

        try {
            Method setPrimitive = getAccessibleMethod(option.getClass(), "set", boolean.class);
            setPrimitive.invoke(option, value);
            return;
        } catch (Throwable ignored) {
            // try boxed overload
        }

        try {
            Method setBoxed = getAccessibleMethod(option.getClass(), "set", Object.class);
            setBoxed.invoke(option, Boolean.valueOf(value));
            return;
        } catch (Throwable ignored) {
            // try setValue variant
        }

        try {
            Method setValue = getAccessibleMethod(option.getClass(), "setValue", Object.class);
            setValue.invoke(option, Boolean.valueOf(value));
        } catch (Throwable ignored) {
            // best effort
        }
    }
}
