public class Variables
{
	public static String x64exe = "RailWorks64.exe", x86exe = "RailWorks.exe",
		mainStatus = "The program will now find your corrupted scenarios / routes. Depending on how many routes / scenarios you have, this process could take a long time.<br><br>" +
		"Don't close the Train Sim process, the program wants Train Sim to crash so it can find the issue.<br><br>" +
		"Don't close this program either or all progress made in checking your files will be lost.<br><br>";
	public static boolean is64bit = true, initialTestEnabled, testing = false, hasCrashed, hasNotCrashed, firstRun = true, isFirstRun = true;
	public static int currentlyChecking = 5, tries, startingSize;
	public static long startTime, currentTime;
	public static int crashTimes;
}
