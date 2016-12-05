import java.util.LinkedList;

public class Piece {

	private int size;
	private byte[] data;
	private int blocks;
	private boolean[] recievedBlocks;
	private LinkedList<Integer[]> requestQueue = new LinkedList<>();
	private int blocksize;
	private int index;

	public Piece(int index, long totalBytes, int blocksize, long piecesize) {
		this.blocksize = blocksize;
		this.index = index;
		size = (int) Math.min(piecesize, totalBytes - index * piecesize);
		data = new byte[size];
		blocks = (size + blocksize - 1) / blocksize;
		recievedBlocks = new boolean[blocks];
		for(int i = 0; i < blocks; i++) {
			recievedBlocks[i] = false;
			int length = Math.min(blocksize, size - i * blocksize);
			requestQueue.add(new Integer[] {i, length});
		}
	}
	
	public int getIndex() {
		return index;
	}
	
	public int[] popRequest() {
		Integer[] a = requestQueue.removeFirst();
		return new int[] {a[0], a[1]};
	}
	
	public int queueSize() {
		return requestQueue.size();
	}
	
	public void putBlock(int begin, byte[] data) {
		for(int i = 0; i < data.length; i++)
			this.data[begin + i] = data[i];
		recievedBlocks[begin / blocksize] = true;
	}
	
	public boolean recievedAllBlocks() {
		for(boolean b : recievedBlocks)
			if(! b)
				return false;
		return true;
	}
	
	public byte[] getData() {
		return data;
	}
}
