package com.interface21.beans.factory.support;

import com.interface21.beans.BeanScanFailureException;
import com.interface21.beans.factory.config.SimpleBeanDefinition;
import com.interface21.context.annotation.Configuration;
import com.interface21.context.stereotype.Controller;
import com.interface21.context.stereotype.Repository;
import com.interface21.context.stereotype.Service;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public class ClassPathBeanScanner implements BeanDefinitionScanner {
    private static final List<Class<? extends Annotation>> beanAnnotations = List.of(Controller.class, Service.class, Repository.class, Configuration.class);

    private final Object[] basePackage;

    public ClassPathBeanScanner(final Object... basePackage) {
        if (basePackage == null || basePackage.length == 0) {
            throw new BeanScanFailureException("basePackage can not be empty");
        }
        this.basePackage = basePackage;
    }

    @Override
    public BeanDefinitionRegistry scan() {
        final Reflections reflections = new Reflections(basePackage);
        final BeanDefinitionRegistry beanDefinitionRegistry = new SimpleBeanDefinitionRegistry();
        beanAnnotations.stream()
                .map(reflections::getTypesAnnotatedWith)
                .flatMap(Set::stream)
                .forEach(beanClass -> beanDefinitionRegistry.registerBeanDefinition(beanClass, SimpleBeanDefinition.from(beanClass)));
        return beanDefinitionRegistry;
    }
}
