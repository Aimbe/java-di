package com.interface21.beans.factory.support;

import com.interface21.beans.factory.annotation.Autowired;
import com.interface21.beans.factory.config.ConfigurationBeanDefinition;
import com.interface21.beans.factory.config.SingletonBeanDefinition;
import com.interface21.context.annotation.Bean;
import com.interface21.context.annotation.Configuration;
import com.interface21.context.stereotype.Component;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import samples.*;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultListableBeanFactoryTest {

    @Test
    void 기본생성자만_있는_클래스의_빈을_생성한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of("SimpleMemoryRepository", new SingletonBeanDefinition(SimpleMemoryRepository.class)));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        assertAll(
                () -> assertThat(beanFactory.getSingletonObjects()).containsKey(SimpleMemoryRepository.class),
                () -> assertThat(beanFactory.getSingletonObjects().get(SimpleMemoryRepository.class)).isInstanceOf(SimpleMemoryRepository.class)
        );
    }

    @Test
    void 이미_빈으로_생성된_경우_재생성하지_않는다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of("SimpleMemoryRepository", new SingletonBeanDefinition(SimpleMemoryRepository.class)));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();
        Object expected = beanFactory.getSingletonObjects().get(SimpleMemoryRepository.class);
        beanFactory.initialize();
        Object actual = beanFactory.getSingletonObjects().get(SimpleMemoryRepository.class);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void 생성자에_파라미터가_빈이_아닌_경우_빈이_생성된_후_다시_생성한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of(
                "SimpleMemoryRepository", new SingletonBeanDefinition(SimpleMemoryRepository.class),
                "SampleService", new SingletonBeanDefinition(SampleService.class)
        ));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        SampleService actual = (SampleService) beanFactory.getSingletonObjects().get(SampleService.class);
        assertThat(actual.getSampleRepository()).isEqualTo(beanFactory.getSingletonObjects().get(SimpleMemoryRepository.class));
    }

    @Test
    void configuration의_경우_하위_bean메소드도_빈으로_생성한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry();
        registry.registerBeanDefinition(ExampleConfig.class, ConfigurationBeanDefinition.from(ExampleConfig.class));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        DataSource actual = (DataSource) beanFactory.getSingletonObjects().get(DataSource.class);
        assertThat(actual).isInstanceOf(JdbcDataSource.class);
    }

    @Test
    void beanClass들을_반환한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of(
                "SimpleMemoryRepository", new SingletonBeanDefinition(SimpleMemoryRepository.class),
                "SampleService", new SingletonBeanDefinition(SampleService.class)
        ));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        Set<Class<?>> actual = beanFactory.getBeanClasses();
        assertThat(actual).contains(SimpleMemoryRepository.class, SampleService.class);
    }

    @Test
    void 없는_bean을_가져가려하면_예외가_발생한다() {
        assertThatThrownBy(() -> new DefaultListableBeanFactory().getBean(SampleService.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 빈입니다.");
    }

    @Test
    void 생성된_빈을_반환한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of("SimpleMemoryRepository", new SingletonBeanDefinition(SimpleMemoryRepository.class)));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        SimpleMemoryRepository actual = beanFactory.getBean(SimpleMemoryRepository.class);
        assertThat(beanFactory.getSingletonObjects()).containsValue(actual);
    }

    @Test
    void 인터페이스로_빈을_반환한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of("SimpleMemoryRepository", new SingletonBeanDefinition(SimpleMemoryRepository.class)));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        SampleRepository actual = beanFactory.getBean(SampleRepository.class);
        assertThat(beanFactory.getSingletonObjects()).containsValue(actual);
    }

    @Test
    void 컨트롤러_어노테이션이_있는_빈만_반환한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of(
                "SimpleMemoryRepository", new SingletonBeanDefinition(SimpleMemoryRepository.class),
                "SampleService", new SingletonBeanDefinition(SampleService.class),
                "SampleController", new SingletonBeanDefinition(SampleController.class)
        ));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();

        Map<Class<?>, Object> actual = beanFactory.getControllers();
        assertThat(actual).containsOnlyKeys(SampleController.class);
    }

    @Test
    void 순환참조인_빈을_초기화하려하면_예외가_발생한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry(Map.of(
                "aClass", new SingletonBeanDefinition(A.class),
                "bClass", new SingletonBeanDefinition(B.class),
                "cClass", new SingletonBeanDefinition(C.class)
        ));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        assertThatThrownBy(beanFactory::initialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("순환참조인 빈이 있어 초기화할 수 없습니다.");
    }

    @Test
    void Configuration의_빈에_의해_순환참조인_빈을_초기화하려하면_예외가_발생한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry();
        registry.registerBeanDefinition(TestConfiguration.class, ConfigurationBeanDefinition.from(TestConfiguration.class));
        registry.registerBeanDefinition(CircularComponent.class, new SingletonBeanDefinition(CircularComponent.class));

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        assertThatThrownBy(beanFactory::initialize)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("순환참조인 빈이 있어 초기화할 수 없습니다.");
    }

    @Test
    void configuration을_통해_빈을_초기화한다() {
        DefaultBeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry();
        registry.registerBeanDefinition(TestImplementConfiguration.class, ConfigurationBeanDefinition.from(TestImplementConfiguration.class));

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();
    }

    @Test
    public void di() {
        BeanDefinitionRegistry registry = new DefaultBeanDefinitionRegistry();
        registry.registerBeanDefinition(SampleController.class, new SingletonBeanDefinition(SampleController.class));
        registry.registerBeanDefinition(SampleService.class, new SingletonBeanDefinition(SampleService.class));
        registry.registerBeanDefinition(JdbcSampleRepository.class, new SingletonBeanDefinition(JdbcSampleRepository.class));
        registry.registerBeanDefinition(IntegrationConfig.class, ConfigurationBeanDefinition.from(IntegrationConfig.class));

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(registry);
        beanFactory.initialize();
        final var sampleController = beanFactory.getBean(SampleController.class);

        assertNotNull(sampleController);
        assertNotNull(sampleController.getSampleService());

        final var sampleService = sampleController.getSampleService();
        assertNotNull(sampleService.getSampleRepository());
    }

    @Component
    public static class A {
        private final B b;

        @Autowired
        public A(final B b) {
            this.b = b;
        }
    }

    @Component
    public static class B {
        private final C c;

        @Autowired
        public B(C c) {
            this.c = c;
        }
    }

    @Component
    public static class C {
        private final A a;

        public C(A a) {
            this.a = a;
        }
    }

    @Configuration
    public static class TestConfiguration {

        private final CircularComponent circularComponent;

        public TestConfiguration(CircularComponent circularComponent) {
            this.circularComponent = circularComponent;
        }

        @Bean
        public NormalComponent testBean() {
            return new NormalComponent();
        }
    }

    @Component
    public static class CircularComponent {
        private final NormalComponent normalComponent;

        public CircularComponent(NormalComponent normalComponent) {
            this.normalComponent = normalComponent;
        }
    }

    public static class NormalComponent{

    }

    @Configuration
    public static class TestImplementConfiguration {

        public TestImplementConfiguration() {
        }

        @Bean
        public NormalComponent testBean() {
            return new NormalComponent();
        }

        @Bean
        public AbstractComponent firstBean() {
            return new FirstComponent();
        }

        @Bean
        public AbstractComponent secondBean() {
            return new SecondComponent();
        }
    }
    public interface AbstractComponent {}
    public static class FirstComponent implements AbstractComponent {}
    public static class SecondComponent implements AbstractComponent {}
}
