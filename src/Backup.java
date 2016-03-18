import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


public class Backup
{

	private List<String> ignore_list;
	
	private String history_delete_suffix = "[delete_%tF]";
	private String history_update_suffix = "[%tF]";
	
	private String target;
	private String source;
	private String history;
	
	private int target_length;
	private int source_length;
	
	private Component source_new_list;
	private Component target_delete_list;
	
	public Backup(String source, String target)
	{
		this.source = source.replace('\\', '/');
		this.target = target.replace('\\', '/')+"/current";
		this.history = target.replace('\\', '/')+"/history";
		
		target_length = this.target.length();
		source_length = this.source.length();
	}
	
	public void start(boolean isFull)
	{
		//System.out.println(target);
		//System.out.println(source);
		//System.out.println(history);
		
		
		loadConfig();
		if (isFull)
		{
			fullBackup(new File(target), new File(source));
		}
		else
		{
			File f = new File(source);
			Composite source = new Composite(f.getName(), f.getAbsolutePath(), f.lastModified() );
			source.createTree();
			
			f = new File(target);
			if (!f.exists()) f.mkdir();
			
			Component target = new Composite(f.getName(), f.getAbsolutePath(), f.lastModified() );
			target.createTree();
			
			f = new File(history);
			if (!f.exists()) f.mkdir();
			
			source_new_list = new Composite("Source", this.source, source.modified);
			target_delete_list = new Composite("Target", this.target, target.modified);
			
			additionalBackup(source, target);
			
			checkFolders();
			
			//source.print(0);
			//System.out.println();
			//target.print(0);
			
			source.createTree();
			target.createTree();
			
			Main.error_log.print(source.equals_s(target));
		}
	}
	
	private void additionalBackup(Component source, Component target)
	{
		Iterator<Component> source_it = source.getIterator();
		Iterator<Component> target_it = target.getIterator();
		
		Component source_child = null;
		Component target_child = null;
		
		// Флаги, для проверки
		// есть ли еще файлы в списке
		boolean source_has;
		boolean target_has;
		
		// Нужен ли следующий файл
		boolean source_next = true;
		boolean target_next = true;
		
		if (source_it.hasNext()) source_has = true;
		else source_has = false;
		
		if (target_it.hasNext()) target_has = true;
		else target_has = false;
		
		String particle;
		
		while (source_has)
		{
			if (source_it.hasNext() && source_next) source_child = source_it.next();
			else source_next = true;
			
			if (target_has)
			{
				if (target_it.hasNext() && target_next) target_child = target_it.next();
				else target_next = true;
				
				// Если эти элменты директории и равны друг другу
				if (!source_child.isLeaf && !target_child.isLeaf && source_child.name.equals(target_child.name))
				{
					additionalBackup(source_child, target_child);
				}
				else if (source_child.name.equals(target_child.name))
				{
					
					// Если это документы и у них различаются даты
					if (source_child.isLeaf && target_child.isLeaf && source_child.modified != target_child.modified )
					{
						particle = String.format(history_update_suffix, System.currentTimeMillis());
						
						StringBuffer sb = new StringBuffer();
						
						sb.append(history).
							append(target_child.path.substring(target_length));
						
						// Копируем в историю
						copyToHistory(new File(target_child.path), new File(sb.toString()), particle);
						
						//System.out.println(target_child.path.substring(target_length));
						//System.out.println("history : "+history);
						//System.out.println("history : "+sb);
						
						// Копируем в бэкап
						copyFile(new File(source_child.path), new File(target_child.path));
					}
					else if (source_child.isLeaf ^ target_child.isLeaf) // Мало вероятно, чтоб файл и папка были с одинаковым именем оО Что то вообще надо делать?
					{
						
					}
					
				}
				else
				{
					/* Если имена не совпдают, то это разные файлы. Или вообще файл-папка.
					 * Проверяем это.
					 */
					if (source_child.compareTo(target_child) > 0) // Если так, то мы прошли target файл, т.е. его нет в source, т.е. target устарел и его надо удалить
					{
						if (!target_child.isLeaf)
						{
							target_delete_list.add(target_child);
						}
						else
						{
							// Копируем в историю
							particle = String.format(history_delete_suffix, System.currentTimeMillis());
							
							File delete = new File(target_child.path);
							
							copyToHistory(delete, new File(history+target_child.path.substring(target_length)), particle);
							
							delete.delete();
						}
						source_next = false;
					}
					else // иначе это новый файл, т.к. он младше target по алфавиту.
					{
						if (!source_child.isLeaf)
						{
							// Новая папка, отмечаем в списке new
							source_new_list.add(source_child);
						}
						else
						{
							// Копируем в бэкап
							copyFile(new File(source_child.path), new File(target+File.separator+source_child.name));
						}
						target_next = false;
					}
				}
			}
			else
			{
				if (!source_child.isLeaf)
				{
					// Новая папка, отмечаем в списке new
					source_new_list.add(source_child);
				}
				else
				{
					// Копируем в бэкап
					copyFile(new File(source_child.path), new File(target+File.separator+source_child.name));
					System.out.println(target+File.separator+source_child.name);
				}
			}
			
			//if (target_child != null) System.out.println("target : "+target_child.path);
			//if (source_child != null) System.out.println("source : "+source_child.path);
			//System.out.println();
			
			// Если больше нет, а надо, то закрываем ветку.
			if (!target_it.hasNext() && target_next) target_has = false;
			if (!source_it.hasNext() && source_next) source_has = false;
			
		}
		
		// Если в списке target что-то осталось
		while (target_has)
		{
			// Нужен ли следующий
			if (target_next)
			{
				target_child = target_it.next();
				target_next = false;
			}
			
			// Если нам нужен был следующий, то его надо обработать иначе нет
			if (!target_next)
			{
				if (target_child.isLeaf)
				{
					particle = String.format(history_delete_suffix, System.currentTimeMillis());
					
					File delete = new File(target_child.path);
					
					copyToHistory(delete, new File(history+target_child.path.substring(target_length)), particle);
					
					delete.delete();
				}
				else
				{
					target_delete_list.add(target_child);
				}
			}
			
			// если в списке что то есть, то берем, иначе выходим
			if (target_it.hasNext())
				target_child = target_it.next();
			else
				target_has = false;
		}
	}

