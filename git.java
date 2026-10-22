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
	public static String createBlob(String filePath) throws IOException {
		String hash = hashFile(filePath);

		Path objects = Path.of("git", "objects");
		Files.createDirectories(objects);

		Path blob = objects.resolve(hash);
		if (Files.notExists(blob)) {
			Files.copy(Path.of(filePath), blob);
		}

		return hash;
	}

	public static void stageFile(String filePath) throws IOException {
		Path root = Path.of("").toAbsolutePath();
		Path file = root.resolve(filePath);

		if (!file.startsWith(root) || !Files.isRegularFile(file)) {
			throw new IOException("no" + filePath);
		}

		String relativePath = root.relativize(file).toString();
		String hash = createBlob(file.toString());
		Path index = Path.of("git", "index");
		Files.createDirectories(index.getParent());
		List<String> lines = new ArrayList<>();

		if (Files.exists(index)) {
			for (String line : Files.readAllLines(index)) {
				int space = line.indexOf(' ');
				if (space >= 0) {
					String existingHash = line.substring(0, space);
					String existingPath = line.substring(space + 1);
					if (existingPath.equals(relativePath) && existingHash.equals(hash)) {
						return;
					}
					if (!(existingPath.equals(relativePath))) {
						lines.add(line);
					}
				}
				// if (space >= 0 && !line.substring(space + 1).equals(relativePath)) {
				// lines.add(line);
				// }
			}
		}

		lines.add(hash + " " + relativePath);
		Files.writeString(index, String.join("\n", lines));
	}

	public static String hashFile(String filePath) throws IOException {
		Path path = Path.of(filePath);
		if (!Files.isRegularFile(path)) {
			throw new IOException("No such file: " + filePath);
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

	public static void init() throws IOException {
		Path git = Paths.get("git");
		Path objects = git.resolve("objects");
		Path index = git.resolve("index");
		Path HEAD = git.resolve("HEAD");
		boolean exists = Files.isDirectory(git) && Files.isDirectory(objects) && Files.isRegularFile(index)
				&& Files.isRegularFile(HEAD);

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

	public static String createTree(List<String> workingList, String dirPath) throws IOException {

		StringBuilder treeContent = new StringBuilder("");

		for (String line : workingList) {

			int firstSpace = line.indexOf(' ');
			int secondSpace = line.indexOf(' ', firstSpace + 1);
			String typeAndHash = line.substring(0, secondSpace);
			String path = line.substring(secondSpace + 1);

			int lastSlash = -1;
			for (int i = 0; i < path.length(); i++) {
				if (path.charAt(i) == '/') {
					lastSlash = i;
				}
			}

			String directory = "";
			String name = path;

			if (lastSlash >= 0) {
				directory = path.substring(0, lastSlash);
				name = path.substring(lastSlash + 1);
			}

			if (directory.equals(dirPath)) {
				treeContent.append(typeAndHash + " " + name + "\n");
			}
		}

		byte[] treeBytes = treeContent.toString().getBytes();
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 is unavailable", e);
		}
		byte[] hash = digest.digest(treeBytes);
		String treeHash = HexFormat.of().formatHex(hash);

		Path objects = Path.of("git", "objects");
		Files.createDirectories(objects);

		Path tree = objects.resolve(treeHash);
		if (Files.notExists(tree)) {
			Files.write(tree, treeBytes);
		}

		return treeHash;
	}

	public static void main(String[] args) throws IOException {
		init();
		try {
			String hashedFile = hashFile("Hello.txt");
			System.out.println(hashedFile);
			stageFile("test.txt");
			stageFile("copy.txt");
			// System.out.println("Staged: " + "test.txt");
			List<String> workingList = new ArrayList<>();
			for(String line : Files.readAllLines(Path.of("git", "index"))){
				workingList.add("blob " + line);
			}
			System.out.println(createTree(workingList, ""));
			System.out.println(createTree(workingList, ""));
		} catch (IOException e) {
			System.out.println("File error: " + e.getMessage());
		}
	}
}
