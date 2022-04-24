import java.util.*;

public class JavaHashSet implements HS_Interface {
  private HashSet<String> set;

  public JavaHashSet (int numBuckets) {
		set = new HashSet<String>(numBuckets);
	}

  public boolean add (String key) {
    return set.add(key);
  }

  public boolean remove (String key) {
    return set.remove(key);
  }

  public boolean contains (String key) {
    return set.contains(key);
  }

  public void clear () {
    set.clear();
  }

  public int size() {
    return set.size();
  }

  public boolean isEmpty () {
    return set.isEmpty();
  }
}
