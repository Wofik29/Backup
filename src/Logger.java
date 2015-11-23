import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Logger {
	
	File log;

	public Logger(String path)
	{
		log = new File(path);
		if (!log.exists()) 
			try
			{
				log.createNewFile();
			}
			catch (Exception ex)
			{
				System.out.println("Неудалось создать лог файл");				
			}
		
	}
	
	public void printSeparatorTag(boolean isStart)
	{
		try
		{
			FileWriter fw;
			fw = new FileWriter(log, true);
			if (isStart) fw.write("\n");
			for (int i=0;i<20;i++)
			{
				fw.write("=");
			}
			
			if (isStart)
				fw.write("Start");
			else
				fw.write("End");
			
			for (int i=0;i<20;i++)
			{
				fw.write("=");
			}
			fw.write("\n");
			fw.close();
		}
		catch (Exception ex)
		{
			System.out.println("Не удалось записать в лог-файл");
		}
	}
	
	public void print(String text)
	{
		
		try
		{
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String date = formatter.format(new Date()) + ":  ";
			FileWriter fw;
			fw = new FileWriter(log, true);
			fw.write(date + text+"\n");
			
			fw.close();
		}
		catch (Exception ex)
		{
			System.out.println("Не удалось записать в лог-файл");
		}
	}
	
}
