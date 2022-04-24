public interface HS_Interface
{
/* 	INSIDE THE ADD FUNCTION YOU MUST TEST FOR FULL AND CALL AN UPSIZE METHOD LIKE ARRBAG DID

	ADD THE INCOMING KEY TO THE HASHSET THEN AT THE BOTTOM OF THE METHOD YOU MUST DO 
	SOMETHING LIKE THIS. YOU MIGHT HAVE DIFFERENT INTERNAL VARIABLE NAMES BUT IT SHOULD 
	BE SIMILAR TO THIS (AFTER ADDING IN THE KEY)
	
	// AT VERY BOTTOM OF ADD METHOD:
	if ( size > MAX_ACCEPTABLE_AVE_BUCKET_SIZE * this.numBuckets)
			upSize_ReHash_AllKeys(); 
	return true;

	THIS MEANS YOU CREATE A NEW BACKBONE ARRAY OF LINKED LISTS BUT -BEFORE- YOU LEAVE .add 
	FUNCTION YOU WALK THRU EVERY LIST IN THE OLD ARRAY AND RE-HASH ALL THOSE KEYS BACK INTO THE 
	NEW BIGGER ARRAY OF LIINKED LISTS

	LOOK AT THE EXPECTED OUTPUT FOR THIS PROJECT - YOU *MUST* DO THE UPSIZING
*/	

	public boolean add( String key ); // dupes must be rejected and return false
   
	public boolean remove( String key ); // if not found return false else remove & return true
   
	public boolean contains( String key ); // true if foound false if not
   
	public boolean isEmpty(); // use the call to size
   
	public int size(); // number of keys currently stored in the container
   
   	public void clear();
} // end DictionaryInterface
