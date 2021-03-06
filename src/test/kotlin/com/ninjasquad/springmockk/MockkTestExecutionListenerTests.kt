package com.ninjasquad.springmockk

import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestContext
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import java.io.InputStream


/**
 * Tests for [MockkTestExecutionListener].
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class MockkTestExecutionListenerTests {

    private val listener = MockkTestExecutionListener()

    @MockK
    private lateinit var applicationContext: ApplicationContext

    @MockK(relaxUnitFun = true)
    private lateinit var postProcessor: MockkPostProcessor

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { applicationContext.getBean<MockkPostProcessor>() } returns this.postProcessor
    }

    @Test
    fun prepareTestInstanceShouldInitMockkAnnotations() {
        val instance = WithMockkAnnotations()
        this.listener.prepareTestInstance(mockTestContext(instance))
        assertThat(instance.mock).isNotNull()
    }

    @Test
    fun prepareTestInstanceShouldInjectMockBean() {
        val instance = WithMockkBean()
        this.listener.prepareTestInstance(mockTestContext(instance))
        verify { postProcessor.inject(
            withArg { assertThat(it.name).isEqualTo("mockBean") },
            instance,
            any<MockkDefinition>()
        )}
    }

    @Test
    fun beforeTestMethodShouldDoNothingWhenDirtiesContextAttributeIsNotSet() {
        val instance = WithMockkBean()
        val mockTestContext = mockTestContext(instance)
        every {
            mockTestContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE)
        } returns null
        this.listener.beforeTestMethod(mockTestContext(instance))
        confirmVerified(postProcessor)
    }

    @Test
    fun beforeTestMethodShouldInjectMockBeanWhenDirtiesContextAttributeIsSet() {
        val instance = WithMockkBean()
        val mockTestContext = mockTestContext(instance)
        every {
            mockTestContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE)
        } returns true
        this.listener.beforeTestMethod(mockTestContext)
        verify { postProcessor.inject(
            withArg { assertThat(it.name).isEqualTo("mockBean") },
            instance,
            any<MockkDefinition>()
        )}
    }

    private fun mockTestContext(instance: Any): TestContext {
        val testContext = mockk<TestContext>(relaxed = true)
        every { testContext.getTestInstance() } returns instance
        every { testContext.getTestClass() } returns instance.javaClass as Class<*>
        every { testContext.getApplicationContext() } returns this.applicationContext
        return testContext
    }

    internal class WithMockkAnnotations {

        @MockK
        lateinit var mock: InputStream

    }

    internal class WithMockkBean {

        @MockkBean
        lateinit var mockBean: InputStream

    }

}
