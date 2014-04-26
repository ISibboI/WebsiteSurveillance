package de.isibboi.websitesurveillance;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.xml.bind.DatatypeConverter;

public class WebsiteSurveillance {
	private static final String HASH_PROPERTY_PREFIX = "hash_";
	private static final String URL_PROPERTY_PREFIX = "url_";
	private static final String DOWN_PROPERTY_PREFIX = "down_";
	private static final String SITES_LIST_PROPERTY = "sites";
	private static final String DEBUG_FLAG = "debug";
	private static final String CONFIG_DIR = ".websitechangetracker";

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		Properties p = new Properties();
		File f = getFile();
		Reader in = new FileReader(f);
		p.load(in);
		in.close();

		if (p.getProperty(SITES_LIST_PROPERTY) == null) {
			p.setProperty(SITES_LIST_PROPERTY, "");
			error("No sites specified!");
		}

		if (p.getProperty(DEBUG_FLAG) != null) {
			info("Debug flag set");
		}

		String[] names = p.getProperty(SITES_LIST_PROPERTY).split("\\s*,\\s*");
		List<String> changed = new ArrayList<>();
		List<String> added = new ArrayList<>();
		List<String> down = new ArrayList<>();
		List<String> up = new ArrayList<>();

		for (String name : names) {
			if (p.getProperty(URL_PROPERTY_PREFIX + name) == null) {
				error("Missing url for: " + name);
			}

			try {
				URL url = new URL(p.getProperty(URL_PROPERTY_PREFIX + name));
				String newHash = getHash(url);

				if (p.getProperty(HASH_PROPERTY_PREFIX + name) == null) {
					// Site was added
					added.add(name);
					p.setProperty(HASH_PROPERTY_PREFIX + name, "" + newHash);
					p.setProperty(DOWN_PROPERTY_PREFIX + name, "false");
					
					if (newHash == null) {
						down.add(name);
					}
				} else {
					if (newHash == null) {
						if ("false".equals(p.getProperty(DOWN_PROPERTY_PREFIX + name))) {
							p.setProperty(DOWN_PROPERTY_PREFIX + name, "true");
							down.add(name);
						}
					} else {
						if ("true".equals(p.getProperty(DOWN_PROPERTY_PREFIX + name))) {
							p.setProperty(DOWN_PROPERTY_PREFIX + name, "false");
							up.add(name);
						}

						if (!newHash.equals(p.getProperty(HASH_PROPERTY_PREFIX + name))) {
							changed.add(name);
							p.setProperty(HASH_PROPERTY_PREFIX + name, newHash);
						}
					}
				}
			} catch (MalformedURLException e) {
				error("Malformed url: " + p.getProperty(URL_PROPERTY_PREFIX + name));
			}
		}

		Writer out = new FileWriter(f);
		p.store(out, "");
		out.close();

		if (added.size() > 0) {
			info("Websites added: " + listToString(added));
		}

		if (changed.size() > 0) {
			warn("Websites changed: " + listToString(changed));
		}
		
		if (up.size() > 0) {
			info("Websites up again: " + listToString(up));
		}
		
		if (down.size() > 0) {
			warn("Websites down: " + listToString(down));
		}
	}

	private static String listToString(List<String> list) {
		Iterator<String> items = list.iterator();
		StringBuilder result = new StringBuilder();

		while (items.hasNext()) {
			result.append(items.next());

			if (items.hasNext()) {
				result.append(", ");
			}
		}

		return result.toString();
	}

	private static String getHash(URL url) throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");

		try (InputStream in = url.openStream()) {
			byte[] buffer = new byte[1024 * 64];
			int length = 0;

			while (true) {
				length = in.read(buffer);

				if (length == -1) {
					break;
				} else {
					md5.update(buffer, 0, length);
				}
			}

			byte[] digest = md5.digest();
			return DatatypeConverter.printHexBinary(digest);
		} catch (IOException e) {
			return null;
		}
	}

	private static File getFile() throws IOException {
		File home = new File(System.getProperty("user.home"));

		if (!home.isDirectory()) {
			error("No user home directory found.");
		}

		File configDir = new File(home, CONFIG_DIR);

		if (configDir.exists() && !configDir.isDirectory()) {
			error(configDir.getAbsolutePath() + " already exists, but is not a file");
		}

		if (!configDir.exists()) {
			configDir.mkdir();
		}

		File f = new File(configDir, ".properties");

		if (f.exists() && !f.isFile()) {
			error(".properties already exists, but is not a file");
		}

		if (!f.exists()) {
			f.createNewFile();
		}

		return f;
	}

	private static void info(String info) {
		JOptionPane.showMessageDialog(null, info, "Information", JOptionPane.INFORMATION_MESSAGE);
	}

	private static void warn(String warning) {
		JOptionPane.showMessageDialog(null, warning, "Information", JOptionPane.WARNING_MESSAGE);
	}

	private static void error(String error) {
		JOptionPane.showMessageDialog(null, error, "Error!", JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}
}