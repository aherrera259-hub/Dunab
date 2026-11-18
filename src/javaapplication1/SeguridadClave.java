package javaapplication1;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Seguridad integrada en Java, sin bibliotecas ni contraseñas en texto plano. */
final class SeguridadClave {
    private static final int ITERACIONES = 120000;
    private SeguridadClave() { }
    static byte[] nuevaSal() {
        byte[] sal = new byte[16]; new SecureRandom().nextBytes(sal); return sal;
    }
    static byte[] derivar(char[] clave, byte[] sal) {
        PBEKeySpec especificacion = new PBEKeySpec(clave, sal, ITERACIONES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("El entorno Java no permite proteger las contraseñas.", ex);
        } finally { especificacion.clearPassword(); }
    }
    static boolean comprobar(char[] clave, byte[] sal, byte[] esperada) {
        if (clave == null || sal.length == 0 || esperada.length == 0) return false;
        byte[] actual = derivar(clave, sal);
        try { return MessageDigest.isEqual(actual, esperada); }
        finally { Arrays.fill(actual, (byte) 0); }
    }
}
