package org.ryuu;

import org.ryuu.functional.*;

import java.util.List;

import static org.ryuu.Promise.Status.*;

public class Promise {
    private Status status = PENDING;
    private Object result;
    private Object reason;
    private final Funcs1Arg<Object, Object> thenResolve = new Funcs1Arg<>();
    private final Funcs1Arg<Object, Object> thenReject = new Funcs1Arg<>();

    public Promise(Action2Args<Action1Arg<Object>, Action1Arg<Object>> executor) {
        try {
            executor.invoke(this::doResolve, this::doReject);
        } catch (Exception e) {
            doReject(e);
        }
    }

    //region then
    @SuppressWarnings("UnusedReturnValue")
    public Promise then(Action1Arg<Object> onFulfilled, Func1Arg<Object, Object> onRejected) {
        return then(
                result -> {
                    onFulfilled.invoke(result);
                    return null;
                },
                onRejected
        );
    }

    @SuppressWarnings("UnusedReturnValue")
    public Promise then(Func1Arg<Object, Object> onFulfilled, Action1Arg<Object> onRejected) {
        return then(
                onFulfilled,
                reason -> {
                    onRejected.invoke(reason);
                    return null;
                }
        );
    }

    @SuppressWarnings("UnusedReturnValue")
    public Promise then(Action1Arg<Object> onFulfilled, Action1Arg<Object> onRejected) {
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

    public Promise then(Func1Arg<Object, Object> onFulfilled, Func1Arg<Object, Object> onRejected) {
        return new Promise((resolve, reject) -> {
            switch (status) {
                case FULFILLED:
                    Object result = onFulfilled.invoke(this.result);
                    if (result instanceof Promise) {
                        ((Promise) result).then(resolve, reject);
                    } else {
                        resolve.invoke(result);
                    }
                    break;
                case REJECTED:
                    Object reason = onRejected.invoke(this.reason);
                    reject.invoke(reason);
                    break;
                case PENDING:
                    thenResolve.add(onFulfilled);
                    thenReject.add(onRejected);
                    break;
            }
        });
    }
    //endregion

    public Promise finallyResolve(Action action) {
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

    public static Promise all(List<Promise> promises) {
        final int promiseCount = promises.size();
        int[] resultCount = {0};
        Object[] resultList = new Object[promiseCount];
        return new Promise((resolve, reject) -> {
            for (int i = 0; i < promiseCount; i++) {
                Promise promise = promises.get(i);
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

    public static <T> Promise resolve(T result) {
        return new Promise((resolve, reject) -> resolve.invoke(result));
    }

    private void doResolve(Object result) {
        if (status != PENDING) {
            return;
        }

        status = FULFILLED;
        if (result instanceof Promise) {
            ((Promise) result).then(
                    resultsResult -> {
                        this.result = resultsResult;
                        thenResolve.invoke(result);
                    },
                    this::doReject
            );
        } else {
            this.result = result;
            thenResolve.invoke(result);
        }
    }

    private void doReject(Object reason) {
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
