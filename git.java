import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;


public class Git {
	public static String createBlob(String sourceFilePath) throws IOException {
		String fileHash = hashFile(sourceFilePath);

		Path objects = Path.of("git", "objects");
		Files.createDirectories(objects);

		Path blob = objects.resolve(fileHash);
		if (Files.notExists(blob)) {
			Files.copy(Path.of(sourceFilePath), blob);
		}

		return fileHash;
	}

	public static void stageFile(String filePath) throws IOException {
		Path repositoryRoot = Path.of("").toAbsolutePath();
		Path fileToStage = repositoryRoot.resolve(filePath);

		if (!fileToStage.startsWith(repositoryRoot) || !Files.isRegularFile(fileToStage)) {
			throw new IOException("no" + filePath);
		}

		String relativePath = repositoryRoot.relativize(fileToStage).toString();
		String fileHash = createBlob(fileToStage.toString());
		Path index = Path.of("git", "index");
		Files.createDirectories(index.getParent());
		List<String> entries = new ArrayList<>();

		if (Files.exists(index)) {
			for (String entry : Files.readAllLines(index)) {
				int space = entry.indexOf(' ');
				if (space >= 0 && !entry.substring(space + 1).equals(relativePath)) {
					entries.add(entry);
				}
			}
		}

		entries.add(fileHash + " " + relativePath);
		Files.writeString(index, String.join("\n", entries));
	}

	public static String hashFile(String sourceFilePath) throws IOException {
		Path path = Path.of(sourceFilePath);
		if (!Files.isRegularFile(path)) {
			throw new IOException("No such file: " + sourceFilePath);
		}

		byte[] fileBytes = Files.readAllBytes(path);

		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 is unavailable", e);
		}

		byte[] hash = digest.digest(fileBytes);
		return HexFormat.of().formatHex(hash);
	}

	public static void initializeRepository() throws IOException {
		Path git = Paths.get("git");
		Path objects = git.resolve("objects");
		Path index = git.resolve("index");
		Path HEAD = git.resolve("HEAD");
		boolean exists = Files.isDirectory(git) && Files.isDirectory(objects)
				&& Files.isRegularFile(index) && Files.isRegularFile(HEAD);

		Files.createDirectories(objects);

		if (Files.notExists(index)) {
			Files.createFile(index);
		}
		if (Files.notExists(HEAD)) {
			Files.createFile(HEAD);
		}

		if (exists) {
			System.out.println("Git Repository Already Exists");
		} else {
			System.out.println("Git Repository Created");
		}
	}

	public static void main(String[] args) throws IOException {
		initializeRepository();
		try {
			String hashedFile = hashFile("Hello.txt");
			System.out.println(hashedFile);
			stageFile("test.txt");
			stageFile("copy.txt");
			System.out.println("Staged: " + "test.txt");
		} catch (IOException e) {
			System.out.println("File error: " + e.getMessage());
		}
	}
}

