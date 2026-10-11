package com.msm.core.objects.dataexchange.imports;

import com.msm.core.objects.config.ObjectImportRegistry;
import com.msm.core.objects.exception.ImportConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportConfigServiceTest {

    private static ImportConfigService service(Map<String, String> props) {
        ObjectImportRegistry registry = new Binder(new MapConfigurationPropertySource(props))
                .bindOrCreate("imports", ObjectImportRegistry.class);
        return new ImportConfigService(registry);
    }

    @Test
    void resolveOrderPutsDependenciesFirst() {
        ImportConfigService service = service(Map.of(
                "imports.objects.order.depends-on[0]", "product",
                "imports.objects.order.depends-on[1]", "customer",
                "imports.objects.product.depends-on[0]", "category",
                "imports.objects.category.identity.fields[0]", "code",
                "imports.objects.customer.identity.fields[0]", "code"
        ));

        assertThat(service.resolveOrder()).containsExactly("category", "customer", "product", "order");
    }

    @Test
    void resolveOrderRejectsCycle() {
        ImportConfigService service = service(Map.of(
                "imports.objects.a.depends-on[0]", "b",
                "imports.objects.b.depends-on[0]", "c",
                "imports.objects.c.depends-on[0]", "a"
        ));

        assertThatThrownBy(service::validate)
                .isInstanceOf(ImportConfigurationException.class)
                .hasMessage("Circular import dependency: a -> b -> c -> a");
    }

    @Test
    void resolveOrderRejectsUnknownDependency() {
        ImportConfigService service = service(Map.of("imports.objects.order.depends-on[0]", "product"));

        assertThatThrownBy(service::validate)
                .isInstanceOf(ImportConfigurationException.class)
                .hasMessageContaining("[order] depends on unknown object [product]");
    }

    @Test
    void sourceAndLegacyHeaderBind() {
        ImportConfigService service = service(Map.of(
                "imports.objects.product.source.sheet", "Products",
                "imports.objects.product.source.header.mode", "FIXED",
                "imports.objects.product.source.header.row", "2",
                "imports.objects.legacy.header.row", "3"
        ));

        assertThat(service.getSheet("product")).isEqualTo("Products");
        assertThat(service.getHeader("product").getRow()).isEqualTo(2);
        assertThat(service.getHeader("legacy").getRow()).isEqualTo(3);
        assertThat(service.getSheet("unknown")).isNull();
        assertThat(service.resolveOrder()).isEqualTo(List.of("legacy", "product"));
    }

    @Test
    void workbookObjectsAreRootThenItsDependents() {
        ImportConfigService service = service(Map.of(
                "imports.objects.v2order.source.sheet", "Order",
                "imports.objects.v2orderitem.source.sheet", "OrderItem",
                "imports.objects.v2orderitem.depends-on[0]", "v2order",
                "imports.objects.itemnote.source.sheet", "ItemNote",
                "imports.objects.itemnote.depends-on[0]", "v2orderitem",
                "imports.objects.product.identity.fields[0]", "code"
        ));

        assertThat(service.getWorkbookObjects("v2order")).containsExactly("v2order", "v2orderitem", "itemnote");
        assertThat(service.getWorkbookObjects("product")).containsExactly("product");
    }

    @Test
    void workbookDependentWithoutSheetIsRejected() {
        ImportConfigService service = service(Map.of(
                "imports.objects.v2order.source.sheet", "Order",
                "imports.objects.v2orderitem.depends-on[0]", "v2order"
        ));

        assertThatThrownBy(() -> service.getWorkbookObjects("v2order"))
                .isInstanceOf(ImportConfigurationException.class)
                .hasMessageContaining("[v2orderitem] depends on [v2order] but has no source.sheet");
    }
}
