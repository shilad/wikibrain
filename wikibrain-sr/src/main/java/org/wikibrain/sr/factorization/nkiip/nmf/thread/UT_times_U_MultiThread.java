/****************************************************************************************

 *Laboratory of Intelligent Information Processing, Nankai University

 *Authors: Zhicheng He, Yingjie Xu, Jun Xu, MaoQiang xie, Yalou Huang

 *****************************************************************************************/
package org.wikibrain.sr.factorization.nkiip.nmf.thread;

import org.wikibrain.sr.factorization.nkiip.nmf.basic.matrix.DenseMatrix;

public class UT_times_U_MultiThread {
	public static int iMaxThreads = 4;
	public int iThreadID = -1;
	public DenseMatrix U;
	public DenseMatrix Sigma;

	public UT_times_U_MultiThread(int iThread, DenseMatrix inU,
			DenseMatrix inSigma) {
		iThreadID = iThread;
		U = inU;
		Sigma = inSigma;
	}

	public void UT_times_U_ThreadI() throws Exception {
		DenseMatrix localSigma = new DenseMatrix(U.m_NumColumn, U.m_NumColumn);

		for (int iTermID = 0; iTermID < U.m_NumRow; iTermID++) {
			if (iTermID % iMaxThreads != iThreadID)
				continue;

			for (int iTopic = 0; iTopic < U.m_NumColumn; iTopic++) {
				for (int iTopic2 = 0; iTopic2 < U.m_NumColumn; iTopic2++) {
					localSigma.SetValue(
							iTopic,
							iTopic2,
							localSigma.GetValue(iTopic, iTopic2)
									+ U.GetValue(iTermID, iTopic)
									* U.GetValue(iTermID, iTopic2));
				}
			}
		}

		synchronized (Sigma) {
			for (int iTopic1 = 0; iTopic1 < Sigma.m_NumRow; iTopic1++) {
				for (int iTopic2 = 0; iTopic2 < Sigma.m_NumColumn; iTopic2++) {
					Sigma.SetValue(
							iTopic1,
							iTopic2,
							Sigma.GetValue(iTopic1, iTopic2)
									+ localSigma.GetValue(iTopic1, iTopic2));
				}
			}
		}
		localSigma = null;
	}
}
