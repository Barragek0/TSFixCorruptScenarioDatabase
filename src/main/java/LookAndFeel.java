import java.awt.Color;
import javax.swing.UIManager;

public class LookAndFeel
{
	public static void setLookAndFeel(String name)
	{
		try
		{
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
			{
				if (name.equals(info.getName()))
				{
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					if (name.equals("Nimbus")) {
						UIManager.put("ProgressBar.repaintInterval", new Integer(1));
						UIManager.put("ProgressBar.cycleTime", new Integer(500));
					}
					break;
				}
			}
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (javax.swing.UnsupportedLookAndFeelException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
