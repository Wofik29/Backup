
public class Leaf extends Component {

	public Leaf(String name, String path, long m)
	{
		isLeaf = true;
		this.name = name;
		this.path = path;
		modified = m;
	}
	
	public void print(int l)
	{
		int i=-1;
		while (i++<l) System.out.print(" ");
		System.out.println("--"+name);
	}
	
	public boolean equals(Component c)
	{
		if (!c.isLeaf) return false;
		
		int result = name.compareTo(c.name);
		if (result == 0) 
		{
			if (modified == c.modified) return true;
			else return false;
		}
		else
		{
			return false;
		}
	
	}
}