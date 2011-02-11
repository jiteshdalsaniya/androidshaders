/**
 * Represents a shader object
 */

package graphics.shaders;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

public class Shader {
	/************************
	 * PROPERTIES
	 **********************/
	
	// program/vertex/fragment handles
	private int _program, _vertexShader, _pixelShader;
	
	// other handles - position/texture/mvpmatrix/normal
	private int muMVPMatrixHandle;
    private int maPositionHandle;
    private int maNormalHandle;
    private int hasTextureHandle;   
    private int maTextureHandle;
	
	// The shaders
	private String _vertexS, _fragmentS;
	private int _vsID, _fsID; // the ids for the files to be read
	
	// does it have textures?
	private boolean hasTextures;
	private int numTextures;
	
    /************************
     * CONSTRUCTOR(S)
     *************************/
	public Shader() {
		
	}
	
	// Takes in Strings directly
	public Shader(String vertexS, String fragmentS, boolean hasTextures, int numTextures) {
		setup(vertexS, fragmentS, hasTextures, numTextures);
	}
	
	// Takes in ids for files to be read
	public Shader(int vID, int fID, Context context, boolean hasTextures, int numTextures) {
		StringBuffer vs = new StringBuffer();
		StringBuffer fs = new StringBuffer();
		
		// read the files
		try {
			// Read the file from the resource
			Log.d("loadFile", "Trying to read vs");
			// Read VS first
			InputStream inputStream = context.getResources().openRawResource(vID);
			// setup Bufferedreader
		    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
		    
		    String read = in.readLine();
		    while (read != null) {
		    	vs.append(read + "\n");
		    	read = in.readLine();
		    }
		    
		    vs.deleteCharAt(vs.length() - 1);
		    Log.d("StringBufferVS", vs.toString());
		    
		    // Now read FS
		    Log.d("loadFile", "Trying to read vs");
			// Read VS first
			inputStream = context.getResources().openRawResource(fID);
			// setup Bufferedreader
		    in = new BufferedReader(new InputStreamReader(inputStream));
		    
		    read = in.readLine();
		    while (read != null) {
		    	fs.append(read + "\n");
		    	read = in.readLine();
		    }
		    
		    fs.deleteCharAt(fs.length() - 1);
		    Log.d("StringBufferFS", fs.toString());
		} catch (Exception e) {
			Log.d("ERROR-readingShader", "Could not read shader: " + e.getLocalizedMessage());
		}
		
		
		// Setup everything
		setup(vs.toString(), fs.toString(), hasTextures, numTextures);
	}
	
	
	/**************************
	 * OTHER METHODS
	 *************************/
	
	/** 
	 * Sets up everything
	 * @param vs the vertex shader
	 * @param fs the fragment shader 
	 */
	private void setup(String vs, String fs, boolean hasTextures, int numTextures) {
		this._vertexS = vs;
		this._fragmentS = fs;
		
		// create the program
		int create = createProgram();
		
		// textures
		this.hasTextures = hasTextures;
		this.numTextures = numTextures;
		
		// setup the handles
		this.setupHandles();
	}
	
	
	/**
	 * Creates a shader program.
	 * @param vertexSource
	 * @param fragmentSource
	 * @return returns 1 if creation successful, 0 if not
	 */
	private int createProgram() {
		// Vertex shader
        _vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, _vertexS);
        if (_vertexShader == 0) {
            return 0;
        }

        // pixel shader
        _pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, _fragmentS);
        if (_pixelShader == 0) {
            return 0;
        }

        // Create the program
        _program = GLES20.glCreateProgram();
        if (_program != 0) {
            GLES20.glAttachShader(_program, _vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(_program, _pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(_program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(_program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e("Shader", "Could not link _program: ");
                Log.e("Shader", GLES20.glGetProgramInfoLog(_program));
                GLES20.glDeleteProgram(_program);
                _program = 0;
                return 0;
            }
        }
        else
        	Log.d("CreateProgram", "Could not create program");
        
        return 1;
    }
	
	/**
	 * Sets up the handles for the inputs
	 */
	public void setupHandles() {
		 // The handles
        // position
        maPositionHandle = GLES20.glGetAttribLocation(_program, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        
        // normal
        // position
        maNormalHandle = GLES20.glGetAttribLocation(_program, "aNormal");
        checkGlError("glGetAttribLocation normal");
        if (maNormalHandle == -1) {
            throw new RuntimeException("Could not get attrib location for normal");
        }
        
        // texture
        // handle for whether textures are available or not
        hasTextureHandle = GLES20.glGetUniformLocation(_program, "hasTexture");
        checkGlError("glGetAttribLocation hasTextureHandle");
        if (hasTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for hasTextureHandle");
        }
        
        if (hasTextures) {
	        maTextureHandle = GLES20.glGetAttribLocation(_program, "textureCoord");
	        checkGlError("glGetAttribLocation aTextureCoord");
	        if (maTextureHandle == -1) {
	            throw new RuntimeException("Could not get attrib location for aTextureCoord");
	        }
        }

        // modelview/projection matrix
        muMVPMatrixHandle = GLES20.glGetUniformLocation(_program, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }
        
        // Enable all of the vertex attribute arrays
		GLES20.glEnableVertexAttribArray(this.maPositionHandle);
		GLES20.glEnableVertexAttribArray(this.maNormalHandle);
		GLES20.glEnableVertexAttribArray(this.hasTextureHandle);
		GLES20.glEnableVertexAttribArray(this.muMVPMatrixHandle);
	}
	
	/**
	 * Loads a shader (either vertex or pixel) given the source
	 * @param shaderType VERTEX or PIXEL
	 * @param source The string data representing the shader code
	 * @return handle for shader
	 */
	private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("Shader", "Could not compile shader " + shaderType + ":");
                Log.e("Shader", GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }
	
	/**
	 * Error for OpenGL
	 * @param op
	 */
	private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("Shader", op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

	/***************************
	 * GET/SET
	 *************************/
	public int get_program() {
		return _program;
	}

	public void set_program(int _program) {
		this._program = _program;
	}

	public int get_vertexShader() {
		return _vertexShader;
	}

	public void set_vertexShader(int shader) {
		_vertexShader = shader;
	}

	public int get_pixelShader() {
		return _pixelShader;
	}

	public void set_pixelShader(int shader) {
		_pixelShader = shader;
	}

	public int getMuMVPMatrixHandle() {
		return muMVPMatrixHandle;
	}

	public void setMuMVPMatrixHandle(int muMVPMatrixHandle) {
		this.muMVPMatrixHandle = muMVPMatrixHandle;
	}

	public int getMaPositionHandle() {
		return maPositionHandle;
	}

	public void setMaPositionHandle(int maPositionHandle) {
		this.maPositionHandle = maPositionHandle;
	}
	public int getMaNormalHandle() {
		return maNormalHandle;
	}

	public void setMaNormalHandle(int maNormalHandle) {
		this.maNormalHandle = maNormalHandle;
	}
	public int getMaTextureHandle() {
		return maTextureHandle;
	}

	public int getHasTextureHandle() {
		return this.hasTextureHandle;
	}

	public void setMaTextureHandle(int maTextureHandle) {
		this.maTextureHandle = maTextureHandle;
	}

	public String get_vertexS() {
		return _vertexS;
	}

	public void set_vertexS(String _vertexs) {
		_vertexS = _vertexs;
	}

	public String get_fragmentS() {
		return _fragmentS;
	}

	public void set_fragmentS(String _fragments) {
		_fragmentS = _fragments;
	}
}
