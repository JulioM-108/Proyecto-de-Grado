package Prototipo.Proyecto_Grado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
public class ProyectoGradoApplication {

	static {
		cargarVariablesEntorno();
	}

	public static void main(String[] args) {
		cargarVariablesEntorno();
		SpringApplication.run(ProyectoGradoApplication.class, args);
	}

	private static boolean envCargado = false;

	/**
	 * Carga automáticamente las variables definidas en el archivo .env
	 * hacia las propiedades del sistema (System.setProperty) antes del arranque de Spring Boot.
	 * Esto garantiza que GEMINI_API_KEY y la configuración de Supabase estén disponibles
	 * sin requerir configuración manual de variables de entorno en el sistema operativo.
	 */
	public static synchronized void cargarVariablesEntorno() {
		if (envCargado) return;

		List<Path> candidatos = List.of(
				Path.of(".env"),
				Path.of("Proyecto_Grado", ".env"),
				Path.of("..", ".env"),
				Path.of("..", "Proyecto_Grado", ".env")
		);

		Path envPath = null;
		for (Path p : candidatos) {
			if (Files.exists(p)) {
				envPath = p;
				break;
			}
		}

		if (envPath != null) {
			try {
				List<String> lineas = Files.readAllLines(envPath);
				for (String linea : lineas) {
					linea = linea.trim();
					if (linea.isEmpty() || linea.startsWith("#")) continue;
					int idx = linea.indexOf('=');
					if (idx > 0) {
						String key = linea.substring(0, idx).trim();
						String val = linea.substring(idx + 1).trim();
						// Remover comillas envolventes si existen
						if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
							val = val.substring(1, val.length() - 1);
						}
						// Inyectar en System.setProperty si no fue definida previamente en el SO
						if (System.getProperty(key) == null && System.getenv(key) == null) {
							System.setProperty(key, val);
						}
					}
				}
				envCargado = true;
				System.out.println(">>> [.env] Variables cargadas exitosamente desde: " + envPath.toAbsolutePath().normalize());
			} catch (IOException e) {
				System.err.println(">>> Error leyendo .env: " + e.getMessage());
			}
		} else {
			System.out.println(">>> [.env] Archivo .env no encontrado en las rutas evaluadas. Directorio de ejecución: " + Path.of(".").toAbsolutePath().normalize());
		}
	}

}
