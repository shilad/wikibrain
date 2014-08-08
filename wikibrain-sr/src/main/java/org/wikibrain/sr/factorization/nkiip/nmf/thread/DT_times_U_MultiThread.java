/****************************************************************************************

 *Laboratory of Intelligent Information Processing, Nankai University

 *Authors: Zhicheng He, Yingjie Xu, Jun Xu, MaoQiang xie, Yalou Huang

 *****************************************************************************************/
package org.wikibrain.sr.factorization.nkiip.nmf.thread;

import org.wikibrain.sr.factorization.nkiip.nmf.basic.matrix.DenseMatrix;
import org.wikibrain.sr.factorization.nkiip.nmf.basic.matrix.Term_Doc_Matrix;
import org.wikibrain.sr.factorization.nkiip.nmf.basic.struct.KeyValuePair;

public class DT_times_U_MultiThread {
	public static int iMaxThreads = 4;
	public int iThreadID = -1;
	public DenseMatrix U;
	public Term_Doc_Matrix D;
	public DenseMatrix Phi;

	public DT_times_U_MultiThread(int iThread, DenseMatrix inU,
			Term_Doc_Matrix inD, DenseMatrix inPhi) {
		iThreadID = iThread;
		U = inU;
		D = inD;
		Phi = inPhi;
	}

	public void DT_times_U_ThreadI() throws Exception {
		for (int iTermID = 0; iTermID < U.m_NumRow; iTermID++) // the id
																// for
																// join
																// and
																// dimishing
		{
			if (iTermID % iMaxThreads != iThreadID)
				continue;

			KeyValuePair[] lstDocValue = D.GetRow(iTermID);
			for (int i = 0; i < lstDocValue.length; i++) {
				KeyValuePair pDocValue = lstDocValue[i];
				int iDocID = pDocValue.key;
				synchronized (Phi.p_MatrixData[iDocID]) {
					for (int iTopicID = 0; iTopicID < U.m_NumColumn; iTopicID++) {
						Phi.SetValue(
								iDocID,
								iTopicID,
								Phi.GetValue(iDocID, iTopicID)
										+ pDocValue.value
										* U.GetValue(iTermID, iTopicID));
					}
				}
			}
		}
	}
}
