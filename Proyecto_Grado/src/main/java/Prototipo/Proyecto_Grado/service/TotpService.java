package Prototipo.Proyecto_Grado.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Servicio para generación y validación de códigos TOTP (RFC 6238).
 * Compatible con Google Authenticator, Microsoft Authenticator y Authy.
 */
@Service
@Slf4j
public class TotpService {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private final SecureRandom random = new SecureRandom();

    private final java.util.Map<String, Long> ultimoWindowUsado = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Genera un secreto aleatorio en Base32 (32 caracteres = 160 bits, especificación RFC 4226/6238).
     */
    public String generarSecretoBase32() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(BASE32_CHARS.charAt(random.nextInt(BASE32_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Construye la URI estándar otpauth:// para escanear en apps autenticadoras.
     */
    public String generarOtpAuthUri(String correo, String secreto) {
        String issuer = "Javeriana Cali ITS";
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8).replace("+", "%20");
        String encodedAccount = URLEncoder.encode(correo, StandardCharsets.UTF_8).replace("+", "%20").replace("%40", "@");
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                encodedIssuer, encodedAccount, secreto, encodedIssuer);
    }

    /**
     * Genera la imagen QR de forma local y autónoma con ZXing en Base64.
     * Evita la fuga de secretos 2FA a servidores externos de internet.
     */
    public String generarQrBase64(String otpAuthUri, int width, int height) {
        try {
            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(
                    otpAuthUri, com.google.zxing.BarcodeFormat.QR_CODE, width, height);

            java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            log.error("Error al generar imagen QR local con ZXing: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Valida si el código de 6 dígitos ingresado es válido para el secreto dado.
     * Evalúa la ventana actual T, así como T-1 y T+1 (tolerancia a desfase de reloj).
     * Utiliza MessageDigest.isEqual para mitigar ataques de temporización (timing attacks).
     * Registra la ventana consumida para mitigar ataques de repetición (replay attack).
     */
    public boolean validarCodigo(String secreto, String codigoIngresado) {
        if (secreto == null || codigoIngresado == null) return false;
        String codigoLimpio = codigoIngresado.replaceAll("\\s+", "").trim();
        if (codigoLimpio.length() != DIGITS) return false;

        long currentWindow = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        Long lastUsedWindow = ultimoWindowUsado.get(secreto);

        // Limpieza periódica de ventanas antiguas para proteger el heap
        if (ultimoWindowUsado.size() > 5000) {
            ultimoWindowUsado.entrySet().removeIf(e -> (currentWindow - e.getValue()) > 10);
        }

        // Comprueba ventana actual y adyacentes para tolerar desfases leves de reloj
        for (long window = currentWindow - 1; window <= currentWindow + 1; window++) {
            if (lastUsedWindow != null && window <= lastUsedWindow) {
                // Ventana ya consumida previamente (prevención de replay attack)
                continue;
            }
            try {
                String expected = generarCodigoParaVentana(secreto, window);
                if (java.security.MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        codigoLimpio.getBytes(StandardCharsets.UTF_8))) {
                    ultimoWindowUsado.put(secreto, window);
                    return true;
                }
            } catch (Exception e) {
                log.error("Error al calcular TOTP: {}", e.getMessage());
            }
        }
        return false;
    }

    String generarCodigoParaVentana(String secreto, long window) throws Exception {
        byte[] key = decodeBase32(secreto);
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (window & 0xFF);
            window >>= 8;
        }

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, DIGITS);
        return String.format("%06d", otp);
    }

    private byte[] decodeBase32(String base32) {
        String clean = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int numBytes = clean.length() * 5 / 8;
        byte[] result = new byte[numBytes];

        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (char c : clean.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }
}
