package org.ryuu;

import org.ryuu.functional.*;

import java.util.List;

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

    //region then
    @SuppressWarnings("UnusedReturnValue")
    public <R> Promise<R> then(Action1Arg<T> onFulfilled, Func1Arg<Object, Object> onRejected) {
        return then(
                result -> {
                    onFulfilled.invoke(result);
                    return null;
                },
                onRejected
        );
    }

    @SuppressWarnings("UnusedReturnValue")
    public <R> Promise<R> then(Func1Arg<T, R> onFulfilled, Action1Arg<Object> onRejected) {
        return then(
                onFulfilled,
                reason -> {
                    onRejected.invoke(reason);
                    return null;
                }
        );
    }

    @SuppressWarnings("UnusedReturnValue")
    public <R> Promise<R> then(Action1Arg<T> onFulfilled, Action1Arg<Object> onRejected) {
        return then(
                result -> {
                    onFulfilled.invoke(result);
                    return null;
                },
                reason -> {
                    onRejected.invoke(reason);
                    return null;
                }
        );
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
                    Object reason = onRejected.invoke(this.reason);
                    reject.invoke(reason);
                    break;
                case PENDING:
                    thenResolve.add((Func1Arg<T, Object>) onFulfilled);
                    thenReject.add(onRejected);
                    break;
            }
        });
    }
    //endregion

    public Promise<T> finallyResolve(Action action) {
        return then(
                value -> {
                    action.invoke();
                    return value;
                },
                reason -> {
                    action.invoke();
                    return reason;
                }
        );
    }

    public static Promise<Object[]> all(List<Promise<Object>> promises) {
        final int promiseCount = promises.size();
        int[] resultCount = {0};
        Object[] resultList = new Object[promiseCount];
        return new Promise<>((resolve, reject) -> {
            for (int i = 0; i < promiseCount; i++) {
                Promise<Object> promise = promises.get(i);
                int finalI = i;
                promise.then(
                        result -> {
                            resultList[finalI] = result;
                            resultCount[0]++;
                            if (resultCount[0] == promiseCount) {
                                resolve.invoke(resultList);
                            }
                        },
                        reject
                );
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
