package Backup;

import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {

	/**
	 * @param args
	 */
	
	static Logger log;
		
	public static void main(String[] args) 
	{
		long start = System.nanoTime();
		log = Logger.getLogger(Main.class.getName());
		
		try
		{
			LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
		}
		catch (Exception ex)
		{
			System.out.println("Не удалось загрузить настройки логирования");
		}
		
		System.out.println();
		
		
		if (args.length<3)
		{
			System.out.println("Не указан тип бэкапа");
			return;
		}
		else
		{
			Backup b = new Backup(args[0], args[1],  (args[2].equals("Full")) );
			b.start();		
		}
		
		long end = System.nanoTime();
		long time = (end - start)/ 1000000 ;
		System.out.println("Time: "+ time  );
		
	}

}
