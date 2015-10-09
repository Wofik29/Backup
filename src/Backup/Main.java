package Backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.FileHandler;;

public class Main {

	/**
	 * @param args
	 */
	
	static Logger log;
	static LogManager lg;
	
	public static void main(String[] args) 
	{
		
		log = Logger.getLogger(Main.class.getName());
		
		try
		{
			LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
		}
		catch (Exception ex)
		{
			
		}
		
		for(int i=0; i<args.length; i++ )
		{
			System.out.println(args[i]);
			
		}
		System.out.println();
		
		
		if (args.length<3)
		{
			System.out.println("Не указан тип бэкапа");
			return;
		}
		else
		{
			
			//System.out.println(args[2]);
			
			Backup b = new Backup(args[0], args[1],  (args[2].equals("Full")) );
			b.start();		
		}
	}

}
