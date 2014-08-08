/****************************************************************************************

 *Laboratory of Intelligent Information Processing, Nankai University

 *Authors: Zhicheng He, Yingjie Xu, Jun Xu, MaoQiang xie, Yalou Huang

 *****************************************************************************************/
package org.wikibrain.sr.factorization.nkiip.nmf.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.wikibrain.sr.factorization.nkiip.nmf.basic.matrix.DenseMatrix;
import org.wikibrain.sr.factorization.nkiip.nmf.basic.matrix.Term_Doc_Matrix;
import org.wikibrain.sr.factorization.nkiip.nmf.basic.thread.D_times_VT_MultiThread;
import org.wikibrain.sr.factorization.nkiip.nmf.basic.thread.VT_times_V_MultiThread;
import org.wikibrain.sr.factorization.nkiip.nmf.thread.DT_times_U_MultiThread;
import org.wikibrain.sr.factorization.nkiip.nmf.thread.UT_times_U_MultiThread;
import org.wikibrain.sr.factorization.nkiip.nmf.thread.Update_U_MultiThread;
import org.wikibrain.sr.factorization.nkiip.nmf.thread.Update_V_MultiThread;

public class NMF {
	public Term_Doc_Matrix m_Term_Doc_DMatrix;
	public DenseMatrix m_Term_Topic_UMatrix;
	public DenseMatrix m_Doc_Topic_VMatrix;

	public int m_iMaxIterNum = 500;
	public String m_UMatrix_prefix = "UMatrix";
	public String m_VMatrix_prefix = "VMatrix";
	public int iOutputIterSkip = 5;
	private String path = "nmf_output/";

	public void Initialization(String fnDMatrix, String fnUMatrix,
			String fnVMatrix, int iNumberOfTopics) throws Exception {
		m_Term_Doc_DMatrix = new Term_Doc_Matrix();
		m_Term_Topic_UMatrix = new DenseMatrix();
		m_Doc_Topic_VMatrix = new DenseMatrix();
		if (m_Term_Doc_DMatrix.Load(fnDMatrix) == false) {
			throw new Exception("fail to load D matrices");
		}
		if (m_Term_Topic_UMatrix.Load(fnUMatrix,
				m_Term_Doc_DMatrix.iNumberOfTerms, iNumberOfTopics) == false
				|| m_Doc_Topic_VMatrix.Load(fnVMatrix,
						m_Term_Doc_DMatrix.iNumberOfDocs, iNumberOfTopics) == false) {
			throw new Exception("fail to load U (or V) matrices");
		}
		System.out.println();
		if(!(new File(path).isDirectory())){
			new File(path).mkdirs();
		}
	}

	public boolean NMF_Learn() throws Exception {
		int iter = 0;
		while (iter < m_iMaxIterNum) {
			System.out.println("Start iteration #" + iter);

			UpdateU();
			System.gc();
			if (iter % iOutputIterSkip == 0 && iter != 0) {
				System.out.println("Write U matrix");
				OutputU(m_UMatrix_prefix + "." + iter);
			}

			UpdateV();
			System.gc();
			if (iter % iOutputIterSkip == 0 && iter != 0) {
				System.out.println("Write V matrix");
				OutputV(m_VMatrix_prefix + "." + iter);
			}
			System.out.println("Complete iteration #" + iter);
			iter++;
		}
		return true;
	}

