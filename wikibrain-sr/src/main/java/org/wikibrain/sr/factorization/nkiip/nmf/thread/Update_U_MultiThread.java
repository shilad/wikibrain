/****************************************************************************************

 *Laboratory of Intelligent Information Processing, Nankai University

 *Authors: Zhicheng He, Yingjie Xu, Jun Xu, MaoQiang xie, Yalou Huang

 *****************************************************************************************/
package org.wikibrain.sr.factorization.nkiip.nmf.thread;

import org.wikibrain.sr.factorization.nkiip.nmf.basic.matrix.DenseMatrix;

public class Update_U_MultiThread{
	public static int iMaxThreads = 4;
	public int iThreadID = -1;
	public DenseMatrix R; // term * topic
	public DenseMatrix S; // topic * topic
	public DenseMatrix U; // term * topic

	public Update_U_MultiThread(int iThread, DenseMatrix inS, DenseMatrix inR,
			DenseMatrix inU) {
		iThreadID = iThread;
		S = inS;
		R = inR;
		U = inU;
	}

	public void Update_U_ThreadI() throws Exception {
		for (int iTerm = 0; iTerm < U.m_NumRow; iTerm++) {
			if (iTerm % iMaxThreads != iThreadID)
				continue;

			float[] lstTmp = new float[U.m_NumColumn];
			for (int iTopic = 0; iTopic < U.m_NumColumn; iTopic++) {
				lstTmp[iTopic] = 0;
				for (int iTopic2 = 0; iTopic2 < U.m_NumColumn; iTopic2++) {
					lstTmp[iTopic] += U.GetValue(iTerm, iTopic2)
							* S.GetValue(iTopic2, iTopic);
				}
			}
			for (int iTopic = 0; iTopic < U.m_NumColumn; iTopic++) {
				float dValue = U.GetValue(iTerm, iTopic)
						* R.GetValue(iTerm, iTopic) / (lstTmp[iTopic]);
				if (dValue < 2e-16) {
					U.SetValue(iTerm, iTopic, 0.0f);
				} else {
					U.SetValue(iTerm, iTopic, dValue);
				}
			}
		}
	}
}
