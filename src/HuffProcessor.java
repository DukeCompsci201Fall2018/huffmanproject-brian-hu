
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
	public enum Header{TREE_HEADER, COUNT_HEADER};
	public Header myHeader = Header.TREE_HEADER;

	
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

		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}
		out.close();
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
		if (temp != HUFF_TREE && temp != HUFF_NUMBER) {
			throw new HuffException("wrong huff number");
		}
		HuffNode root = readTreeHeader(in);
		HuffNode current = root;
		int bits;
		while (true){
			bits = in.readBits(1);
			if(bits == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF");
			}
			if(bits == 0) {
				current = current.getLeft();
			}else if (bits == 1) {
				current = current.getRight();
			}
            	if(current.getLeft() == null && current.getRight() == null) {
            		if (current.getValue() == PSEUDO_EOF) {
            			break;
            		}else {
            			out.writeBits(BITS_PER_WORD, current.getValue());
            			current = root;
            		}
            	} 
        }

//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
//		out.close();
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
		else if (bits == 1) {
			return new HuffNode(bis.readBits(BITS_PER_WORD + 1), 0);
		}
		return new HuffNode(bis.readBits(BITS_PER_WORD + 1), 0);
	}
}