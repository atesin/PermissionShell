package cl.netgamer.permissionshell;

public class Args
{
	private String[] args;
	private int index;
	
	Args(final String[] args)
	{
		this.index = 0;
		this.args = args;
	}
	
	
	String shift()
	{
		if (this.index < this.args.length)
			return this.args[this.index++].toLowerCase();
		
		return "";
	}
	
}
