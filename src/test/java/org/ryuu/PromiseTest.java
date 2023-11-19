package org.ryuu;

import org.junit.jupiter.api.Test;
import org.ryuu.functional.Action1Arg;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PromiseTest {
    @Test
    void executorException() {
        RuntimeException exception = new RuntimeException("executor exception");
        new Promise((resolve, reject) -> {
            throw exception;
        }).then(
                value -> {
                    fail();
                },
                reason -> {
                    assertEquals(exception, reason);
                }
        );
    }

    @Test
    void thenException() {
        new Promise(
                (resolve, reject) -> resolve.invoke(42)
        ).then(
                (Action1Arg<Object>) value -> {
                    throw new RuntimeException("then exception");
                },
                reason -> 42
        ).then(
                value -> {
                    fail();
                },
                reason -> {
                    System.out.println(reason);
                }
        );
    }

    @Test
    void resolveReject() {
        new Promise((resolve, reject) -> {
            resolve.invoke(42);
            reject.invoke(42);
        }).then(
                value -> {
                    assertEquals(42, value);
                },
                reason -> {
                    fail();
                }
        );
    }

    @Test
    void rejectResolve() {
        new Promise((resolve, reject) -> {
            reject.invoke(42);
            resolve.invoke(42);
        }).then(
                value -> {
                    fail();
                },
                reason -> {
                    assertEquals(42, reason);
                }
        );
    }

    @Test
    void resolveThen() {
        new Promise(
                (resolve, reject) -> resolve.invoke(42)
        ).then(
                value -> {
                    assertEquals(42, value);
                },
                reason -> {
                }
        );
    }

    @Test
    void rejectThen() {
        new Promise(
                (resolve, reject) -> reject.invoke(42)
        ).then(
                value -> 42,
                reason -> {
                    assertEquals(42, reason);
                }
        );
    }

    @Test
    void chainCallThen() {
        new Promise(
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
                },
                reason -> {
                }
        );
    }

    @Test
    void parallelCallThen() {
        Promise promise = new Promise(
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
                reason -> {
                }
        );
        promise.then(
                value -> true,
                reason -> {
                }
        ).then(
                value -> {
                    assertTrue(true);
                },
                reason -> {
                }
        );
    }

    @Test
    void promiseInPromise() {
        Promise promiseInPromise = new Promise(
                (resolve, reject) -> resolve.invoke("foo")
        );
        new Promise(
                (resolve, reject) -> resolve.invoke(promiseInPromise)
        ).then(
                value -> {
                    assertEquals("foo", value);
                },
                reason -> {
                }
        );
    }

    @Test
    void promiseInThen() {
        Promise promise = new Promise(
                (resolve, reject) -> resolve.invoke("foo")
        );
        new Promise(
                (resolve, reject) -> resolve.invoke("bar")
        ).then(
                value -> promise,
                reason -> {
                }
        ).then(
                value -> {
                    assertEquals("foo", value);
                },
                reason -> {
                }
        );
    }

    @Test
    void thenWithDifferentTypes() {
        int inputValue = 42;
        new Promise(
                (resolve, reject) -> resolve.invoke(inputValue)
        ).then(
                value -> {
                    assertEquals(42, value);
                    return true;
                },
                reason -> {
                }
        ).then(
                value -> {
                    assertTrue((Boolean) value);
                },
                reason -> {
                }
        );
    }

    @Test
    void all() {
        Object[] expectedResults = new Object[]{"foo", 42, "bar"};
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        Promise.all(Arrays.asList(
                new Promise((resolve, reject) -> resolve.invoke("foo")),
                new Promise((resolve, reject) -> executor.schedule(
                        () -> resolve.invoke(42), 1, TimeUnit.SECONDS
                )),
                new Promise((resolve, reject) -> resolve.invoke("bar"))
        )).then(
                result -> {
                    assertArrayEquals(expectedResults, (Object[]) result);
                },
                reason -> {
                    System.out.println("reason");
                }
        );

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2_000);
                return "Async Result";
            } catch (InterruptedException e) {
                return "Async Error";
            }
        });
        future.join();
    }

    @Test
    void finallyResolve() {
        int expectedResult = 2;
        int[] result = {0};
        Promise promise = new Promise(
                (resolve, reject) -> resolve.invoke(42)
        );
        Promise finallyResolvePromise = promise.finallyResolve(
                () -> result[0]++
        );
        finallyResolvePromise.then(
                value -> {
                    System.out.println(value);
                    result[0]++;
                    assertEquals(42, value);
                },
                reason -> {
                }
        );
        assertEquals(expectedResult, result[0]);
    }

    @Test
    void finallyResolveException() {
        int expectedResult = 2;
        int[] result = {0};
        Object originalReason = new RuntimeException("finallyResolveException");
        new Promise(
                (resolve, reject) -> reject.invoke(originalReason)
        ).finallyResolve(
                () -> result[0]++
        ).then(
                value -> {
                },
                reason -> {
                    assertEquals(originalReason, reason);
                    result[0]++;
                }
        );
        assertEquals(expectedResult, result[0]);
    }

    @Test
    void resolve() {
        int exceptedValue = 42;
        Promise
                .resolve(exceptedValue)
                .then(
                        value -> {
                            assertEquals(exceptedValue, value);
                        },
                        reason -> {
                            fail();
                        }
                );
    }
}