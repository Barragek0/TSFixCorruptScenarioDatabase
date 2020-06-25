import com.sun.corba.se.spi.orbutil.threadpool.Work;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.listFiles;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Main
{
	public static File baseDirectory = new File("C:\\Program Files (x86)\\Steam\\steamapps\\common\\RailWorks/");
	public static File contentDirectory = new File(baseDirectory + "\\Content\\");
	public static File routesDirectory = new File(contentDirectory.getPath() + "\\Routes\\");
	public static File routesBackupDirectory = new File(System.getProperty("user.home") + "\\TSFixCorruptedScenarioDatabase\\Backups\\Routes/");
	public static File backupDirectory = new File(System.getProperty("user.home") + "\\TSFixCorruptedScenarioDatabase\\Backups/");
	public static File corruptRoutesDirectory = new File(System.getProperty("user.home") + "\\TSFixCorruptedScenarioDatabase\\Corrupt/");
	public static File workshopBackupDirectory = new File(System.getProperty("user.home") + "\\TSFixCorruptedScenarioDatabase\\Workshop/");
	public static File crashDumpDirectory = new File(baseDirectory.getPath() + "\\TempDump\\Logs\\");


	static List<WorkshopFile> removing = new ArrayList<>();
	static Timer timer = new Timer();
	static boolean timerStarted;
	static boolean canMoveOn;

	private static List<File> corruptFiles = new ArrayList<>(),
		potentiallyCorruptedRoutes = new ArrayList<>(),
		corruptRoutesGroup = new ArrayList<>(),
		routesWithCorruptScenarios = new ArrayList<>(),
		corruptScenarios = new ArrayList<>();

	static ArrayList<WorkshopFile> toRemove = new ArrayList<WorkshopFile>();

	private static List<WorkshopFile> workshopNamesToCompare = new ArrayList<>(), workshopNames = new ArrayList<>();

	final static SwingProgressBar progressBar = new SwingProgressBar();

	public static JFrame frame = new JFrame("TSFixCorruptedScenarioDatabase");

	public static File[] routes;

	public static Runnable runnable;

	public static File f;

	public static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

	static int finalThreadCount;

	static int threadsComplete = 0;

	public static void main(String[] argv) throws IOException, InterruptedException
	{
		BasicConfigurator.configure();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try
			{
				Utilities.moveFileToOriginalRoutes();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}));

		LookAndFeel.setLookAndFeel("Nimbus");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(progressBar);
		frame.setUndecorated(true);
		frame.setResizable(true);
		Utilities.setFrameSize(600, 75);
		frame.setVisible(true);
		frame.setAlwaysOnTop(true);
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				int result = JOptionPane.showConfirmDialog(frame, "Are you sure you want to exit the application?\r\n\r\nExiting before it has finished may cause bugs in the program and isn't recommended.\r\n\r\nIf you do encounter issues the next time you run the program, follow the steps on the GitHub page.");
				if (result == JOptionPane.OK_OPTION)
				{
					try
					{
						Utilities.moveFileToOriginalRoutes();
					}
					catch (Throwable e1)
					{
						e1.printStackTrace();
					}
					System.exit(0);
				}
			}
		});
		progressBar.setProgressBarIndeterminate(true);

		progressBar.updateStatus("Scraping scenario names & ids from steam workshop...");
		WorkshopScraper.start();
		File workshopFile = new File(baseDirectory + "/workshop.txt");
		Scanner s = new Scanner(workshopFile);
		while (s.hasNextLine())
		{
			String current = s.nextLine();
			current = current.split("\\|\\|")[0];
			current = Utilities.replaceLast(current, " ", "");
			workshopNames.add(new WorkshopFile(current, null));
			log.info("-> " + current);
		}
		s.close();

		if (!Variables.testing)
		{
			int dialogResult = JOptionPane.showConfirmDialog(frame, "Is your RailWorks folder located here?:\r\n\r\n" + baseDirectory + "\r\n\r\n", "", JOptionPane.YES_NO_OPTION);
			if (dialogResult == JOptionPane.NO_OPTION)
			{
				Utilities.setFrameSize(frame.getWidth(), frame.getHeight() + 75);
				progressBar.updateStatus("Hold on a second, the program is looking for your install directory...<br><br>Your computer may freeze for a few seconds...<br><br>If this takes more than a few seconds, you'll be able to direct the program to it.");

				Thread findFile = new Thread(() -> f = FindFile.find(Variables.x86exe, new File("C:/")));
				findFile.start();

				int secondsWaited = 0;
				while (f == null)
				{
					Thread.sleep(1000);
					secondsWaited++;
					if (secondsWaited >= 20)
					{
						continue;
					}
				}

				if (f != null)
				{
					f = new File(f.getPath().replace("RailWorks.exe", ""));
					dialogResult = JOptionPane.showConfirmDialog(frame, "Is this your RailWorks folder?:\r\n\r\n" + f.getPath() + "\r\n\r\n", "", JOptionPane.YES_NO_OPTION);
					if (dialogResult == JOptionPane.NO_OPTION)
					{
						Utilities.browseForBaseDirectory(true);
						log.info("Finished browsing.");
					}
					else
					{
						Utilities.setFrameSize(frame.getWidth(), frame.getHeight() - 50);
					}
				}
				else
				{
					Utilities.browseForBaseDirectory(true);
					log.info("Finished browsing.");
				}
			}

			log.info("Directory is set.");

			File[] directoryListing = Utilities.FindFileTypes(contentDirectory);
			progressBar.resetProgressBar();

			checkForScenarioEditorCorruption(directoryListing);

			Utilities.resetScenarioCache();
			if (databaseNowWorksCorrectly(false))
			{
				Utilities.setFrameSize(frame.getWidth(), frame.getHeight() + 25);
				log.info("Database is working correctly.");
				progressBar.updateStatus("Issue resolved, the database will now load correctly.<br><br>The program will close in 30 seconds...");
				Thread.sleep(30000);
				System.exit(0);
			}
			else
			{
				log.info("Database not working correctly.");
			}
		}
		FindAndRemoveCorruptScenarios();
		if (databaseNowWorksCorrectly(true))
		{
			progressBar.updateStatus("Issue resolved, the database will now load correctly.<br><br>The program will close in 30 seconds...");
			Thread.sleep(30000);
			System.exit(0);
		}
		log.info("Finished!");
	}

	private static void checkForScenarioEditorCorruption(File[] directoryListing) throws InterruptedException, IOException
	{
		progressBar.updateStatus("Checking all scenario files for corruption...");
		for (File file : directoryListing)
		{
			try
			{
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(file);
				doc.getDocumentElement().normalize();
				if (doc.hasChildNodes())
				{
					processNode(doc.getChildNodes(), file, 0, LookForNode.REQUIRED_SET);
					processNode(doc.getChildNodes(), file, 0, LookForNode.DISPLAY_NAME);
					processNode(doc.getChildNodes(), file, 0, LookForNode.SCENARIO_ID);
				}
			}
			catch (Throwable e)
			{
				log.error("Error reading file: " + file.getAbsolutePath());
				e.printStackTrace();
			}
		}

		workshopNames = workshopNames.stream()
			.distinct()
			.collect(Collectors.toList());
		workshopNamesToCompare = workshopNamesToCompare.stream()
			.distinct()
			.collect(Collectors.toList());
		AtomicInteger threads = new AtomicInteger();

		Thread.sleep(500);
		startSearchingWorkshop(threads);

		corruptFiles = corruptFiles.stream()
			.distinct()
			.collect(Collectors.toList());
		int size = corruptFiles.size();
		log.info(size == 0 ? "No corrupt files were found." : "Found " + size + " potentially corrupt files.");
		progressBar.progressBar.setMaximum(size);
		log.info("Backing up the potentially corrupt files.");
		if (size == 0)
		{
			progressBar.updateStatus("No corrupt files were found... Wait a moment for the second phase to start...");
			progressBar.setProgressBarIndeterminate(true);
			Thread.sleep(5000);
		}
		if (size != 0)
		{
			progressBar.updateStatus("Backing up " + size + " potentially corrupted files...");
			Thread.sleep(3000);
			for (File file : corruptFiles)
			{
				String absolutePath = file.getAbsolutePath();
				int startIndex = absolutePath.indexOf("Scenarios");
				int startIndexAdditional = 10;
				startIndex = startIndex + startIndexAdditional;
				int endIndex = absolutePath.indexOf("ScenarioProperties");
				File userHome = new File(backupDirectory + (absolutePath.substring(startIndex, endIndex)));
				if (!userHome.exists())
				{
					userHome.mkdirs();
				}
				Utilities.copyFolder(Paths.get(file.getPath().replace("ScenarioProperties.xml", "")), Paths.get(userHome.getAbsolutePath()));
				progressBar.appendBar();
			}
			progressBar.updateStatus("Your files have been backed up and can be found in your user directory<br>: " + backupDirectory);
			Thread.sleep(10000);
			log.info("Files have been backed up and can be found in the following directory: " + backupDirectory);
			log.info("Deleting the potentially corrupt scenarios...");
			for (File file : corruptFiles)
			{
				file = new File(file.getPath().replace("ScenarioProperties.xml", ""));
				deleteDirectory(file);
				log.info("Deleted " + file.getPath());
			}
		}
	}

	static void startSearchingWorkshop(AtomicInteger threads) throws InterruptedException
	{
		try
		{
			Variables.startingSize = 20;
			progressBar.progressBar.setMaximum(Variables.startingSize);
			progressBar.progressBar.setValue(0);
			progressBar.setProgressBarIndeterminate(false);
			progressBar.updateStatus("Searching for and moving all workshop scenarios...<br><br>");
			for (int i = 0; i < 20; i++)
			{ // loop it many times to make sure it finds every scenario
				Iterator workshopIterator = workshopNamesToCompare.iterator();
				while (workshopIterator.hasNext())
				{
					WorkshopFile workshopFile = (WorkshopFile) workshopIterator.next();
					try
					{
						if (!toRemove.contains(workshopFile) && workshopNames.contains(workshopFile))
						{
							log.error("Both lists contain: " + workshopFile.name + " (" + workshopFile.file + ")");
							workshopFile.file = new File(workshopFile.file.getPath().replace("ScenarioProperties.xml", ""));
							toRemove.add(workshopFile);
						}
					}
					catch (Throwable e)
					{
						if (e instanceof ArrayIndexOutOfBoundsException)
						{
							startSearchingWorkshop(threads);
						}
						else
						{
							e.printStackTrace();
						}
					}
				}
				new File(corruptRoutesDirectory + "/").mkdirs();

				toRemove = (ArrayList<WorkshopFile>) toRemove.stream()
					.distinct()
					.collect(Collectors.toList());

				for (WorkshopFile f : toRemove)
				{
					Utilities.move(f.file, new File(corruptRoutesDirectory + "/" + f.file.getName() + "/"));
				}
				progressBar.appendBar();
				Thread.sleep(200);
			}
			canMoveOn = true;
		}
		catch (Throwable e)
		{
			if (e instanceof ConcurrentModificationException)
			{
				startSearchingWorkshop(threads);
				//e.printStackTrace();
			}
			else
			{
				startSearchingWorkshop(threads);
				//e.printStackTrace();
			}
		}
	}

	private static boolean databaseNowWorksCorrectly(boolean includeMainStatus) throws IOException, InterruptedException
	{
		progressBar.setProgressBarIndeterminate(true);
		progressBar.updateStatus("Train Sim will restart multiple times to verify the database is working correctly.<br><br>This may take a while...<br><br>");
		//check train sim loads correctly 3 times in a row to be certain the database is ok
		if (!includeMainStatus)
		{
			Utilities.setFrameSize(frame.getWidth(), 125);
		}
		if (startSimAndWaitForCrash(includeMainStatus))
		{
			progressBar.updateStatus("Train Sim crashed (attempt 1), moving onto the next resolution step.<br><br>Please wait a moment...");
		}
		else
		{
			progressBar.updateStatus("Train Sim will restart multiple times to verify the database is working correctly.<br><br>This may take a while...<br><br>");
			progressBar.appendBar(1);
			Utilities.resetScenarioCache(); // make sure it wasn't a fluke (there's a ~1 in 10 chance the game will start up even with a corrupt database)
			if (startSimAndWaitForCrash(includeMainStatus))
			{
				progressBar.updateStatus("Train Sim crashed (attempt 2), moving onto the next resolution step.<br><br>Please wait a moment...");
			}
			else
			{
				progressBar.updateStatus("Train Sim will restart multiple times to verify the database is working correctly.<br><br>This may take a while...<br><br>");
				progressBar.appendBar(1);
				//let's see if it now works with the database it previously made
				if (startSimAndWaitForCrash(includeMainStatus))
				{
					progressBar.updateStatus("Train Sim crashed (attempt 3), moving onto the next resolution step.<br><br>Please wait a moment...");
				}
				else
				{
					progressBar.updateStatus("Train Sim will restart multiple times to verify the database is working correctly.<br><br>This may take a while...<br><br>");
					progressBar.appendBar(1);
					return true;
				}
			}
		}
		return false;
	}

	private static void FindAndRemoveCorruptScenarios() throws IOException, InterruptedException
	{
		progressBar.setProgressBarIndeterminate(false);
		Utilities.setFrameSize(600, 250);
		startStage(1);
		progressBar.resetProgressBar();
		startStage(2);
		progressBar.resetProgressBar();
		Utilities.moveFileToOriginalRoutes();

		if (routesWithCorruptScenarios.size() > 0)
		{
			startStage(3);
			try
			{
				((Thread) runnable).interrupt();
			}
			catch (Throwable e)
			{
				log.error("Couldn't cast Runnable to Thread.");
			}
			startStage(4);
			progressBar.setProgressBarIndeterminate(true);
			startStage(5);
			if (!corruptRoutesDirectory.exists())
			{
				corruptRoutesDirectory.mkdirs();
			}
			Runtime.getRuntime().exec("explorer.exe /select," + corruptRoutesDirectory);
			progressBar.updateSecondaryStatus("Finished! We've opened the location where you can find the corrupt scenarios.<br><br>" +
				"This program will exit in 30 seconds.<br><br>");
		}
		progressBar.updateSecondaryStatus("Finished! The program could not find any corrupt scenarios. If your game still crashes, refer to my steam workshop guide: Stop the game crashing when 'Updating scenario database' / 'Saving scenario database'.<br>This program will exit in 30 seconds.<br><br>");
	}

	private static void startUpdatingProgressBarText() throws IOException, InterruptedException
	{
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		runnable = () -> {
			Variables.currentTime = System.currentTimeMillis();
			progressBar.updateProgressBarText(progressBar.progressBar.getValue(), Variables.startingSize, Variables.startTime, Variables.currentTime);
		};
		executor.scheduleAtFixedRate(runnable, 0, 50, TimeUnit.MILLISECONDS);
	}

	private static void startStage(int stage) throws InterruptedException, IOException
	{
		Variables.hasCrashed = false;
		Variables.hasNotCrashed = false;
		Variables.startTime = System.currentTimeMillis();
		// all routes and scenarios have to be checked twice as there's roughly a 1 in 10 chance for the program to start cleanly,
		// even if the scenarios it currently has loaded have corruptions
		boolean hasMoved = false;
		boolean closed = false;
		switch (stage)
		{
			case 1:
				List<File> previousFiveAtATime = new ArrayList<>();
				if (!routesDirectory.exists())
				{
					progressBar.updateStatus("Error: Routes directory not found. If you closed the program part way through running before, your routes folder will have been moved to here: " + routesBackupDirectory + "<br><br>Please move the routes folder back to your content folder and run the program again.<br><br>The program will exit in 30 seconds.");
					Thread.sleep(30000);
					System.exit(0);
				}
				Utilities.resetScenarioCache();
				progressBar.setProgressBarIndeterminate(true);
				if (!routesBackupDirectory.exists())
				{
					routesBackupDirectory.mkdirs();
				}
				//Utilities.moveFilesToRoutesBackup();
				Utilities.setFrameSize(600, 250);
				Thread.sleep(1000);
				closed = false;
				while (!closed)
				{ // TODO, bad way to do this, surely there's a better way?
					try
					{
						routes = routesDirectory.listFiles();
						for (File route : routes)
						{
							potentiallyCorruptedRoutes.add(route);
						}
						closed = true;
					}
					catch (Throwable e)
					{
						e.printStackTrace();
						progressBar.updateStatus("Error: Couldn't sort routes directory.<br><br>Please close all Windows Explorer windows and any other things that could be interacting with your Railworks folder. EG Notepad.<br><br>The program will continue running when all instances have been closed.");
						Thread.sleep(1000);
					}
				}
				hasMoved = false;
				Variables.startingSize = potentiallyCorruptedRoutes.size() * 2;
				Variables.hasCrashed = false;
				Variables.hasNotCrashed = false;
				List<File> currentlyCheckingRoutes = new ArrayList<File>();
				int triedToMoveOn = 0;
				Variables.firstRun = true;
				int totalChecked = 0;
				boolean ranIntoError = false;
				List<File> fiveAtATime = new ArrayList<>();
				boolean needsToMoveFiles = false;
				for (int i = 0; i < potentiallyCorruptedRoutes.size() * 2; i += 5)
				{
					log.info("Routes left: {} - Current: {}", potentiallyCorruptedRoutes.size() * 2, i);
					Variables.currentlyChecking = 5;
					int addit = 5;
					int calc = totalChecked + addit;
					if (calc > potentiallyCorruptedRoutes.size() * 2)
					{
						int newCurrentlyChecking = (potentiallyCorruptedRoutes.size() * 2) - totalChecked;
						Variables.currentlyChecking = newCurrentlyChecking;
					}
					int condition = (potentiallyCorruptedRoutes.size() - 5);
					if (condition < 0)
					{
						Variables.currentlyChecking = potentiallyCorruptedRoutes.size();
					}
					if (Variables.currentlyChecking > 0)
					{
						try
						{
							//add 5 routes at a time to get a vague idea of corrupt routes
							//this speeds up the next stage significantly for players with a lot of routes
							//can also add this to scenarios, havent done it yet

							Utilities.closeLogMate();

							// TODO - really need to re-do this entire thing, horrible mess

							int add1 = 1;
							int add2 = 2;
							int add3 = 3;
							int add4 = 4;
							int route2 = totalChecked + 1;
							int route3 = totalChecked + 2;
							int route4 = totalChecked + 3;
							int route5 = totalChecked + 4;
							fiveAtATime.clear();
							fiveAtATime.add(potentiallyCorruptedRoutes.get(totalChecked));
							fiveAtATime.add(potentiallyCorruptedRoutes.get(route2));
							fiveAtATime.add(potentiallyCorruptedRoutes.get(route3));
							fiveAtATime.add(potentiallyCorruptedRoutes.get(route4));
							fiveAtATime.add(potentiallyCorruptedRoutes.get(route5));
							log.info("Total checked: {} {} {} {} {}", totalChecked, route2, route3, route4, route5);
							if (!hasMoved || Variables.firstRun)
							{
								if (!Variables.firstRun)
								{
									for (File current : fiveAtATime)
									{
										String folderName = current.getName();
										new File(routesDirectory + "/" + folderName + "/").mkdirs();
										new File(routesBackupDirectory + "/" + folderName + "/").mkdirs();
										Utilities.move(new File(routesDirectory + "/" + folderName + "/"), new File(routesBackupDirectory + "/" + folderName + "/"));
									}
									for (int i2 = 0; i2 < previousFiveAtATime.size(); i2++)
									{
										String folderName = previousFiveAtATime.get(i2).getName();
										new File(routesDirectory + "/" + folderName + "/").mkdirs();
										new File(routesBackupDirectory + "/" + folderName + "/").mkdirs();
										Utilities.move(new File(routesBackupDirectory + "/" + folderName + "/"), new File(routesDirectory + "/" + folderName + "/"));
									}
									previousFiveAtATime.clear();
									previousFiveAtATime.add(potentiallyCorruptedRoutes.get(totalChecked));
									previousFiveAtATime.add(potentiallyCorruptedRoutes.get(route2));
									previousFiveAtATime.add(potentiallyCorruptedRoutes.get(route3));
									previousFiveAtATime.add(potentiallyCorruptedRoutes.get(route4));
									previousFiveAtATime.add(potentiallyCorruptedRoutes.get(route5));
									needsToMoveFiles = false;
								}
								else
								{
									for (File current : fiveAtATime)
									{
										String folderName = current.getName();
										Utilities.move(new File(routesDirectory + "/" + folderName + "/"), new File(routesBackupDirectory + "/" + folderName + "/"));
										if (Variables.firstRun)
										{
											previousFiveAtATime.add(potentiallyCorruptedRoutes.get(totalChecked));
											previousFiveAtATime.add(potentiallyCorruptedRoutes.get(route2));
											previousFiveAtATime.add(potentiallyCorruptedRoutes.get(route3));
											previousFiveAtATime.add(potentiallyCorruptedRoutes.get(route4));
											previousFiveAtATime.add(potentiallyCorruptedRoutes.get(route5));
											Variables.firstRun = false;
										}
									}
								}
								hasMoved = true;
							}

							progressBar.updateStatus(Variables.mainStatus + "Loading Train Simulator...<br><br>" + progressBar.secondaryStatus);
							progressBar.updateSecondaryStatus("Stage 1 of 5 - Current progress (" + progressBar.progressBar.getValue() + "/" + Variables.startingSize + "):");
							progressBar.progressBar.setMaximum(Variables.startingSize);
							progressBar.updateStatus(Variables.mainStatus + "Starting Train Simulator...<br><br>" + progressBar.secondaryStatus);
							progressBar.setProgressBarIndeterminate(false);

							Utilities.resetScenarioCache();
							startSimAndWaitForCrash(true);

							if (Variables.tries == 1)
							{
								progressBar.appendBar(Variables.currentlyChecking);
							}
							else if (Variables.hasNotCrashed && Variables.tries >= 2)
							{
								log.info("Routes OK -> " + Arrays.asList(fiveAtATime));
								/*for (File current : fiveAtATime)
								{
									String folderName = current.getName();
									Utilities.move(new File(routesDirectory + "/" + folderName), new File(routesBackupDirectory + "/" + folderName + "/"));
									potentiallyCorruptedRoutes.remove(current);
								}*/
								progressBar.appendBar(Variables.currentlyChecking);
								hasMoved = false;
								totalChecked += 5;
							}
							else if (Variables.hasCrashed && Variables.tries >= 2)
							{
								log.error("Routes may be corrupt -> " + Arrays.asList(fiveAtATime));
								for (File current : fiveAtATime)
								{
									String folderName = current.getName();
									Utilities.move(new File(routesDirectory + "/" + folderName), new File(routesBackupDirectory + "/" + folderName));
									corruptRoutesGroup.add(current);
								}
								progressBar.appendBar(Variables.currentlyChecking);
								hasMoved = false;
								totalChecked += 5;
							}

							if (Variables.tries >= 2)
							{
								Variables.tries = 0;
							}
						}
						catch (Throwable e)
						{
							e.printStackTrace();
							log.info("Reached end of loop");
							/*for (File current : previousFiveAtATime)
							{
								String folderName = current.getName();
								new File(routesDirectory + "/" + folderName + "/").mkdirs();
								new File(routesBackupDirectory + "/" + folderName + "/").mkdirs();
								Utilities.move(new File(routesBackupDirectory + "/" + folderName + "/"), new File(routesDirectory + "/" + folderName + "/"));
							}*/
							ranIntoError = true;
						}
					}
				}
				if (!ranIntoError)
				{
					for (File current : previousFiveAtATime)
					{
						String folderName = current.getName();
						new File(routesDirectory + "/" + folderName + "/").mkdirs();
						new File(routesBackupDirectory + "/" + folderName + "/").mkdirs();
						Utilities.move(new File(routesBackupDirectory + "/" + folderName + "/"), new File(routesDirectory + "/" + folderName + "/"));
					}
				}

				break;
			case 2:
				if (corruptRoutesGroup.isEmpty())
				{
					System.out.println("Corrupt routes is empty... ending...");
				}
				else
				{
					if (!routesDirectory.exists())
					{
						progressBar.updateStatus("Error: Routes directory not found. If you closed the program part way through running before, your routes folder will have been moved to here: " + routesBackupDirectory + "<br><br>Please move the routes folder back to your content folder and run the program again.<br><br>The program will exit in 30 seconds.");
						Thread.sleep(30000);
						System.exit(0);
					}
					Utilities.resetScenarioCache();
					progressBar.setProgressBarIndeterminate(true);
					if (!routesBackupDirectory.exists())
					{
						routesBackupDirectory.mkdirs();
					}
					Thread.sleep(1000);
					hasMoved = false;
					Variables.startingSize = corruptRoutesGroup.size() * 2;
					progressBar.setProgressBarIndeterminate(false);
					Variables.hasCrashed = false;
					Variables.hasNotCrashed = false;
					for (int i = 0; i < corruptRoutesGroup.size(); i = (Variables.tries >= 2 ? i++ : Variables.hasCrashed || Variables.hasNotCrashed ? i : i++))
					{
						File current;
						current = corruptRoutesGroup.get(i);
						String folderName = current.getName();
						Utilities.closeLogMate();
						if (!hasMoved)
						{
							hasMoved = true;
							Utilities.move(new File(routesDirectory + "/" + folderName), new File(routesBackupDirectory + "/" + folderName));
						}
						if (Variables.tries == 1)
						{
							progressBar.appendBar(1);
						}
						else if (Variables.hasNotCrashed && Variables.tries >= 2)
						{
							log.info("The route is ok -> " + corruptRoutesGroup.get(i).getName());
							Utilities.move(new File(routesBackupDirectory + "/" + folderName), current);
							progressBar.appendBar(1);
							corruptRoutesGroup.remove(i);
							hasMoved = false;
						}
						else if (Variables.hasCrashed && Variables.tries >= 2)
						{
							log.error("The route is corrupt -> " + corruptRoutesGroup.get(i).getName());
							Utilities.move(new File(routesBackupDirectory + "/" + folderName), current);
							routesWithCorruptScenarios.add(corruptRoutesGroup.get(i));
							progressBar.appendBar(1);
							corruptRoutesGroup.remove(i);
							hasMoved = false;
						}

						if (Variables.tries >= 2)
						{
							Variables.tries = 0;
						}
						progressBar.updateStatus(Variables.mainStatus + "Loading Train Simulator...<br><br>" + progressBar.secondaryStatus);
						progressBar.updateSecondaryStatus("Stage 2 of 5 - Current progress (" + progressBar.progressBar.getValue() + "/" + Variables.startingSize + "):");
						progressBar.progressBar.setMaximum(Variables.startingSize);
						progressBar.updateStatus(Variables.mainStatus + "Starting Train Simulator...<br><br>" + progressBar.secondaryStatus);

						Utilities.resetScenarioCache();
						startSimAndWaitForCrash(true);
					}
				}
				break;
			case 3:
				if (routesWithCorruptScenarios.isEmpty())
				{
					System.out.println("Corrupt routes is empty... ending...");
				}
				else
				{
					List<File> directories = new ArrayList<>();
					for (int i = 0; i < routesWithCorruptScenarios.size(); i++)
					{
						List<File> directoriesToAdd = new ArrayList<>(
							Arrays.asList(
								new File(routesDirectory + "\\" + routesWithCorruptScenarios.get(i) + "/").listFiles(File::isDirectory)
							)
						);
						for (File directory : directoriesToAdd)
						{
							directories.add(directory);
						}
					}
					Variables.startingSize = directories.size();
					for (int i = 0; i < directories.size(); i++)
					{
						File current = directories.get(i);
						String scenarioFolderName = current.getName();
						String currentPath = current.getPath();
						String routeFolderName = currentPath.substring(currentPath.indexOf("\\Routes\\") + 8, currentPath.lastIndexOf("\\"));
						Utilities.closeLogMate();
						if (Variables.hasNotCrashed && Variables.tries >= 2)
						{
							log.info("The scenario is ok -> " + routeFolderName + "/" + scenarioFolderName);
							Utilities.move(new File(routesDirectory + "\\" + routeFolderName + "/" + scenarioFolderName), current);
							progressBar.appendBar(1);
						}
						else if (Variables.hasCrashed && Variables.tries >= 2)
						{
							log.error("The scenario is corrupt -> " + routeFolderName + "/" + scenarioFolderName);
							Utilities.move(new File(routesDirectory + "\\" + routeFolderName + "/" + scenarioFolderName), current);
							corruptScenarios.add(directories.get(i));
							progressBar.appendBar(1);
						}
						progressBar.updateStatus(Variables.mainStatus + "Loading Train Simulator...<br><br>" + progressBar.secondaryStatus);
						if (((Variables.tries >= 2 && Variables.hasCrashed) || Variables.firstRun)) // make sure we don't get any errors
						{
							Variables.firstRun = false;
							if (Variables.tries >= 2)
							{
								Variables.tries = 0;
							}
							File newRailWorksRoutesFolder = new File(routesBackupDirectory + "/" + routeFolderName + "/" + scenarioFolderName);
							if (!newRailWorksRoutesFolder.exists())
							{
								newRailWorksRoutesFolder.mkdirs();
							}
							Utilities.move(current, newRailWorksRoutesFolder);
							directories.remove(current);
						}
						Utilities.resetScenarioCache();
						progressBar.updateSecondaryStatus("Stage 3 of 5 - Current progress (" + progressBar.progressBar.getValue() + "/" + Variables.startingSize + "):");
						progressBar.progressBar.setMaximum(Variables.startingSize);
						progressBar.updateStatus(Variables.mainStatus + "Starting Train Simulator...<br><br>" + progressBar.secondaryStatus);

						startSimAndWaitForCrash(true);
					}
				}
				break;
			case 4:
				if (corruptRoutesGroup.isEmpty())
				{
					System.out.println("Corrupt routes is empty... ending...");
				}
				else
				{
					Variables.startingSize = corruptScenarios.size();
					progressBar.updateSecondaryStatus("Stage 4 of 5 - Current progress (" + progressBar.progressBar.getValue() + "/" + Variables.startingSize + "):");
					progressBar.progressBar.setMaximum(Variables.startingSize);
					progressBar.updateStatus(Variables.mainStatus + "Moving corrupt scenarios to " + backupDirectory + "...<br><br>" + progressBar.secondaryStatus);
					Thread.sleep(3000);
					for (File corruptScenario : corruptScenarios)
					{
						String scenarioFolderName = corruptScenario.getName();
						String currentPath = corruptScenario.getPath();
						String routeFolderName = currentPath.substring(currentPath.indexOf("\\Routes\\") + 8, currentPath.lastIndexOf("\\"));
						log.error("The scenario is corrupt (stage 4)-> " + routeFolderName + "/" + scenarioFolderName);
						File moveTo = new File(corruptRoutesDirectory + "/" + routeFolderName + "/" + scenarioFolderName);
						moveTo.mkdirs();
						Utilities.move(new File(routesDirectory + "\\" + routeFolderName + "/" + scenarioFolderName), moveTo);
						progressBar.appendBar(1);
						Thread.sleep(500);
					}
				}
				break;
			case 5:
				progressBar.updateSecondaryStatus("Stage 5 of 5 - Searching log files for errors...");
				progressBar.progressBar.setMaximum(Variables.startingSize);
				File[] files = baseDirectory.listFiles((FilenameFilter) (dir, filename) -> filename.endsWith(".log"));
				if (files.length > 0)
				{
					List<String> completeString = new ArrayList<String>();
					for (int i = 0; i < files.length; i++)
					{
						completeString.add(files[i].getPath() + ": " + Utilities.parseFile(files[i], "Error").toString());
					}
					System.out.println(completeString);
					String message = "The following errors were found in the LogMate log files: " + System.lineSeparator() + System.lineSeparator() + completeString + "" + System.lineSeparator() + System.lineSeparator() + "You'll need to fix these errors to fix your corrupt database.";
					JOptionPane.showMessageDialog(frame, message);
					for (int i = 0; i < files.length; i++)
					{
						files[i].delete();
					}
					DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
					LocalDateTime now = LocalDateTime.now();
					try (PrintWriter out = new PrintWriter(baseDirectory + "/Errors-" + dtf.format(now) + ".log"))
					{
						out.println(message);
					}
				}
				Utilities.resetScenarioCache();
				break;
		}
	}

	private static boolean startSimAndWaitForCrash(boolean includeMainStatus) throws
		InterruptedException, IOException
	{
		ProcessBuilder pb = new ProcessBuilder(baseDirectory + "/" + (Variables.is64bit ? Variables.x64exe : Variables.x86exe), "-LogMate", "-SetLogFilters=all");
		pb.directory(baseDirectory);
		Process p = pb.start();

		while (!Utilities.checkTrainSimRunning())
		{
			if (includeMainStatus)
			{
				progressBar.updateStatus(Variables.mainStatus + "Waiting for Train Simulator to start...<br><br>" + progressBar.secondaryStatus);
			}
			Thread.sleep(1000);
		}
		if (!crashDumpDirectory.exists())
		{
			crashDumpDirectory.mkdirs();
		}
		File[] filesBeforeHand = crashDumpDirectory.listFiles();
		List<File> filesBefore = Arrays.asList(filesBeforeHand);
		Variables.hasNotCrashed = false;
		Variables.hasCrashed = false;
		while (!Variables.hasCrashed && !Variables.hasNotCrashed)
		{
			if (includeMainStatus)
			{
				progressBar.updateStatus(Variables.mainStatus + "Waiting for Train Simulator to successfully start or crash...<br><br>" + progressBar.secondaryStatus);
			}
			if (Utilities.scenarioCacheComplete())
			{
				log.info("Successfully started up");
				p.destroy();
				p.waitFor();
				Variables.hasNotCrashed = true;
				Variables.tries++;
				return false;
			}
			if (Utilities.scenarioCachePartiallyComplete())
			{
				Thread.sleep(10000);
				if (Utilities.scenarioCachePartiallyComplete())
				{
					log.info("Crashed");
					p.destroy();
					p.waitFor();
					Variables.hasCrashed = true;
				}
			}
		}
		return true;
	}

	static int total = 0;

	private static enum LookForNode
	{
		REQUIRED_SET,
		DISPLAY_NAME,
		SCENARIO_ID
	} // while doing it like this is a slight speed hit, seperates the code to make it look a bit cleaner

	private static void processNode(NodeList nodeList, File file, int stage, LookForNode node)
	{
		switch (stage)
		{
			case 0:
				for (int count = 0; count < nodeList.getLength(); count++)
				{
					Node tempNode = nodeList.item(count);
					if (tempNode.getNodeType() == Node.ELEMENT_NODE)
					{
						if (tempNode.getNodeName().equalsIgnoreCase("cScenarioProperties"))
						{
							processNode(tempNode.getChildNodes(), file, 1, node);
						}
					}
				}
				break;
			case 1:
				for (int count = 0; count < nodeList.getLength(); count++)
				{
					Node tempNode = nodeList.item(count);
					if (tempNode.getNodeType() == Node.ELEMENT_NODE)
					{
						switch (node)
						{
							case REQUIRED_SET:
								if (node == LookForNode.REQUIRED_SET && tempNode.getNodeName().equalsIgnoreCase("RequiredSet"))
								{
									processNode(tempNode.getChildNodes(), file, 2, node);
								}
								break;
							case DISPLAY_NAME:
								if (tempNode.getNodeName().equalsIgnoreCase("DisplayName"))
								{
									processNode(tempNode.getChildNodes(), file, 2, node);
								}
								break;
						}
					}
				}
				break;
			case 2:
				total = 0;
				for (int count = 0; count < nodeList.getLength(); count++)
				{
					Node tempNode = nodeList.item(count);
					if (tempNode.getNodeType() == Node.ELEMENT_NODE)
					{
						switch (node)
						{
							case REQUIRED_SET:
								if (tempNode.getNodeName().equalsIgnoreCase("iBlueprintLibrary-cBlueprintSetID"))
								{
									processNode(tempNode.getChildNodes(), file, 3, node);
								}
								break;
							case DISPLAY_NAME:
								if (tempNode.getNodeName().equalsIgnoreCase("Localisation-cUserLocalisedString"))
								{
									processNode(tempNode.getChildNodes(), file, 3, node);
								}
								break;
						}
					}
				}
				break;
			case 3:
				for (int count = 0; count < nodeList.getLength(); count++)
				{
					Node tempNode = nodeList.item(count);
					if (tempNode.getNodeType() == Node.ELEMENT_NODE)
					{
						switch (node)
						{
							case REQUIRED_SET:
								if (tempNode.getNodeName().equalsIgnoreCase("Provider"))
								{
									String content = tempNode.getTextContent();
									if (!content.matches(".*[ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz].*"))
									{
										if (total > 500)
										{
											if (!corruptFiles.contains(file))
											{
												corruptFiles.add(file);
												log.error("Found possibly corrupt file (" + total + "): " + file.getAbsolutePath());
											}
										}
										total++;
									}
								}
								break;
							case DISPLAY_NAME:
								if (tempNode.getTextContent().length() > 0 && !workshopNamesToCompare.contains(tempNode.getTextContent()) && !tempNode.getNodeName().equalsIgnoreCase("Other") && !tempNode.getNodeName().equalsIgnoreCase("Key"))
								{
									workshopNamesToCompare.add(new WorkshopFile(tempNode.getTextContent(), file));
									//	log.info("Added Display Name -> {}", tempNode.getTextContent());
								}
								break;
						}
					}
				}
				break;
		}

	}

}

class WorkshopFile
{
	String name;
	File file;

	WorkshopFile(String name, File file)
	{
		this.name = name;
		this.file = file;
	}

	public boolean equals(Object o)
	{
		if (o instanceof WorkshopFile)
		{
			WorkshopFile toCompare = (WorkshopFile) o;
			return this.name.equals(toCompare.name);
		}
		return false;
	}
}