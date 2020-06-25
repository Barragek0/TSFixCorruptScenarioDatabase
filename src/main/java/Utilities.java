import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Utilities
{

	static double round(double value, int places)
	{
		if (places < 0)
		{
			throw new IllegalArgumentException();
		}

		BigDecimal bd = BigDecimal.valueOf(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	static void resetScenarioCache()
	{
		if (new File(Main.contentDirectory + "/RVDBCache.bin").exists())
		{
			new File(Main.contentDirectory + "/RVDBCache.bin").delete();
		}
		if (new File(Main.contentDirectory + "/RVDBCache.bin.MD5").exists())
		{
			new File(Main.contentDirectory + "/RVDBCache.bin.MD5").delete();
		}
		if (new File(Main.contentDirectory + "/SDBCache.bin").exists())
		{
			new File(Main.contentDirectory + "/SDBCache.bin").delete();
		}
		if (new File(Main.contentDirectory + "/SDBCache.bin.MD5").exists())
		{
			new File(Main.contentDirectory + "/SDBCache.bin.MD5").delete();
		}
	}

	static boolean scenarioCacheComplete() throws InterruptedException
	{
		if (new File(Main.contentDirectory + "/RVDBCache.bin").exists() &&
			new File(Main.contentDirectory + "/RVDBCache.bin.MD5").exists())
		{
			return true;
		}
		return false;
	}

	static boolean scenarioCachePartiallyComplete() throws InterruptedException
	{
		if (new File(Main.contentDirectory + "/SDBCache.bin").exists() && !scenarioCacheComplete())
		{
			return true;
		}
		return false;
	}

	static void closeLogMate()
	{
		try
		{
			Runtime.getRuntime().exec("taskkill /F /IM LogMate.exe");
			Thread.sleep(500);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}

	static boolean checkTrainSimRunning() throws IOException
	{
		String line;
		String pidInfo = "";
		Process p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe");
		BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((line = input.readLine()) != null)
		{
			pidInfo += line;
		}
		input.close();
		if (pidInfo.contains(Variables.is64bit ? Variables.x64exe : Variables.x64exe))
		{
			return true;
		}
		return false;
	}


	static File[] FindFileTypes(File input) throws IOException
	{

		try
		{
			boolean recursive = true;

			ArrayList<File> matching = new ArrayList<File>();

			SuffixFileFilter extFilter = new SuffixFileFilter("");

			Main.progressBar.updateStatus("Creating scenario database...");

			List<File> files = listFiles(input.getAbsolutePath());

			for (Iterator<File> iterator = files.iterator(); iterator.hasNext(); )
			{
				File file = (File) iterator.next();
				String name = file.getName();
				if (name.equalsIgnoreCase("ScenarioProperties.xml"))
				{
					matching.add(file);
					//Main.log.info("[FileIterator] Added -> " + file.getAbsolutePath());
				}
			}
			File[] matchingFiles = matching.toArray(new File[matching.size()]);
			if (matchingFiles != null)
			{
				return matchingFiles;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public static String replaceLast(String text, String regex, String replacement) {
		return text.replaceFirst("(?s)"+regex+"(?!.*?"+regex+")", replacement);
	}


	public static <T> ArrayList<T> removeDuplicates(ArrayList<T> list)
	{
		ArrayList<T> newList = new ArrayList<T>();

		for (T element : list) {

			if (!newList.contains(element)) {

				newList.add(element);
			}
		}

		return newList;
	}

	static File[] FindScenarioFolders() throws IOException
	{

		try
		{
			boolean recursive = true;

			ArrayList<File> matching = new ArrayList<File>();

			SuffixFileFilter extFilter = new SuffixFileFilter("");

			Main.progressBar.updateStatus("Creating folder collection...");

			List<File> files = listFiles(Main.routesDirectory.getAbsolutePath());

			for (Iterator<File> iterator = files.iterator(); iterator.hasNext(); )
			{
				File file = (File) iterator.next();
				String name = file.getName();
				if (name.equalsIgnoreCase("Scenarios"))
				{
					matching.add(file);
					//Main.log.info("[FileIterator] Added -> " + file.getAbsolutePath());
				}
			}
			File[] matchingFiles = matching.toArray(new File[matching.size()]);
			if (matchingFiles != null)
			{
				return matchingFiles;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	static List<File> listFiles(String directoryName)
	{
		File directory = new File(directoryName);

		List<File> resultList = new ArrayList<File>();

		File[] fList = directory.listFiles();
		resultList.addAll(Arrays.asList(fList));
		for (File file : fList)
		{
			if (file.isDirectory())
			{
				resultList.addAll(listFiles(file.getAbsolutePath()));
			}
		}
		return resultList;
	}

	static void setFrameSize(int width, int height)
	{ // frame.setSize is unreliable
		SwingUtilities.invokeLater(() -> {
			Main.frame.setPreferredSize(new Dimension(width, height));
			Main.frame.setMaximumSize(new Dimension(width, height));
			Main.frame.setMinimumSize(new Dimension(width, height));
			Main.frame.pack();
		});
	}

	static void copyFolder(Path src, Path dest) throws IOException
	{
		try (Stream<Path> stream = Files.walk(src))
		{
			stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
		}
	}

	static void copy(Path source, Path dest)
	{
		try
		{
			File toDelete = new File(dest.toUri());
			if (toDelete.exists())
			{
				deleteDirectory(toDelete);
			}
			toDelete.mkdirs();
			Files.copy(source, dest, REPLACE_EXISTING);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	static boolean deleteDirectory(File path)
	{
		if (path.exists())
		{
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				if (files[i].isDirectory())
				{
					deleteDirectory(files[i]);
				}
				else
				{
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	static boolean move(File sourceFile, File destFile) throws IOException
	{
		destFile.mkdirs();
		Files.move(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		Main.log.info("Successfully moved " + sourceFile + " to " + destFile);
		return true;

	}

	static long calculateTimeRemaining(long startTime, long currentTime)
	{
		try
		{
			long result = 0;
			result = result + TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime);
			long value = (long) (((double) result / (double) Main.progressBar.progressBar.getValue()) *
				(Main.progressBar.progressBar.getMaximum() - Main.progressBar.progressBar.getValue()));
			value = value * 1000;
			return value;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			return 0;
		}

	}

	static void browseForBaseDirectory(boolean resize)
	{
		if (resize)
		{
			Utilities.setFrameSize(Main.frame.getWidth(), Main.frame.getHeight() - 75);
		}
		Main.progressBar.updateStatus("Please direct the program to your RailWorks folder.");
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.open();
		shell.setVisible(false);
		DirectoryDialog dialog = new DirectoryDialog(shell);
		dialog.setFilterPath("C:\\");
		String result = dialog.open();
		while (result == null)
		{
			browseForBaseDirectory(false);
		}
		Main.baseDirectory = new File(result);
		display.dispose();
		shell.dispose();
	}

	static void moveFilesToRoutesBackup() throws InterruptedException, IOException
	{
		try
		{
			if (Utilities.move(Main.routesDirectory, Main.routesBackupDirectory))
			{
				Main.log.info("Moved routes directory successfully");
			}
		}
		catch (Throwable e)
		{
			Main.log.info("Couldn't move routes directory");

			if (e instanceof DirectoryNotEmptyException)
			{
				if (Variables.isFirstRun)
				{
					Variables.isFirstRun = false;
					Runtime.getRuntime().exec("explorer.exe /select," + Main.routesBackupDirectory);
					Runtime.getRuntime().exec("explorer.exe /select," + Main.routesDirectory);
					Utilities.setFrameSize(600, 275);
				}
				Main.progressBar.updateStatus("Error: Couldn't move routes directory (Not Empty)<br>" +
					"This sometimes happens if the program crashed when it previously ran.<br><br>" +
					"Please move all of the files from here: <br><br> " + Main.routesBackupDirectory + "<br><br>" +
					"to here: <br><br>" +
					Main.routesDirectory + "<br><br>" +
					"We've opened both of these directories for you.<br><br>" +
					"The program will continue running when you've done this.");
			}
			else
			{
				Main.progressBar.updateStatus("Error: Couldn't move routes directory (Access Denied)<br><br>" +
					"Please close all Windows Explorer windows and any other things that could be interacting with your Railworks folder. EG Notepad.<br><br>" +
					"The program will continue running when all instances have been closed.");
			}
			Thread.sleep(1000);
			moveFilesToRoutesBackup();
		}
	}

	static StringBuilder parseFile(File file, String searchStr) throws FileNotFoundException
	{
		Scanner scan = new Scanner(file);
		StringBuilder stringBuilder = new StringBuilder();
		while (scan.hasNext())
		{
			String line = scan.nextLine().toLowerCase().toString();
			if (line.contains(searchStr))
			{
				stringBuilder.append("    " + line + System.getProperty("line.seperator"));
			}
		}
		return stringBuilder;
	}

	static void moveFileToOriginalRoutes() throws InterruptedException
	{
		try
		{
			if (Utilities.move(Main.routesBackupDirectory, Main.routesDirectory))
			{
				Main.log.info("Moved routes directory to original successfully");
			}
			else
			{
				Main.log.info("Couldn't move routes directory to original");
				Main.progressBar.updateStatus("Error: Couldn't sort routes directory to original path.<br><br>" +
					"Please close all Windows Explorer windows and any other things that could be interacting with your Railworks folder. EG Notepad.<br><br>" +
					"The program will continue running when all instances have been closed.");
				Thread.sleep(1000);
				moveFileToOriginalRoutes();
			}
		}
		catch (Throwable e)
		{

		}
	}


}
