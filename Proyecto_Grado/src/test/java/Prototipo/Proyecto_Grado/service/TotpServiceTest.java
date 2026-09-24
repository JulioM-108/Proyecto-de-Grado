package Prototipo.Proyecto_Grado.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {

    private TotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpService();
    }

    @Test
    @DisplayName("Generar secreto Base32 válido de 16 caracteres")
    void testGenerarSecretoBase32() {
        String secreto = totpService.generarSecretoBase32();
        assertNotNull(secreto);
        assertEquals(16, secreto.length());
        assertTrue(secreto.matches("^[A-Z2-7]+$"), "El secreto debe cumplir la especificación Base32 RFC 4648");
    }

    @Test
    @DisplayName("Generar URI otpauth correcta para Google Authenticator")
    void testGenerarOtpAuthUri() {
        String correo = "docente@javerianacali.edu.co";
        String secreto = "JBSWY3DPEHPK3PXP";
        String uri = totpService.generarOtpAuthUri(correo, secreto);

        assertNotNull(uri);
        assertTrue(uri.startsWith("otpauth://totp/"), "Debe comenzar con el protocolo otpauth");
        assertTrue(uri.contains(correo), "Debe contener el correo del docente");
        assertTrue(uri.contains("secret=" + secreto), "Debe contener el secreto");
        assertTrue(uri.contains("issuer=Javeriana%20Cali%20ITS"), "Debe contener el emisor institucional");
    }

    @Test
    @DisplayName("Validar código actual contra el secreto generado")
    void testValidarCodigoActual() throws Exception {
        String secreto = totpService.generarSecretoBase32();
        long currentInterval = System.currentTimeMillis() / 1000L / 30L;
        String codigoCorrecto = totpService.generarCodigoParaVentana(secreto, currentInterval);

        assertNotNull(codigoCorrecto);
        assertEquals(6, codigoCorrecto.length());
        assertTrue(totpService.validarCodigo(secreto, codigoCorrecto), "El código actual debe ser validado como true");
    }

    @Test
    @DisplayName("Rechazar códigos inválidos o nulos")
    void testRechazarCodigosInvalidos() {
        String secreto = totpService.generarSecretoBase32();
        assertFalse(totpService.validarCodigo(secreto, "000000"));
        assertFalse(totpService.validarCodigo(secreto, "ABCDEF"));
        assertFalse(totpService.validarCodigo(secreto, ""));
        assertFalse(totpService.validarCodigo(secreto, null));
        assertFalse(totpService.validarCodigo(null, "123456"));
    }
}
