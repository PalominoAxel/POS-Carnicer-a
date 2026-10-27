package com.dontoribio.pos.controller;

import com.dontoribio.pos.config.AppPaths;
import com.dontoribio.pos.dao.ProductoDAO;
import com.dontoribio.pos.dao.impl.ProductoDAOImpl;
import com.dontoribio.pos.model.Producto;
import com.dontoribio.pos.model.Usuario;
import com.dontoribio.pos.util.AutorizacionSupervisor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador de la ventana de Catalogo/Inventario: alta, edicion y desactivacion
 * de productos. Los precios por kg son fijos y solo cambian aqui (nunca en el POS).
 */
public class CatalogoController {

    private static final String FILTRO_TODOS = "Todos";
    private static final String FILTRO_ACTIVOS = "Activos";
    private static final String FILTRO_INACTIVOS = "Inactivos";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final long DIAS_ALERTA_VENCIMIENTO = 7;

    @FXML
    private TableView<Producto> tablaProductos;
    @FXML
    private TableColumn<Producto, String> colCodigo;
    @FXML
    private TableColumn<Producto, String> colNombre;
    @FXML
    private TableColumn<Producto, BigDecimal> colPrecioMinorista;
    @FXML
    private TableColumn<Producto, BigDecimal> colPrecioMayorista;
    @FXML
    private TableColumn<Producto, BigDecimal> colStock;
    @FXML
    private TableColumn<Producto, String> colLote;
    @FXML
    private TableColumn<Producto, LocalDate> colFechaProduccion;
    @FXML
    private TableColumn<Producto, LocalDate> colFechaVencimiento;
    @FXML
    private TableColumn<Producto, Boolean> colActivo;
    @FXML
    private TableColumn<Producto, LocalDate> colAlerta;

    @FXML
    private TextField campoBuscarProducto;
    @FXML
    private ComboBox<String> comboFiltroEstado;

    @FXML
    private TextField campoCodigo;
    @FXML
    private TextField campoNombre;
    @FXML
    private TextField campoPrecioMinorista;
    @FXML
    private TextField campoPrecioMayorista;
    @FXML
    private TextField campoStock;
    @FXML
    private TextField campoImagenPath;
    @FXML
    private CheckBox checkActivo;
    @FXML
    private TextField campoLote;
    @FXML
    private DatePicker datePickerFechaProduccion;
    @FXML
    private DatePicker datePickerFechaVencimiento;
    @FXML
    private Button botonEditar;
    @FXML
    private Label labelCambiosPendientes;

    /** Edicion de un producto, todavia no persistida (ver {@link #onGuardarCambiosPendientes()}). */
    private record CambioPendiente(Producto producto, boolean esNuevo) {
    }

    private final ProductoDAO productoDAO = new ProductoDAOImpl();
    private Producto productoSeleccionado;
    private ObservableList<Producto> productosBase;
    private FilteredList<Producto> productosFiltrados;
    private Usuario usuarioSesion;

    /**
     * Ediciones de la ficha ya confirmadas con "Guardar en ficha" pero todavia no escritas
     * en la base de datos (clave = codigo de producto). Permite editar varios productos
     * seguidos pidiendo la autorizacion de supervisor una sola vez, al final, en vez de una
     * vez por cada producto guardado.
     */
    private final Map<String, CambioPendiente> cambiosPendientes = new LinkedHashMap<>();

    /** Recibe el usuario autenticado desde MainPOSController, para pedir autorizacion de supervisor si es cajero. */
    public void setUsuarioSesion(Usuario usuario) {
        this.usuarioSesion = usuario;
    }

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecioMinorista.setCellValueFactory(new PropertyValueFactory<>("precioKgMinorista"));
        colPrecioMayorista.setCellValueFactory(new PropertyValueFactory<>("precioKgMayorista"));

