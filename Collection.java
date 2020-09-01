import java.util.ArrayList;

public class Collection<T extends ColInterface> {

	private ArrayList<T> ls = new ArrayList<T>();

	public Collection() {;}

	public ArrayList<T> getls() {return ls;}

	public void add_object(T ob) {ls.add(ob);}

	public void print() {
		if (ls.isEmpty()) System.out.println("List is empty!");
		else for (T ob : ls)
			System.out.println("\n" + ob);
	}

	public T getob(int code) {
		for (T ob : ls)
			if (ob.getCode() == code) return ob;
		return null;
	}

	public boolean hasob(int code) {
		for (T ob : ls)
			if (ob.getCode() == code) return true;
		return false;
	}
}