
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;


/*
 * Объект, который работает с бэкапом.
 */

public class Backup {

	private String source;				// От куда брать
	private String target;				// Куда сохранять
	private String history; 			// файловая история

	private boolean isFull;				// Флаг. Полный или частичный бэкап

	private int root_source;
	private int root_target;
	private int root_history;
	
	private String history_delete_suffix = "[DELETE_%d]";
	private String history_update_suffix = "[%d]";
	
	private List<String> source_dirs;	// список директории 
	private List<String> source_files;	// список файлов
	private List<String> target_dirs;	// список директории бэкапа
	private List<String> target_files;	// список файлов бэкапа
	private List<String> ignore_list;


	public Backup(String storage, String backup, boolean isFull)
		{

			this.source = storage.replace('\\', '/');
			this.target = backup.replace('\\', '/')+"/current";;
			this.history = backup.replace('\\', '/')+"/history";
			this.isFull = isFull;

			this.root_target = this.target.length();
			this.root_source = this.source.length();
			this.root_history = this.history.length();

			target_dirs = new ArrayList<String>();
			target_files = new ArrayList<String>();
			source_dirs = new ArrayList<String>();
			source_files = new ArrayList<String>();
		}

	public void start()
		{
		// Проверка на то, какой бэкап делать полный\частичный
			
			File backup_handler = new File(target);
			loadConfig();
			if (!(backup_handler.exists()) || isFull)
			{
				backup_handler.mkdir();
				fullBackup(new File(target), new File(source));
			}
			else
			{

				// Создание списка файлов и папок бэкапа и хранилища
				createListBackupFiles(new File(target), root_target, target_dirs, target_files);
				createListBackupFiles(new File(source), root_source, source_dirs, source_files);
				
				isNewFolders();
				additionalBackup(new File(target), new File(source), new File(history));
			}
		}



	// Создание списка строк из корня source\target
	public void createListBackupFiles(File f, int beginIndex, List<String> dirs, List<String> files)
	{
		for (File file : f.listFiles())
		{
			String s = file.getPath().substring(beginIndex).intern();
			if (file.isDirectory()) 
			{
				dirs.add(s);
				createListBackupFiles(file, beginIndex, dirs, files);
			}
			else
			{
				files.add(s);
			}
		}
	}

