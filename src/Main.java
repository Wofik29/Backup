import java.io.File;

public class Main {

	/**
	 * @param args
	 */
	
	static Logger log;
	
	public static void main(String[] args) 
	{
		long start = System.nanoTime();
		
		boolean isFull = true;
		String source = "";
		String target = "";
		
		for (String str : args)
		{
			if (str.equals("--help") || str.equals("-h"))
				help();
			else if (str.equals("-f"))
				isFull = true;
			else if (str.equals("-i"))
				isFull = false;
			else if (str.substring(0, 1).equals("-"))
				System.out.println("Неизвестный параметр -> "+str);
			else if (source.isEmpty())
				source = str;
			else if (target.isEmpty())
				target = str;
		}
	
		
		
		if (! (source.isEmpty() && target.isEmpty()))
		{
			// Проверки на возможность чтение и возможность записи
			File test = new File(target);
			{
				File f = new File("");
				if (f.canWrite())
				{
					System.exit(1);
				}
			}
			
			String baseDir = new File("").getAbsolutePath();			
			test = new File (source);
			log = new Logger(baseDir+"/backup_log");
						
			if ("".equals(source))
			{
				log.print("Не указан путь до Source");
				System.exit(1);
			}
			
			if ("".equals(target))
			{
				log.print("Не указан путь до Target");
				System.exit(1);
			}
			
			if (! (test.canRead() && test.canWrite()) ) 
			{
				log.print("Нет доступа для чтения Source");
				System.exit(1);
			}
		
			if (! (test.canRead() && test.canWrite()) ) 
			{
				log.print("Нет доступа для чтения target");
				System.exit(1);
			}
			
			
			
			log.print("Start "+source+" -> "+target);
			Backup b = new Backup(source, target, isFull );
			b.start();
			
			// Выводим время работы
			long end = System.nanoTime();
			long time = (end - start)/ 1000000 ;
			time /= 1000;
			long sec = time%60;
			time /= 60;
			long min = time%60;
			time /= 60;
			log.print("Done "+source+" -> "+target+" (Time: "+(time == 0 ? (min == 0 ? sec+" s" : min+":"+sec) :  time +":"+min+":"+sec )+" )");
			//log.print("Время бэкапа: "+ time +":"+min+":"+sec);
		}
	}
	
	private static void help()
	{
		System.out.println("SYNOPSIS \n \t[option] sourcePath targetPath\n" +
				"DESCRIPTION \n" +
				"\t-f - полный бэкап \n" +
				"\t-i - дополняющий бэкап \n" +
				"\t-h (--help) данная справка");
	}

}
