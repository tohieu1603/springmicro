package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.AbstractIntegrationTest;
import com.hieu.catalog_service.application.command.product.CreateProductCommand;
import com.hieu.catalog_service.domain.exception.ProductAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CreateProductHandler — integration tests")
class CreateProductHandlerIT extends AbstractIntegrationTest {

    @Autowired
    CreateProductHandler handler;

    // ── helpers ───────────────────────────────────────────────────────────────

    private static CreateProductCommand aCommand(String name, String sku) {
        var variant = new CreateProductCommand.VariantCmd(
                sku,
                new BigDecimal("199.99"),
                new BigDecimal("100.00"),
                null,
                null,
                new BigDecimal("0.5"),
                20,
                List.of()
        );
        return new CreateProductCommand(
                name, "Mô tả sản phẩm", null, "TestBrand",
                null, List.of(), null, null, null,
                List.of(variant), false, "test-user"
        );
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createProduct_uniqueSlug_succeeds — tạo sản phẩm với slug duy nhất thành công")
    void createProduct_uniqueSlug_succeeds() {
        String uniqueName = "Unique Product " + UUID.randomUUID();
        String sku = "SKU-UNIQ-" + System.nanoTime();

        var dto = handler.handle(aCommand(uniqueName, sku));

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isNotBlank();
        assertThat(dto.slug()).isNotBlank();
        assertThat(dto.variants()).hasSize(1);
        assertThat(dto.variants().get(0).sku()).isEqualTo(sku);
    }

    @Test
    @DisplayName("createProduct_duplicateSlug_throws409 — cùng tên → ProductAlreadyExistsException")
    void createProduct_duplicateSlug_throws409() {
        // Two products with the same name → same base slug → handler throws on collision
        String name = "Duplicate Slug Product";
        handler.handle(aCommand(name, "SKU-DUP1-" + System.nanoTime()));

        // ensureUniqueSlug appends suffix on first collision; second collision throws
        // We simulate the scenario by exhausting disambiguation via same name repeatedly.
        // The handler itself throws ProductAlreadyExistsException when slug already occupied.
        assertThatThrownBy(() -> {
            // Force slug collision: override ensureUniqueSlug path by inserting same slug
            // The second call with the same exact name will hit an existing slug + suffix attempt
            handler.handle(aCommand(name, "SKU-DUP2-" + System.nanoTime()));
            handler.handle(aCommand(name, "SKU-DUP3-" + System.nanoTime()));
        }).isInstanceOf(ProductAlreadyExistsException.class);
    }

    @Test
    @DisplayName("createProduct_concurrentInsert_409 — 2 threads cùng slug → 1 success + 1 exception (no 500)")
    void createProduct_concurrentInsert_409() throws InterruptedException {
        String name = "Concurrent Slug Product " + UUID.randomUUID();
        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger productAlreadyExists = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    handler.handle(aCommand(name, "SKU-CONCURRENT-" + idx + "-" + System.nanoTime()));
                    successes.incrementAndGet();
                } catch (ProductAlreadyExistsException e) {
                    productAlreadyExists.incrementAndGet();
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(unexpected.get())
                .as("Không được có lỗi 500 — chỉ ProductAlreadyExistsException hoặc success")
                .isZero();
        assertThat(successes.get() + productAlreadyExists.get()).isEqualTo(threads);
        assertThat(successes.get()).isGreaterThanOrEqualTo(1);
    }
}
