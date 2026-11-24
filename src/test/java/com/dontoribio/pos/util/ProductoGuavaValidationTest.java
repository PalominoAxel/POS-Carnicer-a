package com.dontoribio.pos.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dontoribio.pos.model.Producto;
import com.dontoribio.pos.service.ProductoGuavaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class ProductoGuavaValidationTest {

    private static final BigDecimal PRECIO_OK = new BigDecimal("25.00");
    private static final BigDecimal MAYORISTA_OK = new BigDecimal("22.50");
    private static final BigDecimal STOCK_OK = new BigDecimal("5.000");

    @Test
    @DisplayName("Guava: Debe rechazar productos con precio menor o igual a cero")
    void debeRechazarPrecioInvalido() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ProductoGuavaService.validar("RES-01", "Bife", BigDecimal.ZERO,
                        new BigDecimal("10.00"), STOCK_OK));
        assertTrue(ex.getMessage().contains("precio"));
    }

    @Test
    @DisplayName("Guava: Debe rechazar productos con stock negativo")
    void debeRechazarStockNegativo() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ProductoGuavaService.validar("RES-01", "Bife", PRECIO_OK,
                        new BigDecimal("10.00"), new BigDecimal("-1.000")));
        assertTrue(ex.getMessage().contains("Stock"));
    }

    @Test
    @DisplayName("Guava: Debe rechazar precio mayorista negativo")
    void debeRechazarPrecioMayoristaNegativo() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ProductoGuavaService.validar("RES-01", "Bife", PRECIO_OK,
                        new BigDecimal("-1.00"), STOCK_OK));
        assertTrue(ex.getMessage().contains("precio mayorista"));
    }

    @Test
    @DisplayName("Guava: Debe rechazar un código vacío")
    void debeRechazarCodigoVacio() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ProductoGuavaService.validar("  ", "Bife", PRECIO_OK, MAYORISTA_OK, STOCK_OK));
        assertTrue(ex.getMessage().contains("código"));
    }

    @Test
    @DisplayName("Guava: Debe rechazar un nombre nulo")
    void debeRechazarNombreNulo() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ProductoGuavaService.validar("RES-01", null, PRECIO_OK, MAYORISTA_OK, STOCK_OK));
        assertTrue(ex.getMessage().contains("nombre"));
    }

    @Test
    @DisplayName("Guava: Debe lanzar NullPointerException si falta el precio minorista")
    void debeRechazarPrecioNulo() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> ProductoGuavaService.validar("RES-01", "Bife", null, MAYORISTA_OK, STOCK_OK));
        assertTrue(ex.getMessage().contains("precio minorista"));
    }

    @Test
    @DisplayName("Guava: Debe aceptar un producto válido")
    void debeAceptarProductoValido() {
        assertDoesNotThrow(() -> ProductoGuavaService.validar(
                "RES-01", "Bife", PRECIO_OK, MAYORISTA_OK, STOCK_OK));
    }

    @Test
    @DisplayName("Guava: Debe aceptar los valores límite (precio 0.01 y stock 0)")
    void debeAceptarValoresLimite() {
        assertDoesNotThrow(() -> ProductoGuavaService.validar(
                "RES-01", "Bife", new BigDecimal("0.01"), new BigDecimal("0.01"), BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Guava: Debe validar un objeto Producto del modelo")
    void debeValidarObjetoProducto() {
        Producto valido = new Producto(1L, "CER-002", "Chuleta de Cerdo", new BigDecimal("24.90"),
                new BigDecimal("22.50"), "kg", new BigDecimal("34.000"), true);
        Producto invalido = new Producto(2L, "CER-003", "Panceta", new BigDecimal("-5.00"),
                new BigDecimal("22.50"), "kg", new BigDecimal("34.000"), true);

        assertDoesNotThrow(() -> ProductoGuavaService.validar(valido));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ProductoGuavaService.validar(invalido));
        assertTrue(ex.getMessage().contains("precio minorista"));
    }
}