	void UpdateU() throws Exception {
		// Matrix S, dense, topic * topic
		System.out.println("\tUpdate U: S = V^T * V");
		Date timeBegin = new Date();

		DenseMatrix m_Topic_Topic_SMatrix = new DenseMatrix(
				m_Term_Topic_UMatrix.m_NumColumn,
				m_Term_Topic_UMatrix.m_NumColumn);

		// S = V * V^T: Note S is a symmetric matrix
		ArrayList<Thread> lstThread = new ArrayList<Thread>();
		for (int iThread = 0; iThread < VT_times_V_MultiThread.iMaxThreads; iThread++) {
			final VT_times_V_MultiThread m_i = new VT_times_V_MultiThread(
					iThread, m_Doc_Topic_VMatrix, m_Topic_Topic_SMatrix);
			Thread t_i = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						m_i.VT_times_V_ThreadI();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			lstThread.add(t_i);
			t_i.start();
		}
		for (Thread thread : lstThread) {
			thread.join();
		}
		Date timeEnd = new Date();
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println("Time used: "
				+ (timeEnd.getTime() - timeBegin.getTime()) + "ms"
				+ "; finished at " + sdFormat.format(timeEnd));

		// calculate R
		// Matrix R, dense, term * topic: D * V^T

		System.out.println("\tUpdate U: R = D * V^T");
		timeBegin = new Date();

		DenseMatrix m_Term_Topic_RMatrix = new DenseMatrix(
				m_Term_Topic_UMatrix.m_NumRow, m_Term_Topic_UMatrix.m_NumColumn);
		lstThread.clear();
		for (int iThread = 0; iThread < D_times_VT_MultiThread.iMaxThreads; iThread++) {
			final D_times_VT_MultiThread m_i = new D_times_VT_MultiThread(
					iThread, m_Term_Doc_DMatrix, m_Doc_Topic_VMatrix,
					m_Term_Topic_RMatrix);
			Thread t_i = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						m_i.D_times_VT_ThreadI();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			lstThread.add(t_i);
			t_i.start();
		}
		for (Thread thread : lstThread) {
			thread.join();
		}
		timeEnd = new Date();
		System.out.println("Time used: "
				+ (timeEnd.getTime() - timeBegin.getTime()) + "ms"
				+ "; finished at " + sdFormat.format(timeEnd));

		System.out.println("\tUpdate U with R and S");
		timeBegin = new Date();

		lstThread.clear();
		for (int iThread = 0; iThread < Update_U_MultiThread.iMaxThreads; iThread++) {
			final Update_U_MultiThread s_i = new Update_U_MultiThread(iThread,
					m_Topic_Topic_SMatrix, m_Term_Topic_RMatrix,
					m_Term_Topic_UMatrix);
			Thread t_i = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						s_i.Update_U_ThreadI();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			lstThread.add(t_i);
			t_i.start();
		}
		for (Thread thread : lstThread) {
			thread.join();
		}
		timeEnd = new Date();
		System.out.println("Time used: "
				+ (timeEnd.getTime() - timeBegin.getTime()) + "ms"
				+ "; finished at " + sdFormat.format(timeEnd));

		m_Topic_Topic_SMatrix.ReleaseMemory();
		m_Term_Topic_RMatrix.ReleaseMemory();

	}