	public void additionalBackup(File bp, File stg, File history)
	{
		if (!history.exists()) history.mkdir(); // Создание папки с историей, если нет
		
		Collections.sort(target_files);
		Collections.sort(source_files);
		
		//System.out.println(source_files.size());
		//System.out.println(target_files.size());
		
		//Iterator<String> target_it = target_files.iterator();
		//Iterator<String> source_it = source_files.iterator();
		
		// отвечают за то, брать ли слудующий элемент
		boolean isTarget = true;
		boolean isSource = true;
		
		// отвечают за то, заходить ли в цикл вообще
		boolean has_next_target = true;
		boolean has_next_source = true;
		
		// Локальные пути
		String source_path = "";
		String target_path = "";
		
		// позиция
		int source_number = -1;
		int target_number = -1;
		
		// размер, да
		int source_size = source_files.size()-1;
		int target_size = target_files.size()-1;
		
		
		while (has_next_source)
		{

			//System.out.println(isSource+", "+has_next_source);
			//System.out.println(isTarget+", "+has_next_target);
			
			if (isSource)
			{
				/*
				 *  Все же проверки, чтобы не выйти за границу. 
				 *  Т.к. по другому не получалось
				 *  проверить последние файлы, и чтобы не выходить за диапазон  
				 */
				if (++source_number <= source_size)
				source_path = source_files.get(source_number);				
			} 
			else isSource = true;
		
			File target_parent = new File(target+source_path).getParentFile();
			//System.out.println("S: "+(source_number)+" - "+source_path);
			
			if (!target_parent.exists()) target_parent.mkdirs();
			
			if (has_next_target)
			{
				
				if (isTarget)
				{
					if (++target_number <= target_size)
					target_path = target_files.get(target_number);
					
				}
				else isTarget = true;
				
				//System.out.println("T: "+(target_number)+" - "+target_path);
				File target_file = new File(target+target_path);
				File source_file = new File(source+source_path);
				File history_file = new File(this.history+target_path);
				String particle;
				
				if  (target_path == source_path)
				{
					// Если одинаковое имя
					// TODO сделать проверку не только на время изменение и длинну
					
					if (source_file.lastModified() > target_file.lastModified() || source_file.length() != target_file.length() )
					{
						particle = String.format(history_update_suffix, System.currentTimeMillis()); // Создаем текущую дату в виде строки
						copyToHistory(particle, target_file, history_file);
						
						// Копирование в бэкап
						copyFile(source_file, target_file);
					}
					else
					{
						if (source_file.length() != target_file.length() && source_file.lastModified() < target_file.lastModified())
						{
							Main.log.print("Файлы "+source_path+" и "+target_path+"не равны по размеру, но в источнике файл не новый.");
						}
					}
				}
				else // Если имена не сопадают
				{
					System.out.println("---");
					System.out.println(source_path+" - "+target_path);
					System.out.println(source_path.compareTo(target_path));
					
					if (source_path.compareTo(target_path) > 0) // Если больше 0, то source>target. по алфавиту мы прошли target_name
					{
						// Копируем в history как удаленный
						particle = String.format(history_delete_suffix, System.currentTimeMillis());
						copyToHistory(particle, target_file, history_file);
												
						
						
						// Смотрим следующий файл в target
						isSource = false;
					}
					else // Иначе этот файл новый, и мы еще не дошли до нашего файла.
					{
						if (!isIgnore(source_file.getPath())) copyFile(source_file, new File(target+source_path));
						isTarget = false;
					}
				}
			}
			else
			{
				if (!isIgnore(source+source_path)) copyFile(new File(source+source_path), new File(target+source_path));
				isTarget = false;
			}
			
			// Если в target закончились файлы
			if (target_number >= target_size && isTarget) has_next_target = false;
			
			// Если в source закончились файлы и нужен следующий, т.е. не ждет от target.
			
			if (source_number >= source_size && isSource) has_next_source = false;
			
			//System.out.println();
		}
		
		// Если какие то файлы остались в target, ты мы их копируем как удаленные.
		// System.out.println(target_number+" - "+target_size);
		while (target_number < target_size) 
		{
			String particle = String.format(history_delete_suffix, System.currentTimeMillis());
			
			// Тут увеличиваем, в том случае,
			if (isTarget)	target_number++;
			else isTarget = true;
				
			target_path = target_files.get(target_number);
			
			
			copyToHistory(particle, new File(target + target_path), new File(this.history+target_path));
			//System.out.println("next : "+target + target_path+ ", "+history+target_path);  
		}
	}

	private boolean isIgnore(String path)
	{
		File f = new File(path);
		for (String s : ignore_list)
		{
			if (f.getName().matches(s)) 
			{
				return true;
			};
		}
		
		return false;
	}
	
	
	private void copyToHistory(String particle, File target_file, File history_file)
	{
		StringBuilder sb = new StringBuilder();
		String name_file = target_file.getName();
		
		sb.append(history_file.getParent()).
			append(File.separator);
		
		if (name_file.lastIndexOf('.') == -1)
		{
			sb.append(name_file).
				append(".").
				append(particle);
		}
		else
		{
			sb.append(new String(name_file.substring(0, name_file.lastIndexOf('.')+1))).
				append(particle).
				append(new String(name_file.substring(name_file.lastIndexOf('.'), name_file.length())));
		}
		
		//System.out.println("Copy to history: "+target_file+" - "+sb);
		
		if (!history_file.getParentFile().exists()) history_file.getParentFile().mkdirs();
		copyFile(target_file, new File(sb.toString()));
		
		try
		{
			if (target_file.exists()) target_file.delete();
		}
		catch (Exception ex)
		{
			Main.log.print("Не удалось удалить "+ex.getMessage());
		}	
	}
	
