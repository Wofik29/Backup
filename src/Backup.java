
import java.io.File;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;

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
	private List<String> source_dirs;	// список директории 
	private List<String> source_files;	// список файлов
	private List<String> target_dirs;	// список директории бэкапа
	private List<String> target_files;	// список файлов бэкапа


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
			if (!(backup_handler.exists()) || isFull)
			{
				backup_handler.mkdir();
				copyFiles(new File(target), new File(source));
			}
			else
			{
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

		// Создание списка файлов и папок бэкапа и хранилища
		createListBackupFiles(bp, root_target, target_dirs, target_files);
		createListBackupFiles(stg, root_source, source_dirs, source_files);


		Collections.sort(target_files);
		//Collections.sort(backup_dirs);
		Collections.sort(source_files);
		//Collections.sort(storage_dirs);
		//List<String> delete_list;
		
		Iterator<String> target_it = target_files.iterator();
		Iterator<String> source_it = source_files.iterator();
		
		String target_path = target_it.next();
		String source_path = source_it.next();
		
		while (source_it.hasNext())
		{
			if (target_it.hasNext())
			{
				
				//System.out.println(target_path + ", " + source_path);
		
				File target_file = new File(target+target_path);
				File source_file = new File(source+source_path);
				StringBuilder sb = new StringBuilder();
				String particle;
				String name_file = target_file.getName();
				
				File history_file = new File(history+target_path).getParentFile();
				if (!history_file.exists()) history_file.mkdirs();
				
				
				if  (target_path == source_path)
				{
					// Если одинаковое имя
					// TODO сделать проверку не только на время изменение.
					
					if (source_file.lastModified() > target_file.lastModified() || source_file.length() != target_file.length() )
					{
						//System.out.println(true);
						particle = new SimpleDateFormat("yyyy-MM-dd").format( System.currentTimeMillis() ); // Создаем текущую дату в виде строки
						
						sb.append(history_file.getPath()).
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
						
						
						try
						{	
							File f = new File(sb.toString());
							if (f.exists()) f.delete();
							Files.copy(target_file.toPath(), f.toPath());
							//System.out.println(sb);
						}
						catch (Exception ex)
						{
							Main.log.print("Не удалось скопировать "+target_file.toString()+" в историю - "+sb);
						}
						
						try
						{	
							target_file.delete();
							Files.copy(source_file.toPath(), target_file.toPath());
							target_file.setLastModified(source_file.lastModified());							
							//System.out.println("copy "+source_file.getPath() + ", "+target_file.getPath());
						}
						catch (Exception ex)
						{
							Main.log.print("Не удалось скопировать "+source_file.toString()+" в бэкап");
						}
					}
					else
					{
						if (source_file.length() != target_file.length() && source_file.lastModified() < target_file.lastModified())
						{
							Main.log.print("Файлы "+source_path+" и "+target_path+"не равны по размеру, но в источнике файл не новый.");
						}
					}
					
					source_path = source_it.next();
					target_path = target_it.next();
				} 
				else // Если имена не сопадают
				{
					if (source_path.compareTo(target_path) > 0) // Если больше, то a>b. по алфавиту мы прошли target_name
					{
						// Копируем в history как удаленный
						particle = "DELETE_"+new SimpleDateFormat("yyyy-MM-dd").format( System.currentTimeMillis() );
						sb.append(history_file.getPath()).
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
						
						try
						{	
							File f = new File(sb.toString());
							if (f.exists()) f.delete();
							Files.copy(target_file.toPath(), f.toPath());
							
						}
						catch (Exception ex)
						{
							Main.log.print("Не удалось скопировать "+target_file.toString()+" в историю");
						}
						
						try
						{
							if (target_file.exists()) target_file.delete();
						}
						catch (Exception ex)
						{
							Main.log.print("Не удалось удалить "+target_file.toString());
						}
						
						// Смотрим следующий файл в target
						target_path = target_it.next();
					}
					else // Иначе этот файл новый, и мы еще не дошли до нашего файла.
					{
						try
						{	
							File f = new File(target+source_path);
							f.delete();
							Files.copy(source_file.toPath(), f.toPath());
							f.setLastModified(source_file.lastModified());
							//System.out.println("copy ");
							//System.out.println(source_file.getPath() + ", "+target_file.getPath());
						}
						catch (Exception ex)
						{
							Main.log.print("Не удалось скопировать "+source_file.toString()+" в бэкап");
						}
						
						source_path = source_it.next();
						
					}
				}
			}
		}
	}

	/*
	 * Метод рекурсивно делает полный бэкап данных
	 */
	private void copyFiles(File target, File source)
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
				copyFiles(target_file, source_file);
			}
			else
			{
				try
				{
					if (target_file.exists()) target_file.delete();
					Files.copy(source_file.toPath(), target_file.toPath() );
					target_file.setLastModified(source_file.lastModified());
				}
				catch (Exception e)
				{
					Main.log.print("Не удалось скопировать "+source_file.getPath()+" в бэкап");
				}
			}
		}
	}
}
