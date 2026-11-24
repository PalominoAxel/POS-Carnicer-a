package com.dontoribio.pos.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.dontoribio.pos.model.Producto;

import java.math.BigDecimal;

/** Validacion defensiva de los datos de un producto con las precondiciones de Google Guava. */
public final class ProductoGuavaService {

    private ProductoGuavaService() {
    }

    public static void validar(String codigo, String nombre, BigDecimal precioMinorista,
                               BigDecimal precioMayorista, BigDecimal stock) {
        checkArgument(codigo != null && !codigo.isBlank(), "El código del producto es obligatorio");
        checkArgument(nombre != null && !nombre.isBlank(), "El nombre del producto es obligatorio");
        checkNotNull(precioMinorista, "El precio minorista es obligatorio");
        checkNotNull(precioMayorista, "El precio mayorista es obligatorio");
        checkNotNull(stock, "El stock es obligatorio");

        checkArgument(precioMinorista.signum() > 0, "El precio minorista debe ser mayor que cero");
        checkArgument(precioMayorista.signum() > 0, "El precio mayorista debe ser mayor que cero");
        checkArgument(stock.signum() >= 0, "Stock no puede ser negativo");
    }

    public static void validar(Producto producto) {
        checkNotNull(producto, "El producto es obligatorio");
        validar(producto.getCodigo(), producto.getNombre(), producto.getPrecioKgMinorista(),
                producto.getPrecioKgMayorista(), producto.getStockDisponible());
    }
}
