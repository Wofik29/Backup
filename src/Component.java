import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;


public abstract class Component{

	protected String name;
	protected long modified;
	protected boolean isLeaf;
	protected String path;
	
	public void add(Component c)
	{
		throw new UnsupportedOperationException();
	}
	
	public void remove(Component c)
	{
		throw new UnsupportedOperationException();
	}
	
	public Iterator<Component> getIterator()
	{
		throw new UnsupportedOperationException();
	}
	
	public void sort()
	{
		throw new UnsupportedOperationException();
	}
	
	public int size()
	{
		throw new UnsupportedOperationException();
	}
	
	// Копирует самого себя в path. Path должна быть директорией
	public void copy(String path) throws IOException
	{
		throw new UnsupportedOperationException();
	}
	
	// Создание списка файлов в самом себе
	public void createTree()
	{
		throw new UnsupportedOperationException();
	}
	
	public boolean hasLeaf()
	{
		return isLeaf;
	}
	
	public long getModified()
	{
		return modified;
	}
		
	public String getPath()
	{
		return path;
	}
	
	public boolean delete()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString()
	{
		return path;
	}
	
	// Свой метод сравнения, не имеет отношения к библиотечной функции, просто так же назван.
	public int compareTo(Component c)
	{
		return name.compareTo(c.name);
	}
	public abstract boolean equals(Component c);
	public abstract void print(int l);
}
