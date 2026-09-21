import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AWToAT {
	private static final Map<String, String> ACCESS_POINT_MAP = new HashMap<>();
	
	static {
		ACCESS_POINT_MAP.put("accessible", "public");
		ACCESS_POINT_MAP.put("extendable", "public-f");
		ACCESS_POINT_MAP.put("mutable", "public-f");
	}
	
	public String minecraftVersion;
	
	public File remap(File file, String minecraftVersion) {
		this.minecraftVersion = minecraftVersion.replace("_", ".");
		File atFile = createATFile(file);
		processFile(file, atFile);
		return atFile;
	}
	
	private File createATFile(File file) {
		File metaInf = new File(file.getParentFile(), "META-INF");
		if (!metaInf.exists() && !metaInf.mkdir()) throw new RuntimeException("Error creating META-INF folder");
		File atFile = new File(metaInf, "accesstransformer.cfg");
		try {
			atFile.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException("Error creating new file", e);
		}
		return atFile;
	}
	
	private void processFile(File file, File atFile) {
		/* Validates if we need to recreate the Access Transformer file if it's out of date */
		// Get the hash of the file
		String fileHash = getFileHash(file);
		try (Scanner atScanner = new Scanner(atFile)) {
			// Check if the AT file is up-to-date by comparing the hash of the file with the hash stored in the AT file
			boolean hashFound = false;
			while (atScanner.hasNextLine()) {
				String line = atScanner.nextLine();
				if (hashCheck(line, fileHash)) {
					hashFound = true;
				}
			}
			
			// If the AT file is up-to-date, print a message and return
			if (hashFound) {
				System.out.println("Access Transformer file is already up to date.");
				return;
			}
		} catch (FileNotFoundException ignored) {
			// If the AT file does not exist, continue
		}
		
		/* Creates the Access Transformer file */
		// Opens a scanner for reading the Access Widener file and a writer for writing to the Access Transformer file
		try (Scanner scanner = new Scanner(file); FileWriter writer = new FileWriter(atFile)) {
			// Create an ExecutorService with a fixed thread pool size equal to the number of available processors
			ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			// List to hold Future objects representing results of computation
			List<Future<String>> futures = new ArrayList<>();
			
			// Write the hash of the file to the AT file
			writer.write("#DH_MAPPING_HASH:" + fileHash + "\n");
			
			// Read each line from the file
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				// Skip lines starting with "accessWidener", "#" or blank lines
				if (line.startsWith("accessWidener") || line.startsWith("#") || line.isBlank()) continue;
				
				// Submit the line to the executor service for processing
				// The processing is done by the processLine method
				futures.add(executor.submit(() -> processLine(line.split(" "))));
			}
			
			// Write the results to the output file
			// The results are obtained by calling the get method on each Future
			for (Future<String> future : futures) {
				writer.write(future.get());
			}
			
			// Shutdown the executor service to free up resources
			executor.shutdown();
		} catch (Exception e) {
			throw new RuntimeException("Error reading or writing to file", e);
		}
	}
	
	private String processLine(String[] fields) {
		// fields[0] = access point like "accessible", "extendable", "mutable"
		// fields[1] = type like "field", "method", "class"
		// fields[2] = class name
		// fields[3] = field/method name
		// fields[4] = field/method descriptor
		
		try {
			// Store the original field/method name
			String originalName = "";
			
			// If there is a class name, replace the slashes with dots in the package name
			if (fields.length > 2) fields[2] = fields[2].replace("/", ".");
			
			// If there is a field/method name, store the original name and remap it to SRG
			if (fields.length > 3) {
				originalName = fields[3];
				fields[3] = remapToSRG(fields[2], fields[3]);
			}
			
			StringBuilder line = new StringBuilder(ACCESS_POINT_MAP.getOrDefault(fields[0], "public")).append(" ");
			switch (fields[1]) {
				case "field":
					line.append(fields[2]).append(" ").append(fields[3]).append(" #").append(originalName);
					// It'll be like: access-point class-name field-name-SRG # field-name-Mojmap
					// Eg: public net.minecraft.client.Minecraft f_90981_ # instance
					break;
				case "method":
					line.append(fields[2]).append(" ").append(fields[3]).append(fields[4]).append(" #").append(originalName);
					// It'll be like: access-point class-name method-name-SRG method-descriptor # method-name-Mojmap
					// Eg: public net.minecraft.client.Minecraft m_172797_()Lnet/minecraft/client/Minecraft; # getInstance
					break;
				default:
					line.append(fields[2]);
					// It'll be like: access-point class-name
					// Eg: public net.minecraft.client.Minecraft
					break;
			}
			line.append("\n");
			return line.toString();
		} catch (Exception e) {
			throw new RuntimeException("Error processing line", e);
		}
	}
	
	private boolean hashCheck(String line, String fileHash) {
		if (line.startsWith("#DH_MAPPING_HASH:")) {
			String hash = line.substring(17);
			return hash.equals(fileHash);
		}
		return false;
	}
	
	public String getFileHash(File file) {
		try {
			MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
			try (InputStream fis = new FileInputStream(file)) {
				byte[] byteArray = new byte[1024];
				int bytesCount;
				
				// Read file data and update in message digest
				while ((bytesCount = fis.read(byteArray)) != -1) {
					shaDigest.update(byteArray, 0, bytesCount);
				}
			}
			
			byte[] bytes = shaDigest.digest();
			
			// Convert byte array into signum representation
			StringBuilder sb = new StringBuilder();
			for (byte aByte : bytes) {
				sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
			}
			
			// Return complete hash
			return sb.toString();
		} catch (NoSuchAlgorithmException | IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	
	// WARNING: BELOW LIES HIGHLY CURSED CODE AND MIGHT EVEN BE ILLEGAL
	
	
	
	
	// Flag to track if there was an error in the GET request
	boolean error = false;
	
	/**
	 * This method returns a field or method name from Mojang mappings as SRG mappings.
	 * It makes a GET request to the Linkie API to fetch the SRG name.
	 *
	 * @param clazz The class name
	 * @param name The field or method name
	 * @return The SRG name
	 * @throws Exception If there is an error in the GET request or the SRG name is not found in the response
	 */
	private String remapToSRG(String clazz, String name) throws Exception {
		// Encode the class and field/method name to be used in the URL
		String query = URLEncoder.encode(clazz + "." + name, StandardCharsets.UTF_8);
		// Construct the URL for the GET request
		String urlString = "https://linkieapi.shedaniel.me/api/search?namespace=mojang&query=" + query + "&version=" + this.minecraftVersion + "&limit=1&allowClasses=false&allowFields=true&allowMethods=true&translate=mojang_srg";
		URL url = new URI(urlString).toURL();
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		int responseCode = conn.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuilder content = new StringBuilder();
			// Read the response line by line
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();
			conn.disconnect();
			// Regex to find the SRG name in the response
			Pattern pattern = Pattern.compile("\"l\"\\s*:\\s*\\{[^}]*\"i\"\\s*:\\s*\"([^\"]*)\"");
			Matcher matcher = pattern.matcher(content.toString());
			if (matcher.find()) return matcher.group(1);
			else throw new Exception("Couldn't find the SRG mapping for name: " + name + "\nCould not find 'i' in 'l' object in the response"); // `i` is the SRG name which is stored in the `l` JSON object
		} else {
			if (error) {
				// If there was an error in the GET request, and we already tried again, throw an exception
				throw new Exception("The GET request failed");
			}
			// If there was an error in the GET request, wait 2.5 seconds and try again as we probably got rate limited
			error = true;
			Thread.sleep(2500);
			return remapToSRG(clazz, name);
		}
	}
}