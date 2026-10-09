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
	public static String createBlob(String filePath) throws IOException 
	{
    	String hash = hashFile(filePath);

		Path objects = Path.of("git", "objects");
    	Files.createDirectories(objects);

    	Path blob = objects.resolve(hash);
    	if (Files.notExists(blob)) {
        	Files.copy(Path.of(filePath), blob);
    	}

   		return hash;
	}
	public static void stageFile(String filePath) throws IOException 
	{
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
        		if (space >= 0 && !line.substring(space + 1).equals(relativePath)) {
                	lines.add(line);
            	}
        	}
 		}

    	lines.add(hash + " " + relativePath);
    	Files.writeString(index, String.join("\n", lines));
	}
	public static String hashFile(String filePath) throws IOException 
	{
    	Path path = Path.of(filePath);
		if (!Files.isRegularFile(path)) {
			throw new IOException("No such file: " + filePath);
		}

    	byte[] fileBytes = Files.readAllBytes(path);

    	MessageDigest digest;
    	try {
    	    digest = MessageDigest.getInstance("SHA-1");
    	} 
		catch (NoSuchAlgorithmException e) 
		{
       		throw new IllegalStateException("SHA-1 is unavailable", e);
    	}

    	byte[] hash = digest.digest(fileBytes);
    	return HexFormat.of().formatHex(hash);
	}
	
	public static void init() throws IOException
	{
			Path git = Paths.get("git");
			Path objects = git.resolve("objects");
			Path index = git.resolve("index");
			Path HEAD = git.resolve("HEAD");
			boolean exists = Files.isDirectory(git) && Files.isDirectory(objects) && Files.isRegularFile(index) && Files.isRegularFile(HEAD);
			
			Files.createDirectories(objects);

			if(Files.notExists(index)){
			Files.createFile(index);
			}
			if(Files.notExists(HEAD)){
					Files.createFile(HEAD);
					}
			
			if(exists)
				{
			System.out.println("Git Repository Already Exists");
			}
			else
			{
				System.out.println("Git Repository Created");
			}
	}
	public static void main(String[] args) throws IOException 
	{
		init();
		try 
		{
    		String hashedFile = hashFile("Hello.txt");
    		System.out.println(hashedFile);
			stageFile("test.txt");
        	System.out.println("Staged: " + "test.txt");
		}
		catch (IOException e) 
		{
    		System.out.println("File error: " + e.getMessage());
		}
	}
}

