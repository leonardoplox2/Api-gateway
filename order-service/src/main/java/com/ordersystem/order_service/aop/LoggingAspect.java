package com.ordersystem.order_service.aop;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import reactor.core.publisher.Mono;

@Aspect
@Component 

public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.ordersystem.order_service.service..*(..))")
    public Object LogServiceCall(ProceedingJoinPoint joinpoint) throws Throwable{
        String methodName = joinpoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        Object result = joinpoint.proceed();

        if (result instanceof Mono<?> mono){
            return mono
                    .doOnSuccess(valor -> log.info("{} completado en {} ms", methodName, System.currentTimeMillis() - start))
                    .doOnError(ex -> log.error("{} fallo en {} ms : {}", methodName, System.currentTimeMillis() - start, ex.getMessage()));
        }
        log.info("{} ejecutado en {} ms", methodName, System.currentTimeMillis() - start);
        return result;

    }

}
