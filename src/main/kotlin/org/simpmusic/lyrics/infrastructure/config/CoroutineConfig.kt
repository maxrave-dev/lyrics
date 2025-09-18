package org.simpmusic.lyrics.infrastructure.config

import kotlinx.coroutines.*
import org.springframework.beans.BeansException
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.Runnable

/**
 * Configuration class for coroutine scopes
 */
@Configuration
class CoroutineConfig {
    /**
     * Provides the IO dispatcher for IO-bound operations
     * This dispatcher is wrapped in a non-closeable wrapper to prevent Spring from trying to close it
     */
    @Bean
    fun ioDispatcher(): CoroutineDispatcher = NonCloseableCoroutineDispatcher(Dispatchers.IO)

    /**
     * Provides the Default dispatcher for CPU-bound operations
     * This dispatcher is wrapped in a non-closeable wrapper to prevent Spring from trying to close it
     */
    @Bean
    fun defaultDispatcher(): CoroutineDispatcher = NonCloseableCoroutineDispatcher(Dispatchers.Default)

    /**
     * Application scope for long-lived coroutines
     * Uses SupervisorJob to prevent child coroutine failures from affecting others
     */
    @Bean
    @Qualifier("applicationScope")
    fun applicationScope(
        @Qualifier("ioDispatcher") ioDispatcher: CoroutineDispatcher,
    ): CoroutineScope = CloseableCoroutineScope(SupervisorJob() + ioDispatcher)

    /**
     * Bean post processor to prevent Spring from trying to close Kotlin's built-in dispatchers
     */
    @Bean
    fun dispatcherBeanPostProcessor(): DestructionAwareBeanPostProcessor =
        object : DestructionAwareBeanPostProcessor {
            @Throws(BeansException::class)
            override fun requiresDestruction(bean: Any): Boolean = bean !is NonCloseableCoroutineDispatcher && bean is CoroutineDispatcher

            @Throws(BeansException::class)
            override fun postProcessBeforeDestruction(
                bean: Any,
                beanName: String,
            ) {
                // Do nothing for NonCloseableCoroutineDispatcher
            }
        }
}

/**
 * Wrapper class for CoroutineDispatcher that prevents Spring from calling close() on it
 */
class NonCloseableCoroutineDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    override fun dispatch(
        context: kotlin.coroutines.CoroutineContext,
        block: Runnable,
    ) {
        delegate.dispatch(context, block)
    }
}

/**
 * Wrapper class for CoroutineScope that properly implements DisposableBean
 * to ensure the scope is cancelled when the application is shut down
 */
class CloseableCoroutineScope(
    context: kotlin.coroutines.CoroutineContext,
) : CoroutineScope,
    DisposableBean {
    override val coroutineContext: kotlin.coroutines.CoroutineContext = context

    override fun destroy() {
        cancel()
    }
}