	private void copyFile(File source, File target)
	{	
		try
		{
			if (target.exists()) target.delete();
			Files.copy(source.toPath(), target.toPath());
			target.setLastModified(source.lastModified());
		}
		catch (Exception ex)
		{
			Main.log.print("Не удалось скопировать ---> "+ex);
		}
	}
	
	
	/*
	 * Метод рекурсивно делает полный бэкап данных
	 */
	private void fullBackup(File target, File source)
	{
		for (File source_file : source.listFiles())
		{
			StringBuffer sb = new StringBuffer();
			sb.append(target.toString()).
				append(File.separator).
				append(source_file.getName());
			File target_file = new File(sb.toString());
			
			if (source_file.isDirectory())
			{
				target_file.mkdir();
				fullBackup(target_file, source_file);
			}
			else
			{				
				if (!isIgnore(source_file.getPath())) copyFile(source_file, target_file);
			}
		}
	}
	
	/*
	 * Проверяет, появилась ли новая папка в источнике
	 * Если появилась, то пока только лог об этом
	 */
	private void isNewFolders()
	{

		Collections.sort(target_dirs);
		Collections.sort(source_dirs);
		//System.out.println("find folders");
		//System.out.println(source_dirs.size());
		//System.out.println(target_dirs.size());
		
		//Iterator<String> target_it = target_dirs.iterator();
		//Iterator<String> source_it = source_dirs.iterator();
		
		int source_number = 0;
		int target_number = 0;

		
		boolean isTarget = true;
		boolean isSource = true;
		
		boolean has_next_target = true;
		boolean has_next_source = true;
		
		String source_path = "";
		String target_path = "";
		
		int target_size = target_dirs.size()-1;
		int source_size = source_dirs.size()-1;
		
		while (has_next_source)
		{
			
			if (isSource) source_path = source_dirs.get(source_number++);
			else isSource = true;
						
			if (has_next_target)
			{
				
				if (isTarget) target_path = target_dirs.get(target_number++);
				else isTarget = true;
				
				if (source_path != target_path)
				{
					if (source_path.compareTo(target_path) > 0)
					{
						Main.log.print("папка "+target_path+" удалена");
						isSource = false;
					}
					else
					{
						Main.log.print("новая папка "+source_path);
						isTarget = false;
					}
				}
			}
			else
			{
				Main.log.print("новая папка "+source_path);
			}
			
			
			// Если в target закончились файлы
			if (target_number > target_size) has_next_target = false;
			
			
			// Если в source закончились файлы и нужен следующий, т.е. не ждет от target.
			if (source_number > source_size && isSource) has_next_source = false;
			
		}
		
		while (target_number <= target_size)
		{
			target_path = target_dirs.get(target_number++);
			Main.log.print("папка "+target_path+" удалена");
		}
		
	}
	
	private void loadConfig()
	{
		File conf = new File("Backup.ini");
		//System.out.println("load config");
		ignore_list = new ArrayList<String>();
		
		byte command = 0;
		
		// Чтение файла
		try
		{
			BufferedReader in = new BufferedReader(new FileReader(conf));
			try
			{
				String s;
				String[] strs ;
				while ((s = in.readLine()) != null)
				{
					if (s.charAt(0) == '[')
					{
						switch(s)
						{
						case "[delete]":
							command = 1;
							break;
						case "[update]":
							command = 2;
							break;
						case "[ignore]":
							command = 3;
							break;
						default:
							command = 0;
						}
					}
					else
					{
						switch(command)
						{
						case 0:
							
							break;
						case 1:
							s = s.replace("\"", "");
							strs = s.split("=");
							strs[0] = strs[0].trim();
							strs[1] = strs[1].trim();
							if ("history_suffix".equals(strs[0])) history_delete_suffix = strs[1];
							break;
						case 2:
							s = s.replace("\"", "");
							strs = s.split("=");
							strs[0] = strs[0].trim();
							strs[1] = strs[1].trim();
							if ("history_suffix".equals(strs[0])) history_update_suffix = strs[1];
							break;
						case 3:
							ignore_list.add(s);
							break;
						}
					}
				}
			}
			finally
			{
				in.close();
			}
		} 
		catch (Exception e)
		{
			
		}
	}
}
