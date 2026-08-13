package framework.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class PropertyReader {

	private static final Logger logger = LoggerFactory.getLogger(PropertyReader.class);

	private PropertyReader() {
		throw new UnsupportedOperationException("Property Reader class — do not instantiate.");
	}

	/*
	 * Method Description : Reads a property value from config.properties file.
	 * Input Parameter(s) if any : key (String) - The property key to look up
	 * Output Parameter(s) if any : String - The value associated with the given key
	 */
	public static String readProperty(String key) {
		String returnText = "";
		File file = new File("src/main/resources/properties/config.properties");
		try (FileInputStream fileInput = new FileInputStream(file)) {
			Properties properties = new Properties();
			properties.load(fileInput);
			returnText = properties.getProperty(key);
		} catch (Exception e) {
			logger.error("Exception occurred while reading data from Config file", e);
		}
		return returnText;
	}

	/*
	 * Method Description : Writes or updates a property value in config.properties file.
	 * Input Parameter(s) if any :
	 *   key (String) - The property key to update
	 *   value (String) - The value to set for the key
	 * Output Parameter(s) if any : String - The previous value of the property, or null if it did not exist
	 */
	public static String writeProperty(String key, String value) {
		String returnText = "";
		try {
			File dirl = new File(".");
			String file = dirl.getCanonicalPath() + File.separator + "config" + ".properties";
			try (FileInputStream fileIn = new FileInputStream(file);
				 FileOutputStream fileOut = new FileOutputStream(file)) {
				Properties configProperty = new Properties();
				configProperty.load(fileIn);
				configProperty.setProperty(key, value);
				configProperty.store(fileOut, "config properties");
			}
		} catch (Exception e) {
			logger.error("Exception occurred while writing data in property file", e);
		}
		return returnText;
	}

	/*
	 * Method Description : Fetches a property value from a specific application's property file for a given environment.
	 * Input Parameter(s) if any :
	 *   appName (String) - The application name (corresponding property file name)
	 *   env (String) - The environment key to fetch from the property file
	 * Output Parameter(s) if any : String - The value associated with the environment key, or null if not found
	 */
	public static String getPropertyFileURL(String appName, String env) {
		String returnText = null;
		String file = System.getProperty("user.dir")
				+ File.separator + "src/main/resources/properties"
				+ File.separator + appName + ".properties";

		try (FileInputStream fileInput = new FileInputStream(file)) {
			Properties properties = new Properties();
			properties.load(fileInput);

			String brand = framework.utils.WebActions.getRunTimeVariables("brand");

			if (brand == null || brand.trim().isEmpty()) {
				brand = System.getProperty("brand");
			}
			if (brand == null || brand.trim().isEmpty()) {
				brand = System.getenv("brand");
			}

			if (brand != null && !brand.trim().isEmpty()) {
				String brandEnvKey = brand.trim().toUpperCase() + "_" + env.trim().toUpperCase();
				returnText = properties.getProperty(brandEnvKey);
				if (returnText != null && !returnText.trim().isEmpty()) {
					logger.info("URL resolved using brand/env key: {}", brandEnvKey);
					return returnText;
				}
				logger.warn("No URL found for brand/env key: {}. Falling back to env key: {}", brandEnvKey, env);
			}

			returnText = properties.getProperty(env);
			if (returnText == null || returnText.trim().isEmpty()) {
				logger.error("No URL found in {}.properties for env key: {}", appName, env);
			}
		} catch (Exception e) {
			logger.error("Error occurred while fetching URL from Config", e);
		}
		return returnText;
	}

	/*
	 * Method Description : Reads a property value from a specific property file.
	 * Input Parameter(s) if any :
	 *   fileName (String) - The property file name (without extension)
	 *   key (String) - The property key to look up
	 * Output Parameter(s) if any : String - The value associated with the key, or empty string if not found
	 */
	public static String getProperty(String fileName, String key) {
		File file = new File("src/main/resources/properties/" + fileName + ".properties");
		String returnText = "";
		try (FileInputStream fileInput = new FileInputStream(file)) {
			Properties properties = new Properties();
			properties.load(fileInput);
			returnText = properties.getProperty(key);
		} catch (Exception e) {
			logger.error("Error occurred while getting property from property file", e);
		}
		return returnText;
	}

	/*
	 * Method Description : Reads a property value from config.properties and returns as a list of values.
	 * Accepts one or multiple comma-separated values (e.g. "value1" or "value1, value2, value3").
	 * Input Parameter(s) if any : key (String) - The property key to look up
	 * Output Parameter(s) if any : List<String> - Trimmed non-empty values; empty list if property is missing or blank
	 */
	public static List<String> readPropertyAsList(String key) {
		String value = readProperty(key);
		if (value == null || value.trim().isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.stream(value.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
	}
}
