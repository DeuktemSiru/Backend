package com.deuktemsiru.config

import com.deuktemsiru.repository.MemberRepository
import com.deuktemsiru.repository.OrderRepository
import com.deuktemsiru.repository.StoreRepository
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig {

    @Bean
    fun businessMetrics(
        orderRepository: OrderRepository,
        storeRepository: StoreRepository,
        memberRepository: MemberRepository,
    ): MeterBinder = MeterBinder { registry ->
        registerGauge(registry, "deuktemsiru.orders.total", "Total orders") {
            orderRepository.count().toDouble()
        }
        registerGauge(registry, "deuktemsiru.stores.total", "Total stores") {
            storeRepository.count().toDouble()
        }
        registerGauge(registry, "deuktemsiru.members.total", "Total members") {
            memberRepository.count().toDouble()
        }
    }

    private fun registerGauge(
        registry: MeterRegistry,
        name: String,
        description: String,
        supplier: () -> Double,
    ) {
        io.micrometer.core.instrument.Gauge.builder(name, supplier)
            .description(description)
            .register(registry)
    }
}