	/*
	 * Метод рекурсивно делает полный бэкап данных.
	 * Для него ненадо дерева. Просто корни папок.
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
				if (!isIgnore(source_file)) copyFile(source_file, target_file);
			}
		}
	}
	
	/*
	 * Проверяет списки target_delete_list и source_new_list
	 * Если есть совпадение, то в лог
	 * если нет, то удаление
	 */
	private void checkFolders()
	{
		source_new_list.sort();
		target_delete_list.sort();
		
		Iterator<Component> source_it = source_new_list.getIterator();
		Iterator<Component> target_it = target_delete_list.getIterator();
		
		Component source = null;
		Component target = null;
		
		/*
		 * Получается вычислаем, какие из всего списка одинаковы
		 * А в последующих циклах уже удаляем/копируем, что новое/старое
		 */
		while (source_it.hasNext())
		{
			source = source_it.next();
			
			// затратный цикл?
			// Пробегаем по всему списку удаленных и сравниваем.
			// Если совпали, то убираем их из списка и пишем в лог.
			while (target_it.hasNext())
			{
				target = target_it.next();
				
				if (source.equals(target))
				{
					target_it.remove();
					source_it.remove();
					Main.log.print("!!!Equals folders: "+source.path+" -> "+target.path);
					break;
				}
				// если мы по алфавиту прошли дальше, чем имя target, то выходим из цикла.
				else if (source.name.compareTo(target.name) > 0)
				{
					break;
				}
			}
		}
		
		source_it = source_new_list.getIterator();
		target_it = target_delete_list.getIterator();
		
		while (source_it.hasNext())
		{
			source = source_it.next();
			StringBuffer sb = new StringBuffer();
			
			sb.append(this.target).
				append(source.path.substring(source_length));
			
			try
			{
				source.copy(sb.toString());
			}
			catch (Exception ex)
			{
				Main.log.print("Failed copy ---> "+ex);
			}
		}
		
		while (target_it.hasNext())
		{
			target = target_it.next();
			StringBuffer sb = new StringBuffer();
			
			sb.append(this.history).
				append(target.path.substring(target_length));
			
			try
			{
				target.copy(sb.toString());
				target.delete();
			}
			catch (Exception ex)
			{
				Main.log.print("Failed copy ---> "+ex);
			}
			
			File f = new File(sb.toString());
			f.renameTo( new File(f.getAbsolutePath()+" "+ String.format(history_delete_suffix, System.currentTimeMillis()) ));
			
		}
	}
	
	private void copyToHistory(File source, File target, String particle)
	{
		StringBuilder sb = new StringBuilder();
		String name_file = source.getName();
		
		sb.append(target.getParent()).
			append(File.separator);
		
		// Создание имени файл с постфиксом
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
		
		//System.out.println("Copy to history: "+source+" - "+sb);
		
		if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
		copyFile(source, new File(sb.toString()));
		
		try
		{
			if (source.exists()) source.delete();
		}
		catch (Exception ex)
		{
			Main.log.print("Failed deleted ---> "+ex);
		}
	}
	
	private void copyFile(File source, File target)
	{
		
		if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
		
		try
		{
			if (target.exists()) target.delete();
			Files.copy(source.toPath(), target.toPath());
			target.setLastModified(source.lastModified());
		}
		catch (Exception ex)
		{
			Main.log.print("Failed copy ----> "+ex);
		}
	}
	
	
	private boolean isIgnore(File f)
	{
		for (String s : ignore_list)
		{
			if (f.getName().matches(s)) return true;
		}
		return false;
	}
	
	/*
	 * Загрзка настроек.
	 * Нужен файл ini и в нем блоки - [delete], [history], [ignore]
	 */
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
