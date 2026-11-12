package Prototipo.Proyecto_Grado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Punto de entrada del Tutor Socrático (Spring Boot).
 * Antes de arrancar Spring carga las variables del archivo .env como propiedades del sistema
 * y fija la zona horaria por defecto de la JVM.
 */
@SpringBootApplication
public class ProyectoGradoApplication {

	// Se ejecuta al cargar la clase, antes de main y antes de crear el contexto de Spring (también en los tests).
	static {
		cargarVariablesEntorno();
		configurarZonaHoraria();
	}

	/**
	 * Zona horaria de la aplicación (por defecto Colombia). Sin esto, las fechas leídas de Supabase se mostraban
	 * en UTC: un mensaje del 18/10 a las 20:26 aparecía como "19/10 01:26". Junto con
	 * hibernate.timezone.default_storage=NORMALIZE, las fechas se leen y se muestran en esta zona en todas las vistas.
	 * Se puede cambiar con APP_ZONA_HORARIA: primero se busca como propiedad del sistema (incluidas las del .env)
	 * y luego como variable de entorno del SO.
	 */
	public static void configurarZonaHoraria() {
		String zona = System.getProperty("APP_ZONA_HORARIA", System.getenv().getOrDefault("APP_ZONA_HORARIA", "America/Bogota"));
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(java.time.ZoneId.of(zona)));
	}

	/**
	 * Arranca la aplicación. Repite la carga del .env por seguridad; es inofensivo porque
	 * no sobrescribe propiedades que ya estén definidas.
	 */
	public static void main(String[] args) {
		cargarVariablesEntorno();
		SpringApplication.run(ProyectoGradoApplication.class, args);
	}

	/** Indica si el .env ya se leyó, para no procesarlo de nuevo. */
	private static boolean envCargado = false;

	/**
	 * Carga automáticamente las variables definidas en el archivo .env
	 * hacia las propiedades del sistema (System.setProperty) antes del arranque de Spring Boot.
	 * Esto garantiza que GEMINI_API_KEY y la configuración de Supabase estén disponibles
	 * sin requerir configuración manual de variables de entorno en el sistema operativo.
	 * <p>
	 * Formato admitido: líneas CLAVE=valor; se ignoran líneas vacías y comentarios con #, y se quitan
	 * las comillas que envuelvan el valor. Las variables del SO y las propiedades ya definidas tienen
	 * prioridad sobre el .env. También la invoca {@link Prototipo.Proyecto_Grado.config.EnvLoaderEnvironmentPostProcessor}.
	 */
	public static synchronized void cargarVariablesEntorno() {
		if (envCargado) return;

		// Rutas relativas al directorio de ejecución: sirve tanto desde Proyecto_Grado como desde la carpeta superior.
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
						// Quitar comillas envolventes (dobles o simples)
						if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
							val = val.substring(1, val.length() - 1);
						}
						// Solo se define si no existe ya como propiedad del sistema (-D) ni como variable del SO
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
