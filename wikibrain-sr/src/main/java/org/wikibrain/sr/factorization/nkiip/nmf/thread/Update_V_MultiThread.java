/****************************************************************************************

 *Laboratory of Intelligent Information Processing, Nankai University

 *Authors: Zhicheng He, Yingjie Xu, Jun Xu, MaoQiang xie, Yalou Huang

 *****************************************************************************************/
package org.wikibrain.sr.factorization.nkiip.nmf.thread;

import org.wikibrain.sr.factorization.nkiip.nmf.basic.matrix.DenseMatrix;

public class Update_V_MultiThread {
	public static int iMaxThreads = 4;
	public int iThreadID = -1;
	public DenseMatrix Phi; // doc * topic
	public DenseMatrix Sigma; // topic * topic
	public DenseMatrix V; // doc * topic

	public Update_V_MultiThread(int iThread, DenseMatrix inSigma,
			DenseMatrix inPhi, DenseMatrix outV) {
		iThreadID = iThread;
		Sigma = inSigma;
		Phi = inPhi;
		V = outV;
	}

	public void Update_V_ThreadI() throws Exception {
		for (int iDoc = 0; iDoc < Phi.m_NumRow; iDoc++) {
			if (iDoc % iMaxThreads != iThreadID)
				continue;

			float[] lstTmp = new float[V.m_NumColumn];
			for (int iTopic = 0; iTopic < V.m_NumColumn; iTopic++) {
				lstTmp[iTopic] = 0;
				for (int iTopic2 = 0; iTopic2 < V.m_NumColumn; iTopic2++) {
					lstTmp[iTopic] += V.GetValue(iDoc, iTopic2)
							* Sigma.GetValue(iTopic2, iTopic);
				}
			}
			for (int iTopic = 0; iTopic < V.m_NumColumn; iTopic++) {
				float dValue = V.GetValue(iDoc, iTopic)
						* Phi.GetValue(iDoc, iTopic) / lstTmp[iTopic];
				if(dValue < 2e-16){
					V.SetValue(iDoc, iTopic, 0.0F);
				}else{
					V.SetValue(iDoc, iTopic, dValue);
				}
			}
		}
	}
}
