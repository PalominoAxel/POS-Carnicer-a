package com.dontoribio.pos.controller;

import com.dontoribio.pos.dao.CompraDAO;
import com.dontoribio.pos.dao.ProductoDAO;
import com.dontoribio.pos.dao.ProveedorDAO;
import com.dontoribio.pos.dao.impl.CompraDAOImpl;
import com.dontoribio.pos.dao.impl.ProductoDAOImpl;
import com.dontoribio.pos.dao.impl.ProveedorDAOImpl;
import com.dontoribio.pos.model.*;
import com.dontoribio.pos.service.TicketGeneratorService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Controlador de la ventana de Compras: registra ingresos de mercaderia e incrementa el stock. */
public class ComprasController {

    /**
     * Categorias disponibles al crear un producto nuevo desde Compras, con su prefijo de
     * codigo (ver {@link com.dontoribio.pos.dao.ProductoDAO#generarSiguienteCodigo}). El
     * codigo ya no se escribe a mano: se calcula como el siguiente correlativo de la
     * categoria elegida (ej. "RES-011" si el ultimo "RES-xxx" registrado es "RES-010").
     */
    private static final Map<String, String> CATEGORIAS_PRODUCTO = new LinkedHashMap<>();

    static {
        CATEGORIAS_PRODUCTO.put("Res", "RES");
        CATEGORIAS_PRODUCTO.put("Cerdo", "CER");
        CATEGORIAS_PRODUCTO.put("Pollo", "POL");
        CATEGORIAS_PRODUCTO.put("Menudencia", "MEN");
        CATEGORIAS_PRODUCTO.put("Embutidos", "EMB");
    }

    @FXML
    private ComboBox<Proveedor> comboProveedor;
    @FXML
    private ComboBox<Producto> comboProducto;
    @FXML
    private TextField campoPeso;
    @FXML
    private TextField campoCosto;
    @FXML
    private TextField campoLote;
    @FXML
    private DatePicker campoFechaProduccion;
    @FXML
    private DatePicker campoFechaVencimiento;
    @FXML
    private TableView<DetalleCompra> tablaDetalle;
    @FXML
    private TableColumn<DetalleCompra, String> colProducto;
    @FXML
    private TableColumn<DetalleCompra, BigDecimal> colPeso;
    @FXML
    private TableColumn<DetalleCompra, BigDecimal> colCosto;
    @FXML
    private TableColumn<DetalleCompra, BigDecimal> colImporte;
    @FXML
    private TableColumn<DetalleCompra, String> colLote;
    @FXML
    private TableColumn<DetalleCompra, java.time.LocalDate> colFechaProduccion;
    @FXML
    private TableColumn<DetalleCompra, java.time.LocalDate> colFechaVencimiento;
    @FXML
    private Label labelTotal;

    private final ProveedorDAO proveedorDAO = new ProveedorDAOImpl();
    private final ProductoDAO productoDAO = new ProductoDAOImpl();
    private final CompraDAO compraDAO = new CompraDAOImpl();

    private final List<DetalleCompra> detalles = new ArrayList<>();
    private Usuario usuarioSesion;

    public void setUsuarioSesion(Usuario usuario) {
        this.usuarioSesion = usuario;
    }

