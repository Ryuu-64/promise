package org.ryuu;

import org.ryuu.functional.Action1Arg;
import org.ryuu.functional.Action2Args;
import org.ryuu.functional.Func1Arg;
import org.ryuu.functional.Funcs1Arg;

import java.util.ArrayList;
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

    public static Promise<List<Object>> all(List<Promise<Object>> promises) {
        final int promiseCount = promises.size();
        int[] resultCount = {0};
        List<Object> resultList = new ArrayList<>();
        return new Promise<>((resolve, reject) -> {
            for (int i = 0; i < promiseCount; i++) {
                Promise<Object> promise = promises.get(i);
                int finalI = i;
                promise.then(
                        result -> {
                            resultList.set(finalI, result);
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

    //region then
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
                    onRejected.invoke(reason);
                    break;
                case PENDING:
                    thenResolve.add((Func1Arg<T, Object>) onFulfilled);
                    thenReject.add(onRejected);
                    break;
            }
        });
    }
    //endregion

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
