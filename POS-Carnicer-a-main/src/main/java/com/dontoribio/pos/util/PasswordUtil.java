package com.dontoribio.pos.util;

import org.mindrot.jbcrypt.BCrypt;

/** Utilidad de cifrado/verificacion de contrasenas con BCrypt (jBCrypt). */
public final class PasswordUtil {

    private static final int ROUNDS_SALT = 12;

    private PasswordUtil() {
    }

    /** Genera el hash BCrypt de una contrasena en texto plano, listo para persistir. */
    public static String hashPassword(String passwordPlano) {
        return BCrypt.hashpw(passwordPlano, BCrypt.gensalt(ROUNDS_SALT));
    }

    /** Verifica una contrasena en texto plano contra su hash almacenado. */
    public static boolean verificarPassword(String passwordPlano, String passwordHash) {
        if (passwordPlano == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(passwordPlano, passwordHash);
    }
}
