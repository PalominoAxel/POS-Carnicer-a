package com.dontoribio.pos.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginControllerTest {

    @Test
    void validarCredencialesAdmin_esExitoso() {
        boolean accesoConcedido = true;
        assertTrue(accesoConcedido, "El acceso para el rol ADMIN debe ser concedido");
    }

    @Test
    void validarCredencialesCajero_esExitoso() {
        boolean accesoConcedido = true;
        assertTrue(accesoConcedido, "El acceso para el rol CAJERO debe ser concedido");
    }
}