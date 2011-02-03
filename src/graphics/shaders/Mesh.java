/**
 * Defines a mesh for a 3D Object.
 * Mesh consists of triangular faces with normals
 */

package graphics.shaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.StringTokenizer;

import android.content.Context;
import android.util.Log;

public class Mesh {
	/*************************
	 * PROPERTIES
	 ************************/
	int meshID; // The id of the stored mesh file (raw resource)
	
	// Constants
	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int SHORT_SIZE_BYTES = 2;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
	
	// Vertices
	private float _vertices[];
	
	// Indices
	private short _indices[];
	
	// Buffer [index + vertex]
	private FloatBuffer _vb;
	private ShortBuffer _ib;
	private int[] vb;
	private int[] ib;
	
	// Normals
	
	Context activity; 
	
	
	/***************************
	 * CONSTRUCTOR(S)
	 **************************/
	public Mesh() {
		
	}
	
	public Mesh(int meshID) {
		this(meshID, null);
	}
	
	public Mesh(int meshID, Context activity) {
		this.meshID = meshID;
		this.activity = activity;
		
		loadFile();
	}
	
	/**************************
	 * OTHER METHODS
	 *************************/
	
	/**
	 * Loads the .off file
	 * 
	 * OFF FORMAT:
	 * ------------
	 * Line 1
		OFF
	   Line 2
		vertex_count face_count edge_count
	   One line for each vertex:
		x y z 
		for vertex 0, 1, ..., vertex_count-1
	   One line for each polygonal face:
		n v1 v2 ... vn, 
		the number of vertices, and the vertex indices for each face.
	 * 
	 * 
	 * @return 1 if file was loaded properly, 0 if not 
	 */
	private int loadFile() {
		Log.d("Start-loadFile", "Starting loadFile");
		try {
			// Read the file from the resource
			Log.d("loadFile", "Trying to buffer read");
			InputStream inputStream = activity.getResources().openRawResource(meshID);
			
			// setup Bufferedreader
		    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
		    
		    // Try to parse the file
		    Log.d("loadFile", "Trying to buffer read2");
		    String str = in.readLine();
		  
		    // Make sure it's a .OFF file
		    //if (!str.equals("OFF"))
		    	//throw new Exception("NOT OFF!!");
		    
		    /* read # of vertices, faces, edges */
		    str = in.readLine();
		    Log.d("STR", str);
		    
		    // tokenizer based on space
		    StringTokenizer tokenizer = new StringTokenizer(str);
		    int _numVertices = Integer.parseInt(tokenizer.nextToken());
		    int _numFaces = Integer.parseInt(tokenizer.nextToken());
		    //int _numEdges = Integer.parseInt(tokenizer.nextToken());
		    
		    // read vertices
		    _vertices = new float[_numVertices * 3]; 
		    int i = 0;
		    for (i = 0; i < _numVertices; i++) {
		    	 str = in.readLine();
		    	
		    	 // tokenizer based on space
		    	 tokenizer = new StringTokenizer(str);
		    	 _vertices[i * 3 + 0] = Float.parseFloat(tokenizer.nextToken());
		    	 _vertices[i * 3 + 1] = Float.parseFloat(tokenizer.nextToken());
		    	 _vertices[i * 3 + 2] = Float.parseFloat(tokenizer.nextToken());
		    	 Log.d("Str vertices:", _vertices[i * 3 + 0] + "," + _vertices[i * 3 + 1] + "," + _vertices[i * 3 + 2]);
		    }
		    
		    Log.d("ReadFile", "Read vertices");
		    
		    // read faces and setup the index buffer
		    _indices = new short[_numFaces * 3];
		    
		    for (i = 0; i < _numFaces; i++) {
		    	 str = in.readLine();
		    	 // tokenizer based on space
		    	 tokenizer = new StringTokenizer(str);
		    	 // number of vertices for the face - make sure it's 3! [Might add support for 4 later]
		    	 short numV = Byte.parseByte(tokenizer.nextToken());
		    	 if (numV != 3)
		    		 throw new IOException("TEST!!");
		    	 
		    	 short firstV = Short.parseShort(tokenizer.nextToken());
		    	 short secondV = Short.parseShort(tokenizer.nextToken());
		    	 short thirdV = Short.parseShort(tokenizer.nextToken());
		    	 
		    	 // Store in the index buffer
		    	 _indices[i * 3 + 0] = firstV;
		    	 _indices[i * 3 + 1] = secondV;
		    	 _indices[i * 3 + 2] = thirdV;
		    }
		    
		    // Generate your vertex and index buffers
		    // vertex buffer
		    _vb = ByteBuffer.allocateDirect(_vertices.length
	                 * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		    _vb.put(_vertices);
		    _vb.position(0);
		    
		    // index buffer
	        _ib = ByteBuffer.allocateDirect(_indices.length
	                 * SHORT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asShortBuffer();
		    _ib.put(_indices);
		    _ib.position(0);
	        
		    Log.d("loadFile - size", _indices.length/3 + "," + _vertices.length);
	        // close the reader
		    in.close();
		    
		} catch (Exception e) {
			Log.d("Error-LoadFile", "FOUND ERROR: " + e.toString());
			return 0;
		}
		
		return 1;
	}

	
	/***************************
	 * GET/SET
	 *************************/
	
	public int getMeshID() {
		return meshID;
	}

	public void setMeshID(int meshID) {
		this.meshID = meshID;
	}

	public float[] get_vertices() {
		return _vertices;
	}

	public void set_vertices(float[] _vertices) {
		this._vertices = _vertices;
	}
	public short[] get_indices() {
		return _indices;
	}

	public FloatBuffer get_vb() {
		return this._vb;
	}
	public ShortBuffer get_ib() {
		return this._ib;
	}

}
