package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** Sync facades are hand-written delegations; this keeps them aligned with the single async implementation. */
class DomainApiParityTest {

    @Test
    void everyDomainIsReachableFromBothEntryPoints() {
        Map<String, Class<?>> sync = accessors(TS3Api.class);
        Map<String, Class<?>> async = accessors(TS3ApiAsync.class);
        assertEquals(sync.keySet(), async.keySet());
        assertEquals(16, sync.size());
        sync.forEach((name, type) -> {
            assertEquals(type.getSimpleName() + "Async", async.get(name).getSimpleName(), name);
            assertTrue(
                    Modifier.isFinal(type.getModifiers())
                            && Modifier.isFinal(async.get(name).getModifiers()),
                    name);
            assertEquals(
                    0,
                    type.getConstructors().length + async.get(name).getConstructors().length,
                    "Domains are created by the query only");
        });
    }

    @Test
    void syncAndAsyncOperationsMatchExactly() {
        Map<String, Class<?>> async = accessors(TS3ApiAsync.class);
        accessors(TS3Api.class).forEach((name, sync) -> assertParity(sync, async.get(name)));
        assertParity(TS3Api.class, TS3ApiAsync.class);
    }

    private static void assertParity(Class<?> sync, Class<?> async) {
        Map<String, Method> syncMethods = operations(sync);
        Map<String, Method> asyncMethods = operations(async);
        assertEquals(syncMethods.keySet(), asyncMethods.keySet(), sync.getSimpleName());
        syncMethods.forEach((signature, method) -> {
            Method asyncMethod = asyncMethods.get(signature);
            if (asyncMethod.getReturnType() == CompletableFuture.class) {
                Type result = ((ParameterizedType) asyncMethod.getGenericReturnType()).getActualTypeArguments()[0];
                assertEquals(
                        unboxed(result).getTypeName(),
                        method.getGenericReturnType().getTypeName(),
                        signature);
            } else {
                assertEquals(asyncMethod.getGenericReturnType(), method.getGenericReturnType(), signature);
            }
        });
    }

    private static Type unboxed(Type type) {
        if (type == Void.class) return void.class;
        if (type == Integer.class) return int.class;
        if (type == Boolean.class) return boolean.class;
        if (type == Long.class) return long.class;
        return type;
    }

    private static Map<String, Class<?>> accessors(Class<?> entryPoint) {
        Map<String, Class<?>> domains = new TreeMap<>();
        for (Method method : entryPoint.getMethods()) {
            if (method.getParameterCount() == 0
                    && method.getReturnType().getSimpleName().matches("\\w+Api(Async)?")
                    && method.getDeclaringClass() == entryPoint) {
                domains.put(method.getName(), method.getReturnType());
            }
        }
        return domains;
    }

    private static Map<String, Method> operations(Class<?> type) {
        Map<String, Method> methods = new TreeMap<>();
        for (Method method : type.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())
                    || method.getReturnType().getSimpleName().matches("\\w+Api(Async)?")) continue;
            methods.put(method.getName() + Arrays.toString(method.getGenericParameterTypes()), method);
        }
        return methods;
    }
}
