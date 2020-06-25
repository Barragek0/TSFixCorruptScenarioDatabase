
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkshopScraper
{
	static List<String> workshopNameList = new ArrayList<String>();
	private static int timesRan;
	private static int timesRanMain;
	private static boolean finished;
	private static int threadsRunning;
	static List<String> names = new ArrayList<>();
	static List<String> ids = new ArrayList<>();
	static int totalWorkshopPages = 793;
	static int betweenTwoIntsLower = 1;
	static int betweenTwoIntsHigher = 30;
	static int amountOfRuns;
	static int numberOfFiles;

	public static void main(String[] args) throws InterruptedException
	{
		for (int i = 0; i < totalWorkshopPages; i++)
		{
			final int finalI = i; // lambda variables need to be final
			Thread t = new Thread(() -> {
				try
				{
					startThread(finalI);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			});
			t.start();
			System.out.println("Times ran main: " + timesRanMain);
			timesRanMain++;
			Thread.sleep(100);
		}
		System.out.println("Finished first stage");
		splitFileIntoMultiple();
		for (int i = 0; i < numberOfFiles; i++)
		{
			int finalI = i;
			threadsRunning++;
			Thread t = new Thread(() -> {
				try
				{
					if (finalI != 0)
					{
						betweenTwoIntsLower += 30;
						betweenTwoIntsHigher += 30;
					}
					System.out.println("Starting second thread with args: " + betweenTwoIntsLower + " - " + betweenTwoIntsHigher);
					amountOfRuns++;
					secondThread(betweenTwoIntsLower, betweenTwoIntsHigher, amountOfRuns);
					threadsRunning--;
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			});
			t.start();
		}

	}

	private static int running;
	private static int currentI;

	private static void secondThread(int lower, int higher, int amountOfRuns) throws IOException, InterruptedException
	{
		boolean noerror = false;
		for (int i = lower; i < higher; i++)
		{
			String id = ids.get(i);
			try
			{
				id = "https://steamcommunity.com/sharedfiles/filedetails/?id=" + id;
				String text = getPage(id);
				id = text.substring(text.indexOf("#scnr{"), text.length());
				id = id.substring(6, id.indexOf("}\">"));
				//System.out.println("Id3: " + id);
				new File(Main.baseDirectory + "/Workshop/").mkdirs();
				Path path = Paths.get(Main.baseDirectory + "/Workshop/" + amountOfRuns + ".txt");
				Charset charset = StandardCharsets.UTF_8;

				String content = new String(Files.readAllBytes(path), charset);
				String replaceWith = ids.get(i) + " || " + id;
				if (content.contains(ids.get(i)) && !content.contains(id))
				{
					System.out.println("" + replaceWith);
					content = content.replaceAll("" + ids.get(i), replaceWith);
					Files.write(path, content.getBytes(charset));
				}
				Thread.sleep(5000);
			} catch (Throwable e) {
				if (e instanceof StringIndexOutOfBoundsException) {
					System.err.println("Out of bounds for: " + id);
				}
				threadsRunning--;
				if (threadsRunning <= 0)
				{
					System.out.println("Finished");
				}
			}

		}
		if (threadsRunning <= 0)
		{
			System.out.println("Finished");
		}
	}

	static public String getPage(String urlString){
		String result = "";
		try {
			URL url = new URL(urlString);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			String str;
			while ((str = in.readLine()) != null) {
				result += str;
			}
			in.close();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return result;
	}

	private static void act(Document doc, int i, int amountOfRuns) throws IOException
	{
			String pageContent = doc.body().toString();
			String pageContentEdited = pageContent.substring(pageContent.indexOf("#scnr{"), pageContent.length());
			pageContentEdited = pageContentEdited.substring(6, pageContentEdited.indexOf("}"));
		new File(Main.baseDirectory + "/Workshop/").mkdirs();
			Path path = Paths.get(Main.baseDirectory + "/workshop" + amountOfRuns + ".txt");
			Charset charset = StandardCharsets.UTF_8;

			String content = new String(Files.readAllBytes(path), charset);
			String replaceWith = ids.get(i) + " || " + pageContentEdited;
			if (content.contains(ids.get(i)) && !content.contains(pageContentEdited))
			{
				System.out.println("" + replaceWith);
				content = content.replaceAll("" + ids.get(i), replaceWith);
				Files.write(path, content.getBytes(charset));
			}
			else
			{
				//System.out.println("couldn't find " + ids.get(i) + " sleeping and retrying in a little while");
				//Thread.sleep(10000);
				//currentI--;
			}
		System.out.println("Times ran: " + timesRan);
		timesRan++;
	}

	private static void startThread(int i) throws InterruptedException, IOException
	{
		{
			try
			{
				Document doc = Jsoup.connect("https://steamcommunity.com/workshop/browse/?appid=24010&actualsort=trend&p=" + i).timeout(30000).get();
				Elements repositories = doc.getElementsByClass("workshopItemTitle ellipsis");
				String source = doc.body().toString();

				for (Element name : repositories)
				{
					int start = source.indexOf("{\"id\":\"");
					int end = start + 40;
					String currentid = source.substring(start, end);
					currentid = currentid.substring(0, currentid.indexOf("\",\"title\""));
					source = source.substring(end, source.length());
					currentid = currentid.replace("{\"id\":\"", "");
					workshopNameList.add(name.text() + " || " + currentid);
					names.add(name.text());
					ids.add(Math.abs(Integer.parseInt(currentid)) + "");
				}

				FileWriter writer = new FileWriter(Main.baseDirectory + "/workshop.txt");
				for (String string : workshopNameList)
				{
					writer.write(string + System.lineSeparator());
				}
				writer.close();
			}
			catch (Throwable e)
			{
				if (e instanceof ConcurrentModificationException || e instanceof HttpStatusException || e instanceof SocketTimeoutException)
				{
					int finalI = i;
					Thread t = new Thread(() -> {
						try
						{
							startThread(finalI);
						}
						catch (InterruptedException ex)
						{
							//ex.printStackTrace();
						}
						catch (IOException ex)
						{
							//ex.printStackTrace();
						}
					});
					t.start();
				}
				else
				{
					e.printStackTrace();
				}
			}
		}
	}

	private static List<File> splitFileIntoMultiple()
	{
		List<File> fileList = new ArrayList<>();
		try
		{
			String inputfile = Main.baseDirectory + "/workshop.txt";
			double nol = 30;
			File file = new File(inputfile);
			Scanner scanner = new Scanner(file);
			int count = 0;
			while (scanner.hasNextLine())
			{
				scanner.nextLine();
				count++;
			}
			System.out.println("Lines in the file: " + count);

			double temp = (count / nol);
			int temp1 = (int) temp;
			int nof = 0;
			if (temp1 == temp)
			{
				nof = temp1;
			}
			else
			{
				nof = temp1 + 1;
			}
			System.out.println("No. of files to be generated :" + nof);
			numberOfFiles = nof;


			FileInputStream fstream = new FileInputStream(inputfile);
			DataInputStream in = new DataInputStream(fstream);

			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;

			new File(Main.baseDirectory + "/Workshop/").mkdirs();

			for (int j = 1; j <= nof; j++)
			{
				fileList.add(new File(Main.baseDirectory + "/Workshop/" + j + ".txt"));
				FileWriter fstream1 = new FileWriter(Main.baseDirectory + "/Workshop/" + j + ".txt");     // Destination File Location
				BufferedWriter out = new BufferedWriter(fstream1);
				for (int i = 1; i <= nol; i++)
				{
					strLine = br.readLine();
					if (strLine != null)
					{
						out.write(strLine);
						if (i != nol)
						{
							out.newLine();
						}
					}
				}
				out.close();
			}

			in.close();
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
		return fileList;
	}

	static void readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException
	{
		byte[] buf = new byte[(int) numBytes];
		int val = raf.read(buf);
		if (val != -1)
		{
			bw.write(buf);
		}
	}
}