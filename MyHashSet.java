import java.util.*;

public class MyHashSet implements HS_Interface {
	private int numBuckets; // changes over life of the hashset due to resizing the array
	private int nextResize;
	private Node[] bucketArray;
	private int size; // total # keys stored in set right now

	private int[][] xorTable;
	private final int hashMask = 1 << 30; // Used to check if the first bit is true
	private final int rotation = 31;
	private final int signFlipper = 0x7FFFFFFF;

	// THIS IS A TYPICAL AVERAGE BUCKET SIZE. IF YOU GET A LOT BIGGER THEN YOU ARE

	// Moving away from this number in favor of faster ways of checking against
	// this, but this number still holds, but it doesn't really matter honestly
	private static final double MAX_ACCEPTABLE_AVE_BUCKET_SIZE = 1; // Obviously my lucky number duh /s
	private static final int DEFAULT_SEED = 0xAA6342F2; // Favorite number of course, nothing up my sleeve

	public MyHashSet (int numBuckets) {
		this(numBuckets, DEFAULT_SEED);
	}

	public MyHashSet(int numBuckets, int seed) {
		genTable(256,  seed); 
		size = 0;
		nextResize = numBuckets / 4 * 3;
		this.numBuckets = numBuckets;
		bucketArray = new Node[numBuckets]; // array of linked lists
		// System.out.format(
		// 	"IN CONSTRUCTOR: INITIAL TABLE LENGTH=%d RESIZE WILL OCCUR EVERY TIME AVE BUCKET LENGTH EXCEEDS %f\n",
		// 	numBuckets, MAX_ACCEPTABLE_AVE_BUCKET_SIZE);
	}

	private void genTable(int size, long seed) {
		// Imagine having default parameters. Yea. Java kinda sucks in the QOL
		// department. No, I am not writing overload just for that.
		Random r = seed == -1 ? new Random() : new Random(seed);

		xorTable = new int[size][rotation];
    for (int i = 0; i < size; i++) {
			for (int j = 0; j < rotation; j++) {
				xorTable[i][j] = r.nextInt();
			}
    }
	}

	// Note this algorithm obviously sucks really bad for really long strings (like a paragraph), which would require the text to be blocked (not going to implement here)
	// and currently only takes lowercase english letters (which we have), but this weakness can be fixed by just expanding the precomputation
	private int hashOf(String key) // number can be whatever i want it to be
	{
		int hash = 0;

		char[] keyArr = key.toCharArray();

		for (int i = 0; i < keyArr.length; i++) {
			// Nvm don't need to rotate anymore ooga buuga precomputation is free (and gives better distribution)
			// hash = (hash << 1) ^ ((hash & hashMask) >> 30); 
			hash ^= xorTable[keyArr[i] - 'a'][i & rotation];
		}

		return hash;
	}

	public boolean add(String key) {

		// if ( size > MAX_ACCEPTABLE_AVE_BUCKET_SIZE * this.numBuckets)
		if (size > nextResize) { // Precomputation yay
			nextResize <<= 1;
			upSize_ReHash_AllKeys(); // DOUBLE THE ARRAY .length THEN REHASH ALL KEYS
		}

		// your code here to add the key to the table and ++ your size variable
		int fullHash = hashOf(key);

		// Finding the index
		int flippedHash = (signFlipper & fullHash); // Flip hash to always be positive
		int upsizeKey = flippedHash / numBuckets; // See Explanation in upsize function
		int index = flippedHash % numBuckets; // Using upsizeKey is apparently not faster....

		Node n = bucketArray[index];
 
		// Default place if nothing is in the bucket
		if (n == null) {
			bucketArray[index] = new Node(key, fullHash, upsizeKey, null);
			++size;
			return true;
		}

		// Check to place in front
		if (fullHash < n.fullHash) {
			// New node needs to be placed at the front
			bucketArray[index] = new Node(key, fullHash, upsizeKey, n);
			++size;
			return true;
		} else if (fullHash == n.fullHash && key.equals(n.data)) {
			// New Node is a duplicate of the head node
			return false;
		}
		while (n.next != null && fullHash >= n.next.fullHash) {
			if (fullHash == n.next.fullHash && key.equals(n.next.data)) {
				// New Node is a duplicate of n.next
				return false;
			}
			n = n.next;
		}

		n.next = new Node(key, fullHash, upsizeKey, n.next);
		++size;
		return true;
	}

	public boolean remove(String key) {
		int fullHash = hashOf(key);
		int index = (fullHash & signFlipper) % numBuckets;
		Node n = bucketArray[index];

		if (n == null) {
			return false;
		} else if (fullHash == n.fullHash && key.equals(n.data)) {
			// Found at head node
			--size;
			bucketArray[index] = n.next;
			return true;
		} else {
			while (n.next != null && fullHash >= n.next.fullHash) {
				if (fullHash == n.next.fullHash && key.equals(n.next.data)) {
					// Found at n.next
					--size;
					n.next = n.next.next;
					return true;
				}
				n = n.next;
			}
			return false;
		}
	}