    @FXML
    private void initialize() {
        comboProveedor.setItems(FXCollections.observableArrayList(proveedorDAO.listarTodos()));
        comboProducto.setItems(FXCollections.observableArrayList(productoDAO.listarActivos()));

        colProducto.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getProducto().getNombre()));
        colPeso.setCellValueFactory(new PropertyValueFactory<>("pesoKg"));
        colCosto.setCellValueFactory(new PropertyValueFactory<>("costoUnitario"));
        colImporte.setCellValueFactory(new PropertyValueFactory<>("importe"));
        colLote.setCellValueFactory(new PropertyValueFactory<>("lote"));
        colFechaProduccion.setCellValueFactory(new PropertyValueFactory<>("fechaProduccion"));
        colFechaVencimiento.setCellValueFactory(new PropertyValueFactory<>("fechaVencimiento"));

        // La fecha de vencimiento nunca puede ser igual o anterior a la fecha de produccion.
        campoFechaProduccion.valueProperty().addListener((obs, anterior, fechaProduccion) -> {
            if (fechaProduccion != null && campoFechaVencimiento.getValue() != null
                    && !campoFechaVencimiento.getValue().isAfter(fechaProduccion)) {
                campoFechaVencimiento.setValue(null);
            }
            campoFechaVencimiento.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate fecha, boolean vacio) {
                    super.updateItem(fecha, vacio);
                    if (!vacio && fechaProduccion != null && !fecha.isAfter(fechaProduccion)) {
                        setDisable(true);
                    }
                }
            });
        });
    }

    @FXML
    private void onNuevoProveedor() {
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.setTitle("Nuevo proveedor");
        dialogo.setHeaderText(null);

        ButtonType botonGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(botonGuardar, ButtonType.CANCEL);

        TextField campoNombreNuevo = new TextField();
        TextField campoRucNuevo = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.addRow(0, new Label("Nombre / Razon social:"), campoNombreNuevo);
        grid.addRow(1, new Label("RUC (opcional):"), campoRucNuevo);
        dialogo.getDialogPane().setContent(grid);

        Proveedor[] proveedorCreado = new Proveedor[1];
        Button botonGuardarNodo = (Button) dialogo.getDialogPane().lookupButton(botonGuardar);
        botonGuardarNodo.addEventFilter(ActionEvent.ACTION, evento -> {
            String nombre = campoNombreNuevo.getText() == null ? "" : campoNombreNuevo.getText().trim();
            if (nombre.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Ingrese el nombre o razon social del proveedor").showAndWait();
                evento.consume();
                return;
            }
            String ruc = campoRucNuevo.getText() == null || campoRucNuevo.getText().isBlank()
                    ? null : campoRucNuevo.getText().trim();

            Proveedor proveedor = new Proveedor();
            proveedor.setNombreRazonSocial(nombre);
            proveedor.setDocumento(ruc);

            try {
                proveedorDAO.guardar(proveedor);
            } catch (RuntimeException e) {
                new Alert(Alert.AlertType.ERROR, "No se pudo guardar el proveedor: " + e.getMessage()).showAndWait();
                evento.consume();
                return;
            }
            proveedorCreado[0] = proveedor;
        });

        dialogo.showAndWait();
        if (proveedorCreado[0] == null) {
            return;
        }

        comboProveedor.setItems(FXCollections.observableArrayList(proveedorDAO.listarTodos()));
        comboProveedor.getItems().stream()
                .filter(p -> p.getId().equals(proveedorCreado[0].getId()))
                .findFirst()
                .ifPresent(comboProveedor::setValue);
    }

    /**
     * Alta rapida de un producto nuevo mientras se registra una compra: solo pide categoria y
     * nombre. El codigo ya no se escribe a mano, se genera solo como el siguiente correlativo
     * de la categoria elegida (ver {@link #CATEGORIAS_PRODUCTO}). Precio minorista/mayorista
     * y stock se completan despues desde Catalogo (el stock, de hecho, se incrementa
     * automaticamente al agregar este producto a la compra).
     */
    @FXML
    private void onNuevoProducto() {
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.setTitle("Nuevo producto");
        dialogo.setHeaderText(null);

        ButtonType botonGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(botonGuardar, ButtonType.CANCEL);

        ComboBox<String> comboCategoriaNueva = new ComboBox<>(FXCollections.observableArrayList(CATEGORIAS_PRODUCTO.keySet()));
        comboCategoriaNueva.setPromptText("Seleccione una categoria");
        TextField campoNombreNuevo = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.addRow(0, new Label("Categoria:"), comboCategoriaNueva);
        grid.addRow(1, new Label("Nombre:"), campoNombreNuevo);
        dialogo.getDialogPane().setContent(grid);

        Producto[] productoCreado = new Producto[1];
        Button botonGuardarNodo = (Button) dialogo.getDialogPane().lookupButton(botonGuardar);
        botonGuardarNodo.addEventFilter(ActionEvent.ACTION, evento -> {
            String categoria = comboCategoriaNueva.getValue();
            String nombre = campoNombreNuevo.getText() == null ? "" : campoNombreNuevo.getText().trim();

            if (categoria == null || nombre.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Seleccione la categoria e ingrese el nombre del producto").showAndWait();
                evento.consume();
                return;
            }

            Producto producto = new Producto();
            producto.setCodigo(productoDAO.generarSiguienteCodigo(CATEGORIAS_PRODUCTO.get(categoria)));
            producto.setNombre(nombre);
            producto.setPrecioKgMinorista(BigDecimal.ZERO);
            producto.setPrecioKgMayorista(BigDecimal.ZERO);
            producto.setUnidadMedida("kg");
            producto.setStockDisponible(BigDecimal.ZERO);
            producto.setActivo(true);

            try {
                productoDAO.guardar(producto);
            } catch (RuntimeException e) {
                new Alert(Alert.AlertType.ERROR, "No se pudo guardar el producto: " + e.getMessage()).showAndWait();
                evento.consume();
                return;
            }
            productoCreado[0] = producto;
        });

        dialogo.showAndWait();
        if (productoCreado[0] == null) {
            return;
        }

        new Alert(Alert.AlertType.INFORMATION, "Producto \"" + productoCreado[0].getNombre() + "\" creado con el codigo "
                + productoCreado[0].getCodigo() + ". Recuerde completar sus precios en Catalogo antes de venderlo.")
                .showAndWait();

        comboProducto.setItems(FXCollections.observableArrayList(productoDAO.listarActivos()));
        comboProducto.getItems().stream()
                .filter(p -> p.getId().equals(productoCreado[0].getId()))
                .findFirst()
                .ifPresent(comboProducto::setValue);
    }

    @FXML
    private void onAgregarDetalle() {
        Producto producto = comboProducto.getValue();
        BigDecimal peso = parsearDecimal(campoPeso.getText());
        BigDecimal costo = parsearDecimal(campoCosto.getText());

        if (producto == null || peso == null || costo == null || peso.compareTo(BigDecimal.ZERO) <= 0) {
            new Alert(Alert.AlertType.WARNING, "Seleccione producto e ingrese peso y costo validos").showAndWait();
            return;
        }

        LocalDate fechaProduccion = campoFechaProduccion.getValue();
        LocalDate fechaVencimiento = campoFechaVencimiento.getValue();
        if (fechaProduccion != null && fechaVencimiento != null && !fechaVencimiento.isAfter(fechaProduccion)) {
            new Alert(Alert.AlertType.WARNING,
                    "La fecha de vencimiento debe ser posterior a la fecha de produccion "
                            + "(minimo un dia despues, ej. si produjo el " + fechaProduccion
                            + " debe vencer el " + fechaProduccion.plusDays(1) + " o despues)").showAndWait();
            return;
        }

        String lote = campoLote.getText() == null || campoLote.getText().isBlank()
                ? null : campoLote.getText().trim();
        detalles.add(new DetalleCompra(producto, peso, costo, lote,
                campoFechaProduccion.getValue(), campoFechaVencimiento.getValue()));
        refrescarTabla();
        campoPeso.clear();
        campoCosto.clear();
        campoLote.clear();
        campoFechaProduccion.setValue(null);
        campoFechaVencimiento.setValue(null);
    }

    @FXML
    private void onEliminarDetalle() {
        DetalleCompra seleccionado = tablaDetalle.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione un item de la tabla para eliminar").showAndWait();
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "Se eliminara \"" + seleccionado.getProducto().getNombre() + "\" de la lista de compra. Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setTitle("Eliminar item");
        confirmacion.setHeaderText(null);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        detalles.remove(seleccionado);
        refrescarTabla();
    }

    private void refrescarTabla() {
        ObservableList<DetalleCompra> items = FXCollections.observableArrayList(detalles);
        tablaDetalle.setItems(items);
        BigDecimal total = detalles.stream().map(DetalleCompra::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
        labelTotal.setText("Total: S/ " + total.setScale(2, RoundingMode.HALF_UP));
    }

    @FXML
    private void onRegistrarCompra() {
        Proveedor proveedor = comboProveedor.getValue();
        if (proveedor == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione un proveedor").showAndWait();
            return;
        }
        if (detalles.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Agregue al menos un producto a la compra").showAndWait();
            return;
        }

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setUsuario(usuarioSesion);
        detalles.forEach(compra::agregarDetalle);

        try {
            compraDAO.registrarCompra(compra);
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo registrar la compra: " + e.getMessage()).showAndWait();
            return;
        }

        String ticket = TicketGeneratorService.generarCompra(compra, usuarioSesion);
        TicketGeneratorService.mostrarVistaPrevia(ticket);

        detalles.clear();
        refrescarTabla();
        comboProducto.setItems(FXCollections.observableArrayList(productoDAO.listarActivos()));
    }

    private BigDecimal parsearDecimal(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texto.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