        colStock.setCellValueFactory(new PropertyValueFactory<>("stockDisponible"));
        colStock.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP).toPlainString());
            }
        });

        colLote.setCellValueFactory(new PropertyValueFactory<>("loteActual"));
        colFechaProduccion.setCellValueFactory(new PropertyValueFactory<>("fechaProduccionActual"));
        colFechaProduccion.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : valor.format(FORMATO_FECHA));
            }
        });
        colFechaVencimiento.setCellValueFactory(new PropertyValueFactory<>("fechaVencimientoActual"));
        colFechaVencimiento.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate valor, boolean vacio) {
                super.updateItem(valor, vacio);
                setText(vacio || valor == null ? null : valor.format(FORMATO_FECHA));
            }
        });

        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        colAlerta.setCellValueFactory(new PropertyValueFactory<>("fechaVencimientoActual"));
        colAlerta.setCellFactory(columna -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fechaVencimiento, boolean vacio) {
                super.updateItem(fechaVencimiento, vacio);
                getStyleClass().removeAll("celda-alerta-vencido", "celda-alerta-proximo");
                if (vacio || fechaVencimiento == null) {
                    setText(null);
                    return;
                }
                long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
                if (diasRestantes < 0) {
                    setText("⚠ Vencido");
                    getStyleClass().add("celda-alerta-vencido");
                } else if (diasRestantes <= DIAS_ALERTA_VENCIMIENTO) {
                    setText("⚠ Vence en " + diasRestantes + (diasRestantes == 1 ? " dia" : " dias"));
                    getStyleClass().add("celda-alerta-proximo");
                } else {
                    setText(null);
                }
            }
        });

        comboFiltroEstado.setItems(FXCollections.observableArrayList(FILTRO_TODOS, FILTRO_ACTIVOS, FILTRO_INACTIVOS));
        comboFiltroEstado.setValue(FILTRO_TODOS);
        comboFiltroEstado.setOnAction(e -> aplicarFiltros());
        campoBuscarProducto.textProperty().addListener((obs, anterior, nuevo) -> aplicarFiltros());

        tablaProductos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            productoSeleccionado = nuevo;
            botonEditar.setDisable(nuevo == null);
        });

        cargarTabla();
        limpiarFormulario();
        actualizarLabelPendientes();
    }

    private void aplicarFiltros() {
        if (productosFiltrados == null) {
            return;
        }
        String filtroTexto = campoBuscarProducto.getText() == null
                ? "" : campoBuscarProducto.getText().trim().toLowerCase();
        String filtroEstado = comboFiltroEstado.getValue() == null ? FILTRO_TODOS : comboFiltroEstado.getValue();

        productosFiltrados.setPredicate(producto -> {
            boolean coincideTexto = filtroTexto.isEmpty()
                    || producto.getNombre().toLowerCase().contains(filtroTexto)
                    || producto.getCodigo().toLowerCase().contains(filtroTexto);
            boolean coincideEstado = switch (filtroEstado) {
                case FILTRO_ACTIVOS -> producto.isActivo();
                case FILTRO_INACTIVOS -> !producto.isActivo();
                default -> true;
            };
            return coincideTexto && coincideEstado;
        });
    }

    private void cargarTabla() {
        productosBase = FXCollections.observableArrayList(productoDAO.listarTodos());
        reaplicarCambiosPendientesEnTabla();
        productosFiltrados = new FilteredList<>(productosBase, p -> true);
        tablaProductos.setItems(productosFiltrados);
        aplicarFiltros();
    }

    /**
     * Despues de recargar desde la BD, vuelve a superponer las ediciones todavia
     * pendientes (ver {@link #cambiosPendientes}) para que no se pierdan de la vista si se
     * recarga la tabla por otro motivo (p. ej. al desactivar un producto distinto).
     */
    private void reaplicarCambiosPendientesEnTabla() {
        for (CambioPendiente cambio : cambiosPendientes.values()) {
            if (cambio.esNuevo()) {
                productosBase.add(cambio.producto());
                continue;
            }
            for (int i = 0; i < productosBase.size(); i++) {
                if (productosBase.get(i).getCodigo().equalsIgnoreCase(cambio.producto().getCodigo())) {
                    productosBase.set(i, cambio.producto());
                    break;
                }
            }
        }
    }

    private void actualizarLabelPendientes() {
        int pendientes = cambiosPendientes.size();
        labelCambiosPendientes.setText(pendientes == 0
                ? "Sin cambios pendientes"
                : pendientes + (pendientes == 1 ? " cambio pendiente" : " cambios pendientes"));
    }

    @FXML
    private void onEditarProducto() {
        if (productoSeleccionado == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione un producto de la tabla para editar").showAndWait();
            return;
        }
        campoCodigo.setText(productoSeleccionado.getCodigo());
        campoNombre.setText(productoSeleccionado.getNombre());
        campoPrecioMinorista.setText(productoSeleccionado.getPrecioKgMinorista().toPlainString());
        campoPrecioMayorista.setText(productoSeleccionado.getPrecioKgMayorista().toPlainString());
        campoStock.setText(productoSeleccionado.getStockDisponible().toPlainString());
        campoImagenPath.setText(productoSeleccionado.getImagenPath() == null ? "" : productoSeleccionado.getImagenPath());
        checkActivo.setSelected(productoSeleccionado.isActivo());

        campoLote.setText(productoSeleccionado.getLoteActual() == null ? "" : productoSeleccionado.getLoteActual());
        datePickerFechaProduccion.setValue(productoSeleccionado.getFechaProduccionActual());
        datePickerFechaVencimiento.setValue(productoSeleccionado.getFechaVencimientoActual());
        campoLote.setDisable(false);
        datePickerFechaProduccion.setDisable(false);
        datePickerFechaVencimiento.setDisable(false);
    }

    /** Deja la ficha en blanco, lista para registrar un producto nuevo. */
    private void limpiarFormulario() {
        productoSeleccionado = null;
        tablaProductos.getSelectionModel().clearSelection();
        campoCodigo.clear();
        campoNombre.clear();
        campoPrecioMinorista.clear();
        campoPrecioMayorista.clear();
        campoStock.setText("0");
        campoImagenPath.clear();
        checkActivo.setSelected(true);
        campoLote.clear();
        datePickerFechaProduccion.setValue(null);
        datePickerFechaVencimiento.setValue(null);
        campoLote.setDisable(true);
        datePickerFechaProduccion.setDisable(true);
        datePickerFechaVencimiento.setDisable(true);
        botonEditar.setDisable(true);
    }

    @FXML
    private void onExaminarImagen() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar imagen del producto");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagenes", "*.png", "*.jpg", "*.jpeg"));
        var archivo = chooser.showOpenDialog(tablaProductos.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        Path carpetaDestino = AppPaths.carpetaImagenesProductos().toPath();
        try {
            String rutaAbsoluta = copiarImagenACarpetaProductos(archivo.toPath(), carpetaDestino);
            campoImagenPath.setText(rutaAbsoluta);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "No se pudo copiar la imagen a " + carpetaDestino
                    + ": " + e.getMessage()).showAndWait();
        }
    }

    /**
     * Copia la imagen elegida a la carpeta de imagenes de esta instalacion
     * ({@link AppPaths#carpetaImagenesProductos()}, fuera de la carpeta del programa, para
     * que sobreviva reinstalaciones) y devuelve la ruta absoluta para guardarla en la BD.
     */
    private String copiarImagenACarpetaProductos(Path origen, Path carpetaDestino) throws IOException {
        Path destino = carpetaDestino.resolve(origen.getFileName());
        if (!origen.toAbsolutePath().normalize().equals(destino.toAbsolutePath().normalize())) {
            Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return destino.toAbsolutePath().toString().replace('\\', '/');
    }

    /**
     * Valida y "apunta" los cambios de la ficha en memoria ({@link #cambiosPendientes}),
     * sin pedir autorizacion ni tocar la base de datos todavia: eso se hace una sola vez,
     * para todos los productos pendientes juntos, en {@link #onGuardarCambiosPendientes()}.
     * La tabla se actualiza al instante para que el cambio se vea, aunque no este guardado.
     */
    @FXML
    private void onGuardarProducto() {
        String codigo = campoCodigo.getText() == null ? "" : campoCodigo.getText().trim();
        String nombre = campoNombre.getText() == null ? "" : campoNombre.getText().trim();
        if (codigo.isEmpty() || nombre.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Ingrese codigo y nombre del producto").showAndWait();
            return;
        }

        BigDecimal precioMinorista = parsearDecimal(campoPrecioMinorista.getText());
        BigDecimal precioMayorista = parsearDecimal(campoPrecioMayorista.getText());
        BigDecimal stock = parsearDecimal(campoStock.getText());
        if (precioMinorista == null || precioMayorista == null || stock == null) {
            new Alert(Alert.AlertType.WARNING, "Precios y stock deben ser numeros validos").showAndWait();
            return;
        }
        if (precioMinorista.signum() < 0 || precioMayorista.signum() < 0 || stock.signum() < 0) {
            new Alert(Alert.AlertType.WARNING, "Precios y stock no pueden ser negativos").showAndWait();
            return;
        }

        // Un producto ya persistido tiene id; uno recien agregado a cambiosPendientes y
        // vuelto a editar antes de guardarlo todavia no lo tiene, aunque este seleccionado.
        boolean editandoExistente = productoSeleccionado != null && productoSeleccionado.getId() != null;
        Producto producto = editandoExistente ? productoSeleccionado : new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setPrecioKgMinorista(precioMinorista);
        producto.setPrecioKgMayorista(precioMayorista);
        producto.setStockDisponible(stock);
        producto.setUnidadMedida(producto.getUnidadMedida() == null ? "kg" : producto.getUnidadMedida());
        producto.setImagenPath(campoImagenPath.getText() == null || campoImagenPath.getText().isBlank()
                ? null : campoImagenPath.getText().trim());
        producto.setActivo(checkActivo.isSelected());

        if (editandoExistente) {
            producto.setLoteActual(campoLote.getText() == null || campoLote.getText().isBlank()
                    ? null : campoLote.getText().trim());
            producto.setFechaProduccionActual(datePickerFechaProduccion.getValue());
            producto.setFechaVencimientoActual(datePickerFechaVencimiento.getValue());
        }

        cambiosPendientes.put(codigo, new CambioPendiente(producto, !editandoExistente));
        if (!editandoExistente && !productosBase.contains(producto)) {
            productosBase.add(producto);
        }
        tablaProductos.refresh();
        actualizarLabelPendientes();
        limpiarFormulario();
    }

    /**
     * Pide la autorizacion de supervisor UNA SOLA VEZ y recien ahi persiste todos los
     * productos de {@link #cambiosPendientes}, sin importar cuantos se hayan editado.
     */
    @FXML
    private void onGuardarCambiosPendientes() {
        if (cambiosPendientes.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No hay cambios pendientes para guardar").showAndWait();
            return;
        }
        if (!AutorizacionSupervisor.verificarPermiso(usuarioSesion, "modificar el catalogo de productos")) {
            return;
        }

        int total = cambiosPendientes.size();
        int guardados = 0;
        StringBuilder errores = new StringBuilder();
        StringBuilder sinCompraRegistrada = new StringBuilder();
        for (CambioPendiente cambio : cambiosPendientes.values()) {
            Producto producto = cambio.producto();
            try {
                if (cambio.esNuevo()) {
                    productoDAO.guardar(producto);
                } else {
                    productoDAO.actualizar(producto);
                    boolean intentaTrazabilidad = producto.getLoteActual() != null
                            || producto.getFechaProduccionActual() != null
                            || producto.getFechaVencimientoActual() != null;
                    boolean trazabilidadGuardada = productoDAO.actualizarTrazabilidad(producto.getId(),
                            producto.getLoteActual(), producto.getFechaProduccionActual(),
                            producto.getFechaVencimientoActual());
                    if (intentaTrazabilidad && !trazabilidadGuardada) {
                        sinCompraRegistrada.append("- ").append(producto.getNombre()).append('\n');
                    }
                }
                guardados++;
            } catch (RuntimeException e) {
                errores.append("- ").append(producto.getNombre()).append(": ").append(e.getMessage()).append('\n');
            }
        }

        cambiosPendientes.clear();
        cargarTabla();
        limpiarFormulario();
        actualizarLabelPendientes();

        String mensaje = guardados + " de " + total + " producto(s) guardado(s) correctamente.";
        if (sinCompraRegistrada.length() > 0) {
            mensaje += "\n\nEl lote/fecha de produccion/fecha de vencimiento NO se pudo guardar en estos "
                    + "productos porque no tienen ninguna compra registrada (registre una compra en el modulo "
                    + "de Compras primero):\n" + sinCompraRegistrada;
        }
        if (errores.length() > 0) {
            mensaje += "\n\nNo se pudieron guardar:\n" + errores;
        }
        boolean hayAdvertencias = sinCompraRegistrada.length() > 0 || errores.length() > 0;
        new Alert(hayAdvertencias ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION, mensaje).showAndWait();
    }

    /** Descarta todos los cambios de {@link #cambiosPendientes} sin pedir autorizacion ni tocar la base de datos. */
    @FXML
    private void onCancelarCambiosPendientes() {
        if (cambiosPendientes.isEmpty()) {
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "Se descartaran " + cambiosPendientes.size() + " cambio(s) pendiente(s) sin guardar. Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setTitle("Cancelar cambios pendientes");
        confirmacion.setHeaderText(null);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        cambiosPendientes.clear();
        cargarTabla();
        limpiarFormulario();
        actualizarLabelPendientes();
    }

    @FXML
    private void onDesactivarProducto() {
        if (!AutorizacionSupervisor.verificarPermiso(usuarioSesion, "desactivar un producto del catalogo")) {
            return;
        }
        if (productoSeleccionado == null) {
            new Alert(Alert.AlertType.WARNING, "Seleccione un producto de la tabla").showAndWait();
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "Se desactivara el producto \"" + productoSeleccionado.getNombre() + "\". Ya no estara disponible "
                        + "para la venta. Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setTitle("Desactivar producto");
        confirmacion.setHeaderText(null);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        if (respuesta.isEmpty() || respuesta.get() != ButtonType.OK) {
            return;
        }

        productoSeleccionado.setActivo(false);
        productoDAO.actualizar(productoSeleccionado);
        cargarTabla();
        limpiarFormulario();
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
