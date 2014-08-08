/****************************************************************************************

 *Laboratory of Intelligent Information Processing, Nankai University

 *Authors: Zhicheng He, Yingjie Xu, Jun Xu, MaoQiang xie, Yalou Huang

 *****************************************************************************************/
package org.wikibrain.sr.factorization.nkiip.nmf.basic.matrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.Random;

import org.wikibrain.sr.factorization.nkiip.nmf.basic.util.StringSplitter;

public class DenseMatrix {
	public Float[][] p_MatrixData = null;
	public int m_NumRow;
	public int m_NumColumn;

	/**
	 * constructs an empty DenseMatrix
	 */
	public DenseMatrix() {
	}
	
	/**
	 * constructs a DenseMatrix with specified number of rows and columns
	 * @param row number of rows
	 * @param col number of columns
	 */
	public DenseMatrix(int row, int col) {
		p_MatrixData = new Float[row][];
		for (int i = 0; i < row; i++) {
			p_MatrixData[i] = new Float[col];
			for (int j = 0; j < col; j++) {
				p_MatrixData[i][j] = 0.0f;
			}
		}

		m_NumColumn = col;
		m_NumRow = row;
	}

	/**
	 * get method for matrix entry
	 * @param row row number 
	 * @param col column number
	 * @return the specified matrix entry
	 * @throws Exception if out of index bound
	 */
	public Float GetValue(int row, int col) throws Exception {
		if (row >= m_NumRow || col >= m_NumColumn || p_MatrixData == null) {
			throw new Exception("out of index bound");
		}
		return p_MatrixData[row][col];
	}

	/**
	 * set method for matrix entry
	 * @param row row number
	 * @param col column number
	 * @param value new value for the matrix entry
	 * @return succeed or not
	 * @throws Exception if out of index bound
	 */
	public boolean SetValue(int row, int col, float value) throws Exception {
		if (row >= m_NumRow || col >= m_NumColumn || p_MatrixData == null) {
			return false;
		}
		p_MatrixData[row][col] = value;
		return true;
	}

	/**
	 * Matrix Inversion Using Cholesky decomposition
	 */
	public void Inverse() throws Exception {
		// do Cholesky decomposition
		if (m_NumRow != m_NumColumn || m_NumColumn <= 0 || p_MatrixData == null) {
			throw new Exception("cannot conduct inverse");
		}
		int dim = m_NumRow;
		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				if (j == i) {
					float sum = 0;
					for (int k = 0; k < i; k++) {
						sum += p_MatrixData[i][k] * p_MatrixData[i][k];
					}
					if ((p_MatrixData[i][i] - sum) <= 0) {
						throw new Exception("Negative x_ii - sum");
					} else {
						p_MatrixData[i][i] = (float) Math
								.sqrt(p_MatrixData[i][i] - sum);
					}
				} else {
					float sum = 0;
					for (int k = 0; k < i; k++) {
						sum += p_MatrixData[j][k] * p_MatrixData[i][k];
					}
					p_MatrixData[j][i] = (p_MatrixData[j][i] - sum)
							/ p_MatrixData[i][i];
				}
			}
		}

		// calculate the inverse matrix of the upper-triangular matrix
		float[][] Cho_matrix_Inv = new float[dim][dim];
		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				if (j == i) {
					Cho_matrix_Inv[j][i] = 1 / p_MatrixData[j][i];
				} else {
					float sum = 0;
					for (int k = i; k < j; k++) {
						sum += p_MatrixData[j][k] * Cho_matrix_Inv[k][i];
					}
					Cho_matrix_Inv[j][i] = (-sum / p_MatrixData[j][j]);
				}
			}
		}

		// calcuale the inverse matrix
		float[] temp = new float[dim];
		for (int i = 0; i < dim; i++) {
			for (int k = 0; k < dim; k++) {
				temp[k] = 0.0F;
			}
			for (int j = 0; j < dim; j++) {
				if (i <= j) {
					float sum = 0;
					for (int k = j; k < dim; k++) {
						sum += Cho_matrix_Inv[k][i] * Cho_matrix_Inv[k][j];
					}
					temp[j] = sum;
				} else {
					temp[j] = p_MatrixData[j][i];
				}
			}
			for (int j = 0; j < dim; j++) {
				p_MatrixData[i][j] = temp[j];
			}
		}
	}

	/**
	 * load matrix entries from file or generate random ones
	 * @param path file path
	 * @param iRows number of rows when generate random entries
	 * @param iCols number of columns when generate random entries
	 * @return succeed or not
	 * @throws Exception if file reading fails
	 */
	public boolean Load(String path, int iRows, int iCols) throws Exception {
		m_NumRow = iRows;
		m_NumColumn = iCols;
		if (path == null || path.equals("") || !new File(path).exists()) {
			Random rd = new Random(new Date().getTime());

			p_MatrixData = new Float[m_NumRow][];
			for (int i = 0; i < m_NumRow; i++) {
				p_MatrixData[i] = new Float[m_NumColumn];
				for (int j = 0; j < m_NumColumn; j++) {
					p_MatrixData[i][j] = rd.nextFloat();
				}
			}

			System.out.println("\n" + m_NumRow + " rows and "
					+ m_NumColumn + " columns were randomly generated");
			return true;
		}

		BufferedReader sr = new BufferedReader(new FileReader(path));
		String line;
		line = sr.readLine(); // the first line

		p_MatrixData = new Float[m_NumRow][];
		for (int i = 0; i < m_NumRow; i++) {
			p_MatrixData[i] = new Float[m_NumColumn];
		}
		System.out.println();
		int cnt = -1;
		line = sr.readLine();
		while (line != null) {
			String[] tokens = StringSplitter.RemoveEmptyEntries(StringSplitter
					.split("\t ", line));
			cnt++;
			if (cnt < 0 || cnt >= m_NumRow) {
				throw new Exception("row index out of range");
			}
			if (cnt % 10000 == 0) {
				System.out.println("\r" + cnt + "/" + m_NumRow);
			}
			for (int i = 1; i < tokens.length; i++) {
				String tok[] = tokens[i].split(":");
				int iColumnID = Integer.parseInt(tok[0]);
				if (iColumnID < 0 || iColumnID >= m_NumColumn) {
					throw new Exception("column index out of range");
				}
				p_MatrixData[cnt][iColumnID] = Float.parseFloat(tok[1]);
			}
			line = sr.readLine();
		}
		sr.close();
		System.out.println("\n" + m_NumRow + " rows and "
				+ m_NumColumn + " columns are loaded from "+path);
		return true;
	}

	/**
	 * Release all of the memory
	 */
	public void ReleaseMemory() {
		for (int i = 0; i < m_NumRow; i++) {
			p_MatrixData[i] = null;
		}
		p_MatrixData = null;
		m_NumRow = 0;
		m_NumColumn = 0;

	}

	/**
	 * set all of the entries zero
	 */
	public void Zero() throws Exception {
		if (m_NumRow == 0 || m_NumColumn == 0 || p_MatrixData == null) {
			throw new Exception("cannot zero empty matrix");
		}
		for (int i = 0; i < m_NumRow; i++) {
			for (int j = 0; j < m_NumColumn; j++) {
				p_MatrixData[i][j] = 0F;
			}
		}

	}
}
