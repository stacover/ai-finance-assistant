package com.jh.financeResearchAgent.config;

import com.jh.financeResearchAgent.tool.market.MarketTools;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jinhang
 * @since 2026/9/2 22:30
 */
@Configuration
@Slf4j
public class AgentToolConfiguration {
    @Bean
    public ToolCallback[] agentToolCallbacks(MarketTools marketTools) {
        return ToolCallbacks.from(marketTools);
    }

    /**
     * 工具执行专用线程池：用于 ToolExecutionRuntime 的超时与中断控制。
     *
     * <p>使用独立线程池，避免与 Spring 默认的 applicationTaskExecutor 相互影响。
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService toolExecutionExecutor() {
        int corePoolSize = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        return new ThreadPoolExecutor(
                corePoolSize,
                corePoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                toolThreadFactory());
    }

    private ThreadFactory toolThreadFactory() {
        AtomicInteger counter = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, "tool-exec-" + counter.getAndIncrement());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler(
                    (t, e) -> log.error("Uncaught exception in tool executor thread: {}", t.getName(), e));
            return thread;
        };
    }
}
