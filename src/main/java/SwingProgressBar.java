import java.awt.Component;
import java.awt.Dimension;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public class SwingProgressBar extends JPanel
{
	int current;
	JProgressBar progressBar;
	JLabel textComponent;
	JLabel textComponentTimeRemaining;
	String status = "<html><body><center><p style=\"width:450px\">Loading...</p><center></body></html>";
	String secondaryStatus = "";

	public SwingProgressBar()
	{
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setAlignmentX(Component.CENTER_ALIGNMENT);
		lineBreak();
		textComponent = new JLabel(status);
		textComponent.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(textComponent);
		lineBreak();
		progressBar = new JProgressBar();
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		updateBar(0);
		progressBar.setMaximumSize(new Dimension(500, 25));
		progressBar.setMinimumSize(new Dimension(500, 25));
		progressBar.setSize(500, 25);
		progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(progressBar);
		lineBreak();
		textComponentTimeRemaining = new JLabel("");
		textComponentTimeRemaining.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(textComponentTimeRemaining);
		lineBreak();
	}

	private void lineBreak()
	{
		add(Box.createRigidArea(new Dimension(10, 10)));
	}

	public void updateBar(int newValue)
	{
		current = newValue;
		progressBar.setValue(newValue);
	}

	public void updateStatus(String status)
	{
		textComponent.setText("<html><body><center><p style=\"width:450px\">" + status + "</p><center></body></html>");
	}

	public void updateTimeRemaining(String time)
	{
		textComponentTimeRemaining.setText("<html><body><center><p style=\"width:450px\">" + time + "</p><center></body></html>");
	}

	public void updateSecondaryStatus(String status)
	{
		secondaryStatus = status;
	}

	public void resetProgressBar()
	{
		progressBar.setValue(0);
	}

	public void appendBar()
	{
		current = current + 1;
		progressBar.setValue(current);
	}

	public void appendBar(int i)
	{
		current = current + i;
		progressBar.setValue(current);
	}

	public void updateProgressBarText(int current, int total, long startTime, long currentTime)
	{
		try
		{
			if (current != 0 && total != 0)
			{
				long timeLeft = Utilities.calculateTimeRemaining(startTime, currentTime);
				String timeLeftFormatted = String.format("%d min, %d sec",
					TimeUnit.MILLISECONDS.toMinutes(timeLeft),
					TimeUnit.MILLISECONDS.toSeconds(timeLeft) -
						TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeLeft))
				);
				double percentage = Utilities.round(((double) current / (double) total) * 100, 2);
				progressBar.setString((percentage == 0 ? 0 : percentage) + "%");
				updateTimeRemaining("Estimated time remaining: " + timeLeftFormatted);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}

	public void setProgressBarIndeterminate(boolean b)
	{
		if (!b)
		{
			progressBar.setStringPainted(true);
		}
		else
		{
			progressBar.setStringPainted(false);
		}
		progressBar.setIndeterminate(b);
	}
}
