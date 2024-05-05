package sk.tuke.meta.persistence.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import sk.tuke.meta.persistence.annotations.AtomicPersistenceOperation;

@Aspect
public class TransactionAspect {

    @Pointcut("@annotation(atomicOperation)")
    public void callAt(AtomicPersistenceOperation atomicOperation) {}

    @Around("callAt(atomicOperation)")
    public Object manageTransaction(ProceedingJoinPoint joinPoint, AtomicPersistenceOperation atomicOperation) throws Throwable {
        return null;
    }

}
