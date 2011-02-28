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

	// the number of elements for each vertex
	// [coordx, coordy, coordz, normalx, normaly, normalz....]
	private final int VERTEX_ARRAY_SIZE = 6;

	// Vertices
	private float _vertices[];

	// Indices
	private short _indices[];

	// Buffer [index + vertex]
	private FloatBuffer _vb;
	private ShortBuffer _ib;

	// Normals
	private float[] _faceNormals;
	private int[]   _surroundingFaces; // # of surrounding faces for each vertex

	// Store the context
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

			// read vertices - going to store vertex coordinates + normals
			_vertices = new float[_numVertices * this.VERTEX_ARRAY_SIZE]; 
			int i = 0;
			for (i = 0; i < _numVertices; i++) {
				str = in.readLine();

				// tokenizer based on space
				tokenizer = new StringTokenizer(str);
				_vertices[i * this.VERTEX_ARRAY_SIZE]     = Float.parseFloat(tokenizer.nextToken());
				_vertices[i * this.VERTEX_ARRAY_SIZE + 1] = Float.parseFloat(tokenizer.nextToken());
				_vertices[i * this.VERTEX_ARRAY_SIZE + 2] = Float.parseFloat(tokenizer.nextToken());
				Log.d("Str vertices:", _vertices[i * this.VERTEX_ARRAY_SIZE + 0] + "," + _vertices[i * this.VERTEX_ARRAY_SIZE + 1] + "," + _vertices[i * this.VERTEX_ARRAY_SIZE + 2]);
			}

			Log.d("ReadFile", "Read vertices");

			// read faces and setup the index buffer
			// array size
			int arraySize = _numFaces * 3;
			_indices = new short[arraySize];

			// setup the face normals
			_faceNormals = new float[arraySize]; // NEEDED?
			_surroundingFaces = new int[_numVertices]; // # of surrounding faces for each vertex

			// initialize to 0
			for(int x = 0; x < _numVertices; x++) {
				_vertices[x * this.VERTEX_ARRAY_SIZE + 3] = 0;
				_vertices[x * this.VERTEX_ARRAY_SIZE + 4] = 0;
				_vertices[x * this.VERTEX_ARRAY_SIZE + 5] = 0;
				_surroundingFaces[x] = 0;
			}

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

				Log.d("Reading Faces:", _indices[i * 3 + 0] + " " + _indices[i * 3 + 1] + " " + _indices[i * 3 + 2]);

				// Calculate the face normal
				setFaceNormal(i, firstV, secondV, thirdV);
			}

			// finally calculate the exact vertex normals
			for(int x = 0; x < _numVertices; x++) {
				_vertices[x * this.VERTEX_ARRAY_SIZE + 3] /= _surroundingFaces[x];
				_vertices[x * this.VERTEX_ARRAY_SIZE + 4] /= _surroundingFaces[x];
				_vertices[x * this.VERTEX_ARRAY_SIZE + 5] /= _surroundingFaces[x];

				Log.d("SurroundingFaces: ", x + ": " + _surroundingFaces[x]);

				Log.d("Exact normal: ", _vertices[x * this.VERTEX_ARRAY_SIZE + 3] + "," + _vertices[x * this.VERTEX_ARRAY_SIZE + 4] + "," + _vertices[x * this.VERTEX_ARRAY_SIZE + 5]);
			}

			// TEMPORARY
			/*_vertices[0 * this.VERTEX_ARRAY_SIZE + 3] = 0.0f;
			_vertices[0 * this.VERTEX_ARRAY_SIZE + 4] = 0.0f;
			_vertices[0 * this.VERTEX_ARRAY_SIZE + 5] = 1.0f;
			
			_vertices[1 * this.VERTEX_ARRAY_SIZE + 3] = -1.0f;
			_vertices[1 * this.VERTEX_ARRAY_SIZE + 4] = 1.0f;
			_vertices[1 * this.VERTEX_ARRAY_SIZE + 5] = 1.0f;
			
			_vertices[2 * this.VERTEX_ARRAY_SIZE + 3] = -1.0f;
			_vertices[2 * this.VERTEX_ARRAY_SIZE + 4] = -1.0f;
			_vertices[2 * this.VERTEX_ARRAY_SIZE + 5] = 1.0f;
			
			_vertices[3 * this.VERTEX_ARRAY_SIZE + 3] = 1.0f;
			_vertices[3 * this.VERTEX_ARRAY_SIZE + 4] = -1.0f;
			_vertices[3 * this.VERTEX_ARRAY_SIZE + 5] = 1.0f;
			
			_vertices[4 * this.VERTEX_ARRAY_SIZE + 3] = 1.0f;
			_vertices[4 * this.VERTEX_ARRAY_SIZE + 4] = 1.0f;
			_vertices[4 * this.VERTEX_ARRAY_SIZE + 5] = -1.0f;
			
			_vertices[5 * this.VERTEX_ARRAY_SIZE + 3] = -1.0f;
			_vertices[5 * this.VERTEX_ARRAY_SIZE + 4] = 1.0f;
			_vertices[5 * this.VERTEX_ARRAY_SIZE + 5] = -1.0f;
			
			_vertices[6 * this.VERTEX_ARRAY_SIZE + 3] = -1.0f;
			_vertices[6 * this.VERTEX_ARRAY_SIZE + 4] = -1.0f;
			_vertices[6 * this.VERTEX_ARRAY_SIZE + 5] = -1.0f;
			
			_vertices[7 * this.VERTEX_ARRAY_SIZE + 3] = 1.0f;
			_vertices[7 * this.VERTEX_ARRAY_SIZE + 4] = -1.0f;
			_vertices[7 * this.VERTEX_ARRAY_SIZE + 5] = -1.0f;*/
			// END TEMPORARY


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

	/**
	 * Sets the face normal of the i'th face
	 * @param i the index of the face
	 * @param firstV first vertex of the triangle
	 * @param secondV second vertex of the triangle
	 * @param thirdV third vertex of the triangle
	 */
	private void setFaceNormal(int i, int firstV, int secondV, int thirdV) {
		// get coordinates of all the vertices
		float v1[] = {_vertices[firstV * VERTEX_ARRAY_SIZE], _vertices[firstV * VERTEX_ARRAY_SIZE + 1], _vertices[firstV * VERTEX_ARRAY_SIZE + 2]};
		float v2[] = {_vertices[secondV * VERTEX_ARRAY_SIZE], _vertices[secondV * VERTEX_ARRAY_SIZE + 1], _vertices[secondV * VERTEX_ARRAY_SIZE + 2]};
		float v3[] = {_vertices[thirdV * VERTEX_ARRAY_SIZE], _vertices[thirdV * VERTEX_ARRAY_SIZE + 1], _vertices[thirdV * VERTEX_ARRAY_SIZE + 2]};

		// calculate the cross product of v1-v2 and v2-v3
		float v1v2[] = {v1[0]-v2[0], v1[1]-v2[1], v1[2]-v2[2]};
		//float v3v2[] = {v2[0]-v3[0], v2[1]-v3[1], v2[2]-v3[2]};
		float v3v2[] = {v3[0]-v2[0], v3[1]-v2[1], v3[2]-v2[2]};

		Log.d("V1V2: ", v1v2[0] + "," + v1v2[1] + "," + v1v2[2]);
		Log.d("V3V2: ", v3v2[0] + "," + v3v2[1] + "," + v3v2[2]);
		float cp[] = crossProduct(v1v2, v3v2);

		// try normalizing here
		float sqrt = (float)Math.sqrt(cp[0] * cp[0] +
				cp[1] * cp[1] +
				cp[2] * cp[2]);

		cp[0] /= sqrt;
		cp[1] /= sqrt;
		cp[2] /= sqrt;
		
		if (cp[0] == -0.0f)
			cp[0] = 0.0f;
		if (cp[1] == -0.0f)
			cp[1] = 0.0f;
		if (cp[2] == -0.0f)
			cp[2] = 0.0f;
		// end normalizing

		// set the normal
		_faceNormals[i * 3]     = cp[0];
		_faceNormals[i * 3 + 1] = cp[1];
		_faceNormals[i * 3 + 2] = cp[2];
		Log.d("NORMAL:", cp[0] + "," + cp[1] + "," + cp[2]);

		// Setup for vertex normal construction;
		_vertices[firstV * this.VERTEX_ARRAY_SIZE + 3] += _faceNormals[i * 3];
		_vertices[firstV * this.VERTEX_ARRAY_SIZE + 4] += _faceNormals[i * 3 + 1];
		_vertices[firstV * this.VERTEX_ARRAY_SIZE + 5] += _faceNormals[i * 3 + 2];

		_vertices[secondV * this.VERTEX_ARRAY_SIZE + 3] += _faceNormals[i * 3];
		_vertices[secondV * this.VERTEX_ARRAY_SIZE + 4] += _faceNormals[i * 3 + 1];
		_vertices[secondV * this.VERTEX_ARRAY_SIZE + 5] += _faceNormals[i * 3 + 2];

		_vertices[thirdV * this.VERTEX_ARRAY_SIZE + 3] += _faceNormals[i * 3];
		_vertices[thirdV * this.VERTEX_ARRAY_SIZE + 4] += _faceNormals[i * 3 + 1];
		_vertices[thirdV * this.VERTEX_ARRAY_SIZE + 5] += _faceNormals[i * 3 + 2];

		// increment # of faces around the vertex
		_surroundingFaces[firstV]++;
		_surroundingFaces[secondV]++;
		_surroundingFaces[thirdV]++;
	}

	/**
	 * Calculates the cross product of two 3d vectors
	 */
	public float[] crossProduct(float[] v0, float[] v1)
	{
		float crossProduct[] = new float[3];

		crossProduct[0] = v0[1] * v1[2] - v0[2] * v1[1];
		crossProduct[1] = v0[2] * v1[0] - v0[0] * v1[2];
		crossProduct[2] = v0[0] * v1[1] - v0[1] * v1[0];

		return crossProduct;
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