	public void clear() {
		size = 0;
		bucketArray = new Node[numBuckets];
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public int size() {
		return size;
	}

	public boolean contains(String key) {
		int fullHash = hashOf(key);
		int index = (signFlipper & fullHash) % numBuckets;
		Node n = bucketArray[index];

		if (n == null) {
			return false;
		} else if (fullHash == n.fullHash && key.equals(n.data)) {
			// Found at head node
			return true;
		} else {
			while (n.next != null && fullHash >= n.next.fullHash) {
				if (fullHash == n.next.fullHash && key.equals(n.next.data)) {
					return true;
				}
				n = n.next;
			}
			return false;
		}
	}

	private void upSize_ReHash_AllKeys() {
		// System.out.format("KEYS HASHED=%10d UPSIZING TABLE FROM %8d to %8d REHASHING ALL KEYS\n", size, bucketArray.length, bucketArray.length * 2);

		// double startTime = System.currentTimeMillis();

		Node[] oldArr = bucketArray;
		bucketArray = new Node[this.numBuckets * 2];

		/*
		 * UPSIZING STRATEGY: Only works since each upsize exactly doubles the output
		 * Let n = original table size, then originally: index = fullHash % n
		 * When we upsize, index2 = fullHash % 2n
		 * 
		 * Note 0 <= index < n, and 0 <= index2 < 2n. index is also congruent to index2
		 * mod n
		 * Therefore must be the case that index2 is either index or index + n
		 * 
		 * To know whether the item should go into the index or index + n bucket, we
		 * look at the upsizeKey, which is fullHash / n
		 * d
		 * Note fullHash = n * upsizeKey + index
		 * 
		 * If this number is even, then fullHash = n * 2k + index where k is some
		 * integer, therefore fullHash % 2n = index
		 * 
		 * If this number is odd, then fullHash = n * (2k+1) + index = 2 * 2k + (n +
		 * index) where k is some integer
		 * 
		 * By multiplying the upsizeKey by 2 (left shift 1), the key is updated to the
		 * new bucket size, so we can use the same key the next time we upsize
		 * 
		 * This incidentally also shows that items originating from the index bucket can
		 * only end up in index or index + n bucket on the new table
		 * 
		 * It is also impossible for any other item on the original table that did not
		 * come from the index bucket to land in the index or index + n bucket on the
		 * new array
		 * 
		 * Using this fact we can redirect all nodes in the same bucket from the
		 * original table to one of two location without checking for sorting placement
		 */

		for (int i = 0; i < this.numBuckets; i++) {
			Node n = oldArr[i];

			// Locate new buckets
			Node lowerDestination = null;
			Node upperDestination = null;

			while (n != null) {
				if ((n.upsizeKey & 1) == 0) {
					// Lower Bucket
					if (lowerDestination == null) {
						bucketArray[i] = n;
					} else {
						lowerDestination.next = n;
					}
					lowerDestination = n;
				} else {
					// Upper Bucket
					if (upperDestination == null) {
						bucketArray[this.numBuckets + i] = n;
					} else {
						upperDestination.next = n;
					}
					upperDestination = n;
				}

				// Update upsize key to new bucket size
				n.upsizeKey >>= 1;

				n = n.next;
			}

			// Clear any remaining references on the tail nodes (so they don't point to their nexts from the original list)
			if (lowerDestination != null)
				lowerDestination.next = null;
			if (upperDestination != null)
				upperDestination.next = null;
		}

		this.numBuckets *= 2;

		// System.out.format(" Spent %4.0fms\n", (System.currentTimeMillis() - startTime));
	}

	public void printInternal() {
		// System.out.println("Keys: ");
		// for (int i = 0; i < xorTable.length; i++) {
		// 	System.out.printf("%c: %s (%s)\n", (char)('a' + i), printHex(xorTable['a' + i]), printBin(xorTable['a' + i]));
		// }

		double mean = size / numBuckets;
		double sum = 0f;

		System.out.println("\nContent: ");
		for (int i = 0; i < numBuckets; i++) {
			System.out.println(i);
			int count = 0;
			for (Node n = bucketArray[i]; n != null; n = n.next) {
				System.out.println("\t" + n);
				count++;
			}

			sum += (count - mean) * (count - mean);
		}

		System.out.println("StdDev = " + Math.sqrt(sum / numBuckets));
	}

	public static String printBin (int hash) {
		String res = "";
		for (int i = 0; i < 32; i++) {
			int cVal = hash & 1;
			res = cVal + res;
			hash >>= 1;
		}

		return res;
	}

	public static String printHex (int hash) {
		String res = "";
		for (int i = 0; i < 8; i++) {
			int cVal = hash & 15;
			res = (cVal < 10 ? (char)('0' + cVal) : (char)('A' + (cVal - 10))) + res;
			hash >>= 4;
		}

		return res;
	}

	public int forceCheckCount() {
		int counter = 0;
		for (int i = 0; i < numBuckets; i++) {
			for (Node n = bucketArray[i]; n != null; n = n.next) {
				counter++;
			}
		}
		return counter;
	}

	public int countIncorrectHashLocations() {
		int counter = 0;
		for (int i = 0; i < numBuckets; i++) {
			for (Node n = bucketArray[i]; n != null; n = n.next) {
				if (i != ((signFlipper & n.fullHash) % numBuckets)) {
					counter++;
				}
			}
		}

		return counter;
	}

} // END MyHashSet CLASS

class Node {
	String data;
	int fullHash;
	int upsizeKey;
	Node next;

	public Node(String data, int fullHash, int upsizeKey, Node next) {
		this.data = data;
		this.fullHash = fullHash;
		this.upsizeKey = upsizeKey;
		this.next = next;
	}

	@Override
	public String toString() {
		return String.format("%12s(%s)", data, MyHashSet.printHex(fullHash));
	}
}