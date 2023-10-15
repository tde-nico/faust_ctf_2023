package de.faust.auction;

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ReflectionHelper {
    private ReflectionHelper() {}

    public static Stream<Method> collectAllRemoteMethodsFromClass(Class<?> clazz) {
        return doCollectAllRemoteMethodsFromClass(clazz)
                .distinct();
    }

    private static Stream<Method> doCollectAllRemoteMethodsFromClass(Class<?> clazz) {
        if (!Remote.class.isAssignableFrom(clazz)) {
            return Stream.of();
        }
        Stream<Method> fromInterfaces = Arrays.stream(clazz.getInterfaces())
                .flatMap(ReflectionHelper::doCollectAllRemoteMethodsFromClass);
        Stream<Method> fromSuperclass = fromInterfaces;
        if (clazz.getSuperclass() != null) {
            fromSuperclass = Stream.concat(
                    // add from interfaces
                    fromInterfaces,
                    // collect from superclass
                    doCollectAllRemoteMethodsFromClass(clazz.getSuperclass()));
        }
        return Stream.concat(
                // add from superclass
                fromSuperclass,
                // add methods from this class/interface
                Arrays.stream(clazz.getDeclaredMethods())
        );
    }
}
