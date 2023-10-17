package org.ryuu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromiseTest {
    @Test
    void executorException() {
        new Promise<>((resolve, reject) -> {
            throw new RuntimeException("executor exception");
        }).then(
                value -> {
                    fail();
                    return 42;
                },
                reason -> {
                    ((Throwable) reason).printStackTrace();
                    return 42;
                }
        );
    }

    @Test
    void thenException() {
        new Promise<>(
                (resolve, reject) -> resolve.invoke(42)
        ).then(
                value -> {
                    throw new RuntimeException("then exception");
                },
                reason -> 42
        ).then(
                value -> {
                    fail();
                    return 42;
                },
                reason -> {
                    ((Throwable) reason).printStackTrace();
                    return 42;
                }
        );
    }

    @Test
    void resolveReject() {
        new Promise<>((resolve, reject) -> {
            resolve.invoke(42);
            reject.invoke(42);
        }).then(
                value -> {
                    assertEquals(42, value);
                    return 42;
                },
                reason -> {
                    fail();
                    return 42;
                }
        );
    }

    @Test
    void rejectResolve() {
        new Promise<>((resolve, reject) -> {
            reject.invoke(42);
            resolve.invoke(42);
        }).then(
                value -> {
                    fail();
                    return 42;
                },
                reason -> {
                    assertEquals(42, reason);
                    return 42;
                }
        );
    }

    @Test
    void resolveThen() {
        new Promise<>(
                (resolve, reject) -> resolve.invoke(42)
        ).then(
                value -> {
                    assertEquals(42, value);
                    return 1;
                },
                reason -> 42
        );
    }

    @Test
    void rejectThen() {
        new Promise<>(
                (resolve, reject) -> reject.invoke(42)
        ).then(
                value -> 42,
                reason -> {
                    assertEquals(42, reason);
                    return 1;
                }
        );
    }

    @Test
    void chainCallThen() {
        new Promise<>(
                (resolve, reject) -> resolve.invoke(42)
        ).then(
                value -> {
                    assertEquals(42, value);
                    return 1;
                },
                reason -> 42
        ).then(
                value -> {
                    assertEquals(1, value);
                    return 1;
                },
                reason -> 42
        );
    }

    @Test
    void parallelCallThen() {
        Promise<Integer> promise = new Promise<>(
                (resolve, reject) -> resolve.invoke(42)
        );
        promise.then(
                value -> "foo",
                reason -> 42
        ).then(
                value -> {
                    assertEquals("foo", value);
                    return 42;
                },
                reason -> 42
        );
        promise.then(
                value -> true,
                reason -> 42
        ).then(
                value -> {
                    assertTrue(true);
                    return 42;
                },
                reason -> 42
        );
    }

    @Test
    void thenWithDifferentTypes() {
        new Promise<>(
                (resolve, reject) -> resolve.invoke(42)
        ).then(
                value -> {
                    assertEquals(42, value);
                    return true;
                },
                reason -> 42
        ).then(
                value -> {
                    assertTrue(value);
                    return 1;
                },
                reason -> 42
        );
    }
}