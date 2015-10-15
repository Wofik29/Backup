package Backup;

import java.io.File;
import java.util.List;
import java.nio.file.Files;
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
	
	private String storage;				// От куда брать
	private String backup;				// Куда сохранять
	private String history; 			// файловая история
	
	private boolean isFull;				// Флаг. Полный или частичный бэкап
	
	private List<File> storage_dirs;	// список директории 
	private List<File> storage_files;	// список файлов
	private List<File> backup_dirs;		// список директории бэкапа
	private List<File> backup_files;	// список файлов бэкапа
	
	
	public Backup(String backup, String storage, boolean isFull)
		{
			this.storage = storage;
			this.backup = backup+File.separator+"current";;
			this.history = backup+File.separator+"history";
			this.isFull = isFull;
						
			backup_dirs = new ArrayList<File>();
			backup_files = new ArrayList<File>();
			storage_dirs = new ArrayList<File>();
			storage_files = new ArrayList<File>();	
		}
	
	public void start()
		{
			
			// Проверка на то, какой бэкап делать полный\частичный
			File backup_handler = new File(backup);
			if (!(backup_handler.exists()) || isFull)
			{
				Main.log.info("Start full backup");
				backup_handler.mkdir();
				copyFiles(new File(backup), new File(storage));
				Main.log.info("End full backup");
			}
			else
			{
				Main.log.info("Start additional backup");
				additionalBackup(new File(backup), new File(storage), new File(history));
				Main.log.info("Start additional backup");
			}
			//save();
		}
	
	
	
	// Создание списка файлов в бэкапе
	public void createListBackupFiles(File f, List<File> dirs, List<File> files)
	{
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
	
	public void additionalBackup(File bp, File stg, File history)
	{
		if (!history.exists()) history.mkdir(); // Создание папки с историей, если нет
		
		// Создание списка файлов и папок бэкапа и хранилища
		createListBackupFiles(bp, backup_dirs, backup_files);
		createListBackupFiles(stg, storage_dirs, storage_files);
		
		
		Collections.sort(backup_files);
		//Collections.sort(backup_dirs);
		Collections.sort(storage_files);
		//Collections.sort(storage_dirs);
		
		Iterator<File> backup_it = backup_files.iterator();
		Iterator<File> storage_it = storage_files.iterator();
		
		File backup_file = backup_it.next();
		File storage_file = storage_it.next();

// Много if. Как уменьшить?
		while (storage_it.hasNext())
		{
			// Проверка на временный файл
			if (storage_file.getName().substring(storage_file.getName().length()-1).equals("~"))
			{
				storage_file = storage_it.next();
				continue;
			}
			
			
			if (backup_it.hasNext())
			{
				if (backup_file.getName().equals(storage_file.getName())) 
				{
					// Если именна файлов совпадают, но файл из хранилище новее, то копировать
					if (backup_file.lastModified() < storage_file.lastModified()) 
					{
						// Создаем имя файла с временем изменения
						DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH:mm");
					    String s = formatter.format(new Date(backup_file.lastModified()) );
					    String name = backup_file.getName();
					    name = name.substring(0, name.lastIndexOf('.')+1)+s+'.'+name.substring(name.lastIndexOf('.')+1);
					    
						//копируем из бэкапа в историю
					    String path = backup_file.getPath().replaceAll(backup, history.getPath());
					    path = path.replaceAll(backup_file.getName(), name); 
					    //System.out.println("Copy to history : "+ backup_file.toString() + " - "+path);
					    
					    try 
					    {
					    	Files.copy(backup_file.toPath(), new File(path).toPath());
						} 
					    catch (Exception e) 
						{
							e.printStackTrace();
						}
					    backup_file.delete();
					    
					    // копируем из сторэдж в бэкап
					    path = storage_file.getPath().replaceAll(storage, backup);					    					    
					    
					    //System.out.println("Copy to backup : "+ storage_file.toString() + " - "+path);
					    
					    try 
					    {
					    	File f = new File(path);
					    	if (!f.exists()) Files.copy(storage_file.toPath(), f.toPath());
						} 
					    catch (Exception e) 
						{
							e.printStackTrace();
						}
					    
					}
					// TODO проверка, одинаковый ли путь. Т.е. файл может быть тот же, но в другом месте (случайный перенос) 
					
					// System.out.println("equal: " + storage_file.toString());
					storage_file = storage_it.next();
					backup_file = backup_it.next();
				}
				else
				{
					// Если файлы не совпали по имени, то проверяется, новый ли файл в хранилизеще
					try
					{
						FileTime ft = Files.readAttributes(storage_file.toPath(), BasicFileAttributes.class).creationTime();
						if (ft.toMillis() - ( new Date().getTime()) >= 1000*60*60*24 ) // Если файл создан за последние сутки, то копируем как новый  
						{
							
							String path = storage_file.getPath().replaceAll(storage, backup);							
							Files.copy(storage_file.toPath(), new File(path).toPath() );
							Main.log.info("Добавлен новый файл: "+path);
							storage_file = storage_it.next();
						}
						else
						{
							DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy-HH:mm");
						    String s = formatter.format(new Date(backup_file.lastModified()) );
						    String name = backup_file.getName();
						    name = name.substring(0, name.lastIndexOf('.')+1)+"DELETE."+s+'.'+name.substring(name.lastIndexOf('.')+1);
						    
						    String path = backup_file.getPath().replaceAll(backup, history.getPath());
						    path = path.replaceAll(backup_file.getName(), name);
						    
						    Files.copy(backup_file.toPath(), new File(path).toPath());
						    
							Main.log.log(Level.SEVERE, "Файл из хранилища не совпал с файлом из бэкапа. Перемещен в папку истории : " + path);
							backup_file = backup_it.next();
						}
					}
					catch (Exception ex)
					{
						System.out.println(ex.toString());						
					}
				}
			}
		}	
	}
	
	/*
	 *  Метод рекурсивно делает полный бэкап данных и сохраняет объект
	 */
	private void copyFiles(File bp, File stg)
	{
		for (File file : stg.listFiles())
		{
		
			if (file.isDirectory())
			{
				File next = new File(bp+File.separator+file.getName());
				next.mkdir();
				copyFiles(next, file);
			}
			else
			{
				try
				{
					
					char c = file.getName().charAt(file.getName().length()-1);
					if ( c != '~') Files.copy(file.toPath(),new File(bp.toString()+File.separator+file.getName() ).toPath() ) ;
				}
				catch (Exception e)
				{
					Main.log.log(Level.SEVERE, "Exception: Failed copy fail - "+file.toPath() +"\n"+ e);
				}	
			}
		}
	}
}
