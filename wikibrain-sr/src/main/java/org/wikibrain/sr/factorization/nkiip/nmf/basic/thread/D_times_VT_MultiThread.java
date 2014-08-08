/****************************************************************************************

 *Laboratory of Intelligent Information Processing, Nankai University

 *Authors: Zhicheng He, Yingjie Xu, Jun Xu, MaoQiang xie, Yalou Huang

 *****************************************************************************************/
package org.wikibrain.sr.factorization.nkiip.nmf.basic.thread;

import org.wikibrain.sr.factorization.nkiip.nmf.basic.matrix.DenseMatrix;
import org.wikibrain.sr.factorization.nkiip.nmf.basic.matrix.Term_Doc_Matrix;
import org.wikibrain.sr.factorization.nkiip.nmf.basic.struct.KeyValuePair;

public class D_times_VT_MultiThread {
	public static int iMaxThreads = 4;
	public int iThreadID = -1;
	public Term_Doc_Matrix D; // term * doc
	public DenseMatrix V; // doc * topic
	public DenseMatrix R; // term * topic

	/**
	 * constructs a D_times_VT_MultiThread
	 * @param id thread id
	 * @param inD term_document matrix
	 * @param inV document_topic matrix
	 * @param inR R matrix to store calculation results
	 */
	public D_times_VT_MultiThread(int id, Term_Doc_Matrix inD, DenseMatrix inV,
			DenseMatrix inR) {
		iThreadID = id;
		D = inD;
		V = inV;
		R = inR;
	}

	/**
	 * calculates the specified part of D_times_VT
	 * @throws Exception if calculation fails
	 */
	public void D_times_VT_ThreadI() throws Exception {
		// for each terms
		for (int iTerm = 0; iTerm < D.iNumberOfTerms; iTerm++) {
			if (iTerm % iMaxThreads != iThreadID)
				continue;

			KeyValuePair[] lstDocTfIDF = D.GetRow(iTerm);
			// for each topics
			for (int iTopic = 0; iTopic < V.m_NumColumn; iTopic++) {
				R.SetValue(iTerm, iTopic, 0);
				for (int i = 0; i < lstDocTfIDF.length; i++) {
					KeyValuePair pDocTfIDF = lstDocTfIDF[i];
					R.SetValue(iTerm, iTopic,
							R.GetValue(iTerm, iTopic) + pDocTfIDF.value
									* V.GetValue(pDocTfIDF.key, iTopic));
				}
			}
		}
	}
}
