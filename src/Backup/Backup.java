package Backup;

import java.io.File;
import java.util.List;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

/*
 * Объект, который работает с бэкапом.
 */

public class Backup {
	
	private String storage;				// От куда брать
	private String backup;				// Куда сохранять
	private String history; 			// файлова история
	
	private boolean isFull;				// Флаг. Полный или частичный бэкап
	
	/*
	 *  индекс символа, с которого брать относительный путь, для переноса файла в бэкап
	 *  
	 *  Например:  
	 *  /Something/path/Storage/Something_dir/core.functions1.php
	 *		В данном случает этим индексом будет "e" в слове storage. т.е. остальной путь мы копируем
	 *  и присоединяем в этому пути.
	 *	/Path/To/Backup/Files/Something_dir/core.functions1.php
	 *  
	 */
	private int num_of_path; 
	
	private List<File> storage_dirs;	// список директории 
	private List<File> storage_files;	// список файлов
	private List<File> backup_dirs;		// список директории бэкапа
	private List<File> backup_files;	// список файлов бэкапа
	
//	private Date now_time;			
//	private Date last_time;
//	private Date last_full_backup;
	
//	private File storage_file;		
//	private File backup_file;		
//	private File history_file;		 
	
	
	public Backup(String backup, String storage, boolean isFull)
		{
			this.storage = storage;
			num_of_path = storage.length();
			this.backup = backup+File.separator+"current";;
			this.history = backup+File.separator+"history";
			this.isFull = isFull;
			
			backup_dirs = new ArrayList<File>();
			backup_files = new ArrayList<File>();
			storage_dirs = new ArrayList<File>();
			storage_files = new ArrayList<File>();
			
/*			
			backup_file = new File(this.backup);
			if (!backup_file.exists()) backup_file.mkdir();
			
			history_file = new File(this.history);
			if (!history_file.exists()) history_file.mkdir();
			
			storage_file = new File(this.storage);
*/
		}
	
	public void start()
		{
			
			// Тут проверка на то, какой бэкап делать полный\частичный
			File backup_handler = new File(backup);
			if (!(backup_handler.exists()) || isFull)
			{
				System.out.println("Full");
				//Main.log.info("Start full backup");
				backup_handler.mkdir();
				copyFiles(new File(backup), new File(storage));
				//Main.log.info("End full backup");
			}
			else
			{
				System.out.println("noFull");
				additionalBackup(new File(backup), new File(storage), new File(history));
			}
			//save();
		}
	
	
	
	// Создание списка файлов в бэкапе
	public void createListBackupFiles(File f, List<File> dirs, List<File> files)
	{

		//System.out.println(f.getPath());
		//System.out.println(f.listFiles());
		for (File file : f.listFiles())
		{
			if (file.isDirectory()) 
			{
				dirs.add(file);
				createListBackupFiles(file, dirs, files);
			}
			else
			{
				files.add(file);
			}
		}
		
	}
	
	// Проверка на совпадение файл из backup
	public boolean checkoutFile(Iterator<File> it, File file)
	{
		// пробегать по масиву dirs\files на поиск соответствия.
		while (it.hasNext())
		{
			File f = (File) it.next();
			
			if (f.getName().equals(file.getName()) && f.lastModified()<file.lastModified() )
			{
				return true;
			}
			else
			{
				
			}
		}
		
		
		return false;
	}
	
	public void additionalBackup(File bp, File stg, File history)
	{
		createListBackupFiles(bp, backup_dirs, backup_files);
		createListBackupFiles(stg, storage_dirs, storage_files);
		
		Collections.sort(backup_files);
		Collections.sort(backup_dirs);
		Collections.sort(storage_files);
		Collections.sort(storage_dirs);
		
		Iterator<File> backup_it = backup_files.iterator();
		Iterator<File> storage_it = storage_files.iterator();
		
		File backup_file = backup_it.next();
		File storage_file = storage_it.next();

// Много if. Как уменьшить?
		while (storage_it.hasNext())
		{
			if (backup_it.hasNext())
			{
				if (backup_file.getName() != storage_file.getName()) 
				{
					try
					{
						FileTime ft = Files.readAttributes(storage_file.toPath(), BasicFileAttributes.class).creationTime();
						if (ft.toMillis() - ( new Date().getTime()) <= 1000*60*60*24 ) // Если файл создан за последние сутки, то копируем как новый  
						{
							String path = backup_file.getPath().substring(num_of_path);
							Files.copy(backup_file.toPath(), new File(path).toPath() );
						}
					}
					catch (Exception ex){}
					
				}
			}
		}
		
	/*	
		for (File file : stg.listFiles())
		{
			if (file.isDirectory())
			{
				//checkoutFile(dirs.iterator(), file);
				additionalBackup(new File(bp.getPath()+File.separator+file.getName()), file, new File(history.getPath()+File.separator+file.getName()));
			}
			else
			{
				// Если в конце файла нет ~ (знака временного файла), и файл новый, то копировать
				if(true) //file.getName().substring(file.getName().length()-1) != "~" && checkoutFile(files.iterator(), file) )
				{
					DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
				    String s = formatter.format(new Date(file.lastModified()) );
				    String name = null;
//TODO проверка на удаленные файлы
				    File f = new File(bp.getPath()+File.separator+file.getName());
					try
				    {
						if (f.exists())
						{
							// Создаем имя для файла в history с датой переноса
							name = file.getName().substring(0, file.getName().lastIndexOf('.')+1)+s+'.'+file.getName().substring(file.getName().lastIndexOf('.')+1);
							
							//Проверяем, есть ли папка history
							if (!history.exists())
							{
								history.mkdirs();
							}
							
							Files.copy(f.toPath(), new File(history.getPath()+File.separator+name).toPath() ); // Копирование из бэкапа в историю
							f.delete();
						}
						Files.copy(file.toPath(), new File(bp.toPath()+File.separator+file.getName() ).toPath());//копирование в бэкап
						System.out.println(file.getPath());
				    }
				    catch (Exception ex)
				    {
				    	ex.printStackTrace();
				    	System.out.println( history.getPath()+File.separator+name);
				    }
				}
			}
		}
		*/
	}
	
	/*
	 *  Метод рекурсивно делает полный бэкап данных и сохраняет объект
	 */
	private void copyFiles(File bp, File stg)
	{
		for (File file : stg.listFiles())
		{
			//System.out.println(file.getPath());
			
			if (file.isDirectory())
			{
				File next = new File(bp+File.separator+file.getName());
				next.mkdir();
				//System.out.println("directory "+next.getAbsolutePath());
				copyFiles(next, file);
			}
			else
			{
				try
				{
					Files.copy(file.toPath(),new File(bp.toString()+File.separator+file.getName() ).toPath() ) ;
				}
				catch (Exception e)
				{
					//Main.log.log(Level.SEVERE, "Exception: Failed copy fail - "+file.toPath() +"\n"+ e);
				}	
			}
		}
	
	}
	
	private void save()
	{
		
		try
		{
			File f = new File( 
					backup.substring(0, backup.lastIndexOf(File.separator))+
					File.separator+"backup.ser");
			ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f));
			os.writeObject(this);
			os.close();
			Main.log.info("Saving completed");
		}
		catch (Exception ex)
		{
			//Main.log.log(Level.SEVERE, "Exception: Failed saving \n"+ ex);
		}	
	}

}
