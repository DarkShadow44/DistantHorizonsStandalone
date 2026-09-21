package com.seibel.distanthorizons.core.util.objects.pooling;

import com.seibel.distanthorizons.core.logging.DhLogger;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import it.unimi.dsi.fastutil.chars.CharArrayList;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

/**
 * A thread-safe string pool backed by a trie.
 * 
 * @link https://en.wikipedia.org/wiki/Trie
 * @link https://claude.ai/share/eb7fddbe-03a0-4562-88a0-a089b7a52006
 */
public class StringPool
{
	private static final DhLogger LOGGER = new DhLoggerBuilder().build(); 
	public static final StringPool INSTANCE = new StringPool();
	
	private final TrieNode root = new TrieNode();
	
	
	
	//=============//
	// constructor //
	//=============//
	///region
	
	// for now we only need a single INSTANCE,
	// if that changes in the future we can change this up
	private StringPool() {}
	
	///endregion
	
	
	
	//===================//
	// get pooled string //
	//===================//
	///region
	
	/**
	 * Returns a pooled String instance for the given character array.
	 * If an equivalent string already exists in the pool, returns that instance.
	 * Otherwise, creates a new String and adds it to the pool.
	 *
	 * @param chars the character array to convert to a pooled string
	 * @return a pooled String instance
	 */
	public String getPooledString(CharArrayList chars) { return this.getPooledString(chars, 0, chars.size()); }
	
	/**
	 * Returns a pooled String instance for a substring of the given character array.
	 *
	 * @param chars the character array
	 * @param offset the starting offset
	 * @param length the number of characters to use
	 * @return a pooled String instance
	 */
	public String getPooledString(CharArrayList chars, int offset, int length)
	{
		if (length == 0)
		{
			return "";
		}
		
		TrieNode currentNode = this.root;
		
		// Navigate/create the trie path
		for (int i = 0; i < length; i++)
		{
			char c = chars.getChar(offset + i);
			currentNode = currentNode.getOrCreateChild(c);
		}
		
		// Get or set the string at the leaf node
		return currentNode.getOrSetString(chars, offset, length);
	}
	
	///endregion
	
	
	
	//================//
	// helper methods //
	//================//
	///region
	
	public void clear() { this.root.clear(); }
	
	/**
	 * Returns an approximate count of pooled strings.
	 * Note: This is a best-effort count and may not be exact in concurrent scenarios.
	 */
	public long approximateSize() { return this.root.countStrings(); }
	
	///endregion
	
	
	
	//================//
	// helper classes //
	//================//
	///region
	
	/**
	 * Node in the trie structure. Uses a {@link StampedLock} for efficient concurrent access
	 * with optimistic reads for the common use case where the node already exists.
	 */
	private static class TrieNode
	{
		private volatile ConcurrentHashMap<Character, TrieNode> children;
		private volatile String value;
		private final StampedLock lock = new StampedLock();
		
		
		
		//================//
		// node interface //
		//================//
		///region
		
		TrieNode getOrCreateChild(char inputChar)
		{
			// Optimistic read - try without locking first
			long stamp = this.lock.tryOptimisticRead();
			ConcurrentHashMap<Character, TrieNode> currentChildren = this.children;
			
			// check for an existing child node
			if (stamp != 0 // zero means exclusively locked
				&& this.lock.validate(stamp) 
				&& currentChildren != null)
			{
				TrieNode child = currentChildren.get(inputChar);
				if (child != null)
				{
					return child;
				}
			}
			
			// we need to acquire a read lock to safely check again
			stamp = this.lock.readLock();
			try
			{
				if (this.children != null)
				{
					TrieNode child = this.children.get(inputChar);
					if (child != null)
					{
						return child;
					}
				}
				
				// Upgrade to write lock to create the child
				long writeStamp = this.lock.tryConvertToWriteLock(stamp);
				if (writeStamp == 0)
				{
					this.lock.unlockRead(stamp);
					writeStamp = this.lock.writeLock();
				}
				stamp = writeStamp;
				
				// add the missing node if needed (check in case of concurrency)
				if (this.children == null)
				{
					this.children = new ConcurrentHashMap<>();
				}
				return this.children.computeIfAbsent(inputChar, newChar -> new TrieNode());
			}
			finally
			{
				this.lock.unlock(stamp);
			}
		}
		
		String getOrSetString(CharArrayList chars, int offset, int length)
		{
			// Optimistic read
			long stamp = this.lock.tryOptimisticRead();
			String currentValue = this.value;
			
			if (stamp != 0 
				&& this.lock.validate(stamp) 
				&& currentValue != null)
			{
				return currentValue;
			}
			
			// Acquire read lock
			stamp = this.lock.readLock();
			try
			{
				if (this.value != null)
				{
					return this.value;
				}
				
				// Upgrade to write lock
				long writeStamp = this.lock.tryConvertToWriteLock(stamp);
				if (writeStamp == 0)
				{
					// already locked by another thread, wait
					this.lock.unlockRead(stamp);
					writeStamp = this.lock.writeLock();
				}
				stamp = writeStamp;
				
				// Double-check
				if (this.value != null)
				{
					return this.value;
				}
				
				// create our newly pooled string
				this.value = new String(chars.elements(), offset, length);
				return this.value;
			}
			finally
			{
				this.lock.unlock(stamp);
			}
		}
		
		///endregion
		
		
		
		//================//
		// helper methods //
		//================//
		///region
		
		void clear()
		{
			long stamp = this.lock.writeLock();
			try
			{
				if (this.children != null)
				{
					this.children.clear();
				}
				this.children = null;
				this.value = null;
			}
			finally
			{
				this.lock.unlock(stamp);
			}
		}
		
		long countStrings()
		{
			long stamp = this.lock.tryOptimisticRead();
			ConcurrentHashMap<Character, TrieNode> currentChildren = this.children;
			String currentValue = this.value;
			
			if (!this.lock.validate(stamp))
			{
				stamp = this.lock.readLock();
				try
				{
					currentChildren = this.children;
					currentValue = this.value;
				}
				finally
				{
					this.lock.unlockRead(stamp);
				}
			}
			
			long count = currentValue != null ? 1 : 0;
			if (currentChildren != null)
			{
				for (TrieNode child : currentChildren.values())
				{
					count += child.countStrings();
				}
			}
			return count;
		}
		
		///endregion
		
	}
	
	///endregion
	
	
	
}

