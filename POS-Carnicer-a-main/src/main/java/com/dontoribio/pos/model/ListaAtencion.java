package com.dontoribio.pos.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuenta de atencion en curso. El POS permite hasta 7 listas paralelas
 * en memoria (una por cliente/mostrador atendido simultaneamente) antes
 * de consolidarlas en una {@link Venta}.
 */
public class ListaAtencion {

    public static final int MAX_LISTAS = 7;

    private final int numero; // 1..7
    private Cliente cliente;
    private final List<DetalleVenta> items = new ArrayList<>();
    private EstadoLista estado = EstadoLista.ABIERTA;

    public ListaAtencion(int numero) {
        if (numero < 1 || numero > MAX_LISTAS) {
            throw new IllegalArgumentException("Numero de lista fuera de rango (1-" + MAX_LISTAS + ")");
        }
        this.numero = numero;
    }

    public void agregarItem(DetalleVenta item) {
        items.add(item);
    }

    public void quitarItem(DetalleVenta item) {
        items.remove(item);
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(DetalleVenta::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean estaVacia() {
        return items.isEmpty();
    }

    public void limpiar() {
        items.clear();
        cliente = null;
        estado = EstadoLista.ABIERTA;
    }

    public int getNumero() {
        return numero;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public List<DetalleVenta> getItems() {
        return items;
    }

    public EstadoLista getEstado() {
        return estado;
    }

    public void setEstado(EstadoLista estado) {
        this.estado = estado;
    }
}