	void UpdateV() throws Exception {
		/*
		 * Steps 1. Sigma = (U^T * U + \lambda2 I)^-1 //M * M 2. Phi = U^T * D
		 * // K * M \times M * N 3. V = Sigma * Phi // K * K \times K * N
		 */

		System.out.println("\tUpdate V: Sigma = U^T * U. ");
		Date timeBegin = new Date();

		DenseMatrix Sigma = new DenseMatrix(m_Term_Topic_UMatrix.m_NumColumn,
				m_Term_Topic_UMatrix.m_NumColumn);

		ArrayList<Thread> lstThread = new ArrayList<Thread>();
		lstThread.clear();
		for (int iThread = 0; iThread < UT_times_U_MultiThread.iMaxThreads; iThread++) {
			final UT_times_U_MultiThread s_i = new UT_times_U_MultiThread(
					iThread, m_Term_Topic_UMatrix, Sigma);
			Thread t_i = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						s_i.UT_times_U_ThreadI();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			lstThread.add(t_i);
			t_i.start();

		}
		for (Thread thread : lstThread) {
			thread.join();
		}
		Date timeEnd = new Date();
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println("Time used: "
				+ (timeEnd.getTime() - timeBegin.getTime()) + "ms"
				+ "; finished at " + sdFormat.format(timeEnd));

		System.out.println("\tUpdate V: Phi = D^T * U. ");
		timeBegin = new Date();
		lstThread.clear();
		DenseMatrix Phi = new DenseMatrix(m_Doc_Topic_VMatrix.m_NumRow,
				m_Doc_Topic_VMatrix.m_NumColumn);
		for (int iThread = 0; iThread < DT_times_U_MultiThread.iMaxThreads; iThread++) {
			final DT_times_U_MultiThread s_i = new DT_times_U_MultiThread(
					iThread, m_Term_Topic_UMatrix, m_Term_Doc_DMatrix, Phi // out
			);
			Thread t_i = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						s_i.DT_times_U_ThreadI();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			lstThread.add(t_i);
			t_i.start();

		}
		for (Thread thread : lstThread) {
			thread.join();
		}
		timeEnd = new Date();
		System.out.println("Time used: "
				+ (timeEnd.getTime() - timeBegin.getTime()) + "ms"
				+ "; finished at " + sdFormat.format(timeEnd));

		System.out.println("\tUpdate V with Sigma and Phi");
		timeBegin = new Date();
		lstThread.clear();
		for (int iThread = 0; iThread < Update_V_MultiThread.iMaxThreads; iThread++) {
			final Update_V_MultiThread s_i = new Update_V_MultiThread(iThread,
					Sigma, Phi, m_Doc_Topic_VMatrix);
			Thread t_i = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						s_i.Update_V_ThreadI();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			lstThread.add(t_i);
			t_i.start();

		}
		for (Thread thread : lstThread) {
			thread.join();
		}
		timeEnd = new Date();
		System.out.println("Time used: "
				+ (timeEnd.getTime() - timeBegin.getTime()) + "ms"
				+ "; finished at " + sdFormat.format(timeEnd));

		Sigma.ReleaseMemory();
		Phi.ReleaseMemory();
	}

	public void OutputU(String fn) throws Exception {
		BufferedWriter sw = new BufferedWriter(new FileWriter(path + fn));

		int NumberOfWordsInU = 0;
		sw.write("TermID\tTopicID:value TopicID:value ...");
		sw.newLine();
		for (int iTerm = 0; iTerm < m_Term_Topic_UMatrix.m_NumRow; iTerm++) {

			String value = "";
			for (Integer iPos = 0; iPos < m_Term_Topic_UMatrix.m_NumColumn; iPos++) {
				value += iPos.toString()+":"+m_Term_Topic_UMatrix.GetValue(iTerm, iPos) + " ";
			}
			
			String strTerm = m_Term_Doc_DMatrix.lstInternalTermID2OrgTermID
					.get(iTerm);
			sw.write(strTerm + "\t" + value.trim());
			sw.newLine();
			NumberOfWordsInU++;
		}
		System.out.println("\nOutput U: " + NumberOfWordsInU
				+ " terms written into " + fn);
		sw.close();
	}

	public void OutputV(String fn) throws Exception {

		BufferedWriter sw = new BufferedWriter(new FileWriter(path + fn));

		sw.write("DocID\tTopicID:value TopicID:value ...");
		sw.newLine();
		for (Integer iDoc = 0; iDoc < m_Doc_Topic_VMatrix.m_NumRow; iDoc++) {
			String value = "";
			for (Integer iTopic = 0; iTopic < m_Doc_Topic_VMatrix.m_NumColumn; iTopic++) {
				value += iTopic.toString() + ":"
						+ m_Doc_Topic_VMatrix.GetValue(iDoc, iTopic).toString()
						+ " ";
			}
			sw.write(iDoc.toString() + "\t" + value.trim());
			sw.newLine();
		}

		sw.close();
	}
}
