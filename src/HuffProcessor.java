import java.util.*;
/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;


	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root,out);
		
		in.reset();
		writeCompressedBits(in,codings,out);
		out.close();
	}
	
	public int[] readForCounts(BitInputStream in) {
		int[] freq = new int[ALPH_SIZE + 1];
		freq[PSEUDO_EOF] = 1;
		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			freq[val] += 1;
		}
		return freq;
	}
	
	public HuffNode makeTreeFromCounts(int[] freq) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		
		for(int n=0; n < freq.length; n++){
			if(freq[n]>0){
			pq.add(new HuffNode(n,freq[n],null,null));
			}
		}
		
		while (pq.size() > 1) {
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode t = new HuffNode(0,left.myWeight+right.myWeight,left,right);
			pq.add(t);
			}
		HuffNode root = pq.remove();
		return root;
		}
	
	public String[] makeCodingsFromTree(HuffNode root) {
		
		String[] codings = new String[ALPH_SIZE + 1];
		codings = codingHelper(root,"", codings);
		return codings;
	}
	
	public String[] codingHelper(HuffNode root, String s,String[] codings) {
		
		if(root.myLeft == null && root.myRight== null) codings[root.myValue] = s;
		
		if(root.myLeft != null) {
			codingHelper(root.myLeft, s + "0",codings);
		}
		
		if(root.myRight != null) {
			codingHelper(root.myRight, s + "1",codings);
		}
		
		return codings;
		}
	
	public void writeHeader(HuffNode root, BitOutputStream out) {
		if(root.myLeft == null && root.myRight == null) {
			out.write(1);
			out.write(root.myValue);
		}
		if(root.myLeft != null) {
			out.write(0);
			writeHeader(root.myLeft, out);
		}
		
		if(root.myRight != null) {
			out.write(0);
			writeHeader(root.myRight, out);
		}
	}
	
	public void writeCompressedBits(BitInputStream BIS, String[] codings, BitOutputStream BOS) {
		while(true) {
		int bits = BIS.readBits(BITS_PER_WORD);
		if(bits == -1) {
		BOS.writeBits(codings[PSEUDO_EOF].length(), Integer.parseInt(codings[PSEUDO_EOF],2));
		break;
		}
		String temp = codings[bits];
		BOS.writeBits(temp.length(), Integer.parseInt(temp, 2));
		}
		}

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
		int temp = in.readBits(BITS_PER_INT);
		if (temp != HUFF_TREE) {  
			throw new HuffException("illegal header starts with " +temp);
		}

		HuffNode root = readTreeHeader(in);
		HuffNode current = root;
		int bits;
		while (true){
			bits = in.readBits(1);
			if(bits == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF");
			}else {
				if(bits == 0) {
					current = current.myLeft;
				}else current = current.myRight;
	            
				if(current.myLeft == null && current.myRight == null) {
	            	if (current.myValue == PSEUDO_EOF) {
	            		break;
	            	}else {
	            		out.writeBits(BITS_PER_WORD, current.myValue);
	            		current = root;
	            		}
	            	}
			} 
        }
		out.close();


	}
	//helper method for decompress
	//reads the tree of the header
	public HuffNode readTreeHeader(BitInputStream bis) {
		int bits = bis.readBits(1);
		if(bits == -1) {
			throw new HuffException("no PSEUDO_EOF");
		}
		if (bits == 0) {
			HuffNode left = readTreeHeader(bis);
			HuffNode right = readTreeHeader(bis);
			HuffNode x = new HuffNode(0,0,left,right);
			return x;
		}
		else{
			return new HuffNode(bis.readBits(BITS_PER_WORD + 1), 0);
		}
	}
	

}
	
