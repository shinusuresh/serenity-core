package net.thucydides.core.reflection;

import net.serenitybdd.core.collect.NewList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class StackTraceAnalyser {

    private final StackTraceElement stackTraceElement;
    private final Logger logger = LoggerFactory.getLogger(StackTraceAnalyser.class);

    private StackTraceAnalyser(StackTraceElement stackTraceElement) {
        this.stackTraceElement = stackTraceElement;
    }

    public static StackTraceAnalyser forStackTraceElement(StackTraceElement stackTraceElement) {
        return new StackTraceAnalyser(stackTraceElement);
    }

    public Method getMethod() {
        try {
            if (allowedClassName(stackTraceElement.getClassName()) && !lambda(stackTraceElement.getClassName())) {
                Class callingClass = Class.forName(stackTraceElement.getClassName());
                return extractMethod(stackTraceElement, callingClass);
            }
        } catch (ClassNotFoundException classNotFoundIgnored) {
            logger.debug("Couldn't find class during Stack analysis: " + classNotFoundIgnored.getLocalizedMessage());
        } catch (NoClassDefFoundError noClassDefFoundErrorIgnored) {
            logger.debug("Couldn't find class definition during Stack analysis: " + noClassDefFoundErrorIgnored.getLocalizedMessage());
        }
        return null;
    }

    public Method getUnfilteredMethod() {
        try {
            Class callingClass = Class.forName(stackTraceElement.getClassName());
            Method matchingMethod = extractMethod(stackTraceElement, callingClass);
            if (matchingMethod != null) {
                return matchingMethod;
            }
        } catch (ClassNotFoundException classNotFoundIgnored) {
            logger.debug("Couldn't find class during Stack analysis: " + classNotFoundIgnored.getLocalizedMessage());
        } catch (NoClassDefFoundError noClassDefFoundErrorIgnored) {
            logger.debug("Couldn't find class definition during Stack analysis: " + noClassDefFoundErrorIgnored.getLocalizedMessage());
        }
        return null;
    }

    private boolean lambda(String className) {
        return className.contains("$$Lambda$");
    }

    public static Method extractMethod(StackTraceElement stackTraceElement, Class callingClass) {
        Class targetClass;
        if (isInstrumentedMethod(stackTraceElement)) {
            targetClass = callingClass.getSuperclass();
        } else {
            targetClass = callingClass;
        }
        try {
            Method methodFound = targetClass.getMethod(stackTraceElement.getMethodName());
            if (methodFound == null ){
                methodFound = targetClass.getDeclaredMethod(stackTraceElement.getMethodName());
            }
            return methodFound;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static boolean isInstrumentedMethod(StackTraceElement stackTraceElement) {
        return StringUtils.isNotEmpty(stackTraceElement.getFileName()) && (stackTraceElement.getFileName().equals("<generated>"));
    }

    private final static List<String> HIDDEN_PACKAGES = NewList.of("sun.","java","org.gradle");
    private boolean allowedClassName(String className) {
        if (className.contains("$")) { return false; }
        if (inHiddenPackage(className)) { return false; }

        return true;
    }

    private boolean inHiddenPackage(String className) {
        for(String hiddenPackage : HIDDEN_PACKAGES) {
            if (className.startsWith(hiddenPackage)) { return true; }
        }
        return false;
    }

    public static List<Method> inscopeMethodsIn(StackTraceElement[] stackTrace) {
        List<Method> methods = new ArrayList<>();
        for (StackTraceElement stackTraceElement : stackTrace) {
            Method method = StackTraceAnalyser.forStackTraceElement(stackTraceElement).getMethod();
            if (method != null) {
                methods.add(method);
            }
        }
        return methods;
    }

}
