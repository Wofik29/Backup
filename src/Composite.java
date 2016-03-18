import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


public class Composite extends Component {

	List<Component> childs = new ArrayList<Component>();
	
	public Composite(String name, String path, long m)
	{
		isLeaf = false;
		this.name = name;
		this.path = path;
		modified = m;
	}	
	
	public void add(Component c)
	{
		childs.add(c);
	}
	
	public void remove(Component c)
	{
		childs.remove(c);
	}
	
	public boolean delete()
	{
		for (Component c : childs)
		{
			if (c.isLeaf)
			{
				File f = new File(c.path);
				f.delete();
			}
			else
			{
				c.delete();
			}
		}

		childs.clear();
		File f = new File(path);
		return f.delete();
	}


	/*
	 * Копирует данный узел со всей внутренностью в указанный путь
	 */
	public void copy(String path) throws IOException 
	{
		if (path.substring(path.length()-1).equals("/")) path = new String(path.substring(path.length()-1));
		File target = new File(path);
		
		if (!target.exists()) 
		{
			target.mkdirs();
		}
		
		for (Component c : childs)
		{
			if (!c.isLeaf)
			{
				c.copy(path+"/"+c.name);
				
			}
			else
			{
				try
				{
					File f = new File(path+"/"+c.name);
					if (f.exists()) f.delete();
					Files.copy(	Paths.get(c.path), f.toPath());
					f.setLastModified(c.modified);
				}
				catch( Exception ex)
				{
					throw ex;
				}
			}
		}
	}

	public int size()
	{
		return childs.size();
	}

	/*
	 * Создает дерево папок/файлов для данного узла
	 */
	public void createTree()
	{
		childs.clear();
		File main_folder = new File(path);

		for (File f : main_folder.listFiles() )
		{
			if (f.isDirectory())
			{
				Component folder = new Composite(f.getName(), f.getAbsolutePath(), f.lastModified());
				add(folder);
				folder.createTree();
			}
			else
			{
				Component fl = new Leaf(f.getName(), f.getAbsolutePath(), f.lastModified());
				add(fl);
			}
		}
		sort();
	}

	public Iterator<Component> getIterator()
	{
		return childs.iterator();
	}

	public void sort()
	{
		Collections.sort(childs,
				new Comparator<Component>()
				{
					public int compare(Component o1, Component o2)
					{
						return o1.name.compareTo(o2.name);
					}
				}
		);
	}


	/* 
	 * Метод чисто проверяет, равны ли компоненты.
	 * Не дает понять, какой из файлов не равен.
	 */
	public boolean equals(Component c)
	{
		// Если папку сравниваем с фалом, то однозначно
		if (c.isLeaf)	return false;

		if (size() != c.size())
		{
			System.out.println("not size : "+c.path);
			return false;
		}

		Iterator<Component> it = childs.iterator();
		Iterator<Component> alien_it = c.getIterator();

		Component child;

		while (it.hasNext())
		{
			child = it.next();

			if (alien_it.hasNext())
			{
				Component alien_child = alien_it.next();
				if (!child.equals(alien_child))
				{
					return false;
				}
			}
			else
			{
				return false;
			}
		}

		if (alien_it.hasNext())
		{
			return false;
		}

		return true;
	}


	public String equals_s(Component c)
	{
		// Если папку сравниваем с фалом, то однозначно
		if (c.isLeaf) return "Component is leaf! : "+c.path;

		if (size() != c.size())
		{
			return "Components not equals is size : "+c.path;
		}

		Iterator<Component> it = childs.iterator();
		Iterator<Component> alien_it = c.getIterator();

		Component child;
		
		while (it.hasNext())
		{
			child = it.next();
			
			if (alien_it.hasNext())
			{
				Component alien_child = alien_it.next();
				if (!child.equals(alien_child))
				{
					return child.path+" != "+alien_child.path;
				}
			}
			else
			{
				return "Components not equals is size";
			}
		}

		if (alien_it.hasNext())
		{
			return "Components not equals is size";
		}

		return "Equals! : "+path+" ---> "+c.path;
	}

	public void print(int l)
	{
		int i=-1;
		while (i++<l) System.out.print(" ");
		System.out.println("/"+name);
		
		for (Component c : childs)
		{
			System.out.println(c.name);
		}
	}
}
