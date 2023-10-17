package org.ryuu;

import org.ryuu.functional.*;

import static org.ryuu.Promise.Status.*;

public class Promise<T> {
    private Status status = PENDING;
    private T result;
    private Object reason;
    private final Funcs1Arg<T, Object> thenResolve = new Funcs1Arg<>();
    private final Funcs1Arg<Object, Object> thenReject = new Funcs1Arg<>();

    public Promise(Action2Args<Action1Arg<T>, Action1Arg<Object>> executor) {
        try {
            executor.invoke(this::resolve, this::reject);
        } catch (Exception e) {
            reject(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> Promise<R> then(Func1Arg<T, R> onFulfilled, Func1Arg<Object, Object> onRejected) {
        return new Promise<>((resolve, reject) -> {
            switch (status) {
                case FULFILLED:
                    R result = onFulfilled.invoke(this.result);
                    resolve.invoke(result);
                    break;
                case REJECTED:
                    onRejected.invoke(reason);
                    break;
                case PENDING:
                    thenResolve.add((Func1Arg<T, Object>) onFulfilled);
                    thenReject.add(onRejected);
                    break;
            }
        });
    }

    private void resolve(T result) {
        if (status != PENDING) {
            return;
        }

        status = FULFILLED;
        this.result = result;
        thenResolve.invoke(result);
    }

    private void reject(Object reason) {
        if (status != PENDING) {
            return;
        }

        status = REJECTED;
        this.reason = reason;
        thenReject.invoke(reason);
    }

    enum Status {
        PENDING,
        FULFILLED,
        REJECTED
    }
}
