/**
 * Class represents a 3D object. 
 * Consists of a mesh of triangles, any textures, lighting properties, etc. 
 */

package graphics.shaders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class Object3D {
	/*************************
	 * PROPERTIES
	 ************************/
	// Context
	Context context;
	
	// Constants
	private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 3 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
	
    // Mesh
	Mesh mesh; 						// The mesh of triangles
	int meshID;						// Mesh file (.OFF) from resources
	
	// texture
	private boolean hasTexture;
	private int texFile;
	private int _texID;
	
	// lighting
	
	// etc.
	
	/***************************
	 * CONSTRUCTOR(S)
	 **************************/
	public Object3D(int meshID, boolean hasTexture, Context context) {
		this(-1, meshID, hasTexture, context);
	}
	
	public Object3D(int texFile, int meshID, boolean hasTexture, Context context) {
		this.texFile = texFile;
		this.meshID = meshID;
		this.hasTexture = hasTexture;
		
		// the mesh
		mesh = new Mesh(meshID, context);
		
		// texture
		//setupTexture();
	} 
	
	/**************************
	 * OTHER METHODS
	 *************************/
	
	/*
	 * Draws the object
	 */
	public void draw(int positionHandle) {
		
		// the vertex buffer
		FloatBuffer vb = mesh.get_vb();
		float[] vertices = mesh.get_vertices();
		
		ShortBuffer ib = mesh.get_ib();
		short[] indices = mesh.get_indices();
		
		// Do binding and whatnot
		// generate buffers first
		int buffers[] = new int[2];
	    GLES20.glGenBuffers(2, buffers, 0);
	    
	    // bind vertex buffer
	    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
                     vertices.length * FLOAT_SIZE_BYTES,
                     vb,
                     GLES20.GL_STATIC_DRAW);
		
        // for indices
	    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[1]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
                     indices.length * 2,
                     ib,
                     GLES20.GL_STATIC_DRAW);
        
		// draw texture?
		if (this.hasTexture) {
			/*GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _texID);
			
			GLES20.glVertexAttribPointer(shader.getMaTextureHandle(), 2, GLES20.GL_FLOAT, false,
		                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, vb);
		    //checkGlError("glVertexAttribPointer maTextureHandle");
		    GLES20.glEnableVertexAttribArray(shader.getMaTextureHandle());*/
		}
        
		// attrib pointer for vertex buffer
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
		GLES20.glEnableVertexAttribArray(positionHandle);
		GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, vb);
		
		// Draw using index buffer
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length, GLES20.GL_UNSIGNED_SHORT, ib);
		
        //checkGlError("glVertexAttribPointer maPosition");
        //fb.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        //checkGlError("glEnableVertexAttribArray maPositionHandle");
     
        //checkGlError("glEnableVertexAttribArray maTextureHandle");
	}
	
	/*
	 * Calls the mesh draw functions
	 */
	public void drawMesh() {
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
		//GLES20.glDr
	}
	
	/*
	 * Sets up the texture
	 */
	public void setupTexture(Context context) {
		// create new texture ids
		int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        _texID = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _texID);

        // parameters
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);

        InputStream is = context.getResources()
            .openRawResource(texFile);
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(is);
        } finally {
            try {
                is.close();
            } catch(IOException e) {
                // Ignore.
            }
        }

        // create it 
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
	}

	
	
	
	/***************************
	 * GET/SET
	 *************************/
	public Mesh getMesh() {
		return mesh;
	}

	public void setMesh(Mesh mesh) {
		this.mesh = mesh;
	}

	public int getMeshID() {
		return meshID;
	}

	public void setMeshID(int meshID) {
		this.meshID = meshID;
	}

	public boolean hasTexture() {
		return hasTexture;
	}

	public void setHasTexture(boolean hasTexture) {
		this.hasTexture = hasTexture;
	}

	public int getTexFile() {
		return texFile;
	}

	public void setTexFile(int texFile) {
		this.texFile = texFile;
	}

	public int get_texID() {
		return _texID;
	}

	public void set_texID(int _texid) {
		_texID = _texid;
	}
	
}
