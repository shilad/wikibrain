/****************************************************************************************

 *Laboratory of Intelligent Information Processing, Nankai University

 *Authors: Zhicheng He, Yingjie Xu, Jun Xu, MaoQiang xie, Yalou Huang

 *****************************************************************************************/
package org.wikibrain.sr.factorization.nkiip.nmf.main;

import org.wikibrain.sr.factorization.nkiip.nmf.basic.thread.D_times_VT_MultiThread;
import org.wikibrain.sr.factorization.nkiip.nmf.basic.thread.VT_times_V_MultiThread;
import org.wikibrain.sr.factorization.nkiip.nmf.basic.util.Arguments;
import org.wikibrain.sr.factorization.nkiip.nmf.thread.DT_times_U_MultiThread;
import org.wikibrain.sr.factorization.nkiip.nmf.thread.UT_times_U_MultiThread;
import org.wikibrain.sr.factorization.nkiip.nmf.thread.Update_U_MultiThread;
import org.wikibrain.sr.factorization.nkiip.nmf.thread.Update_V_MultiThread;

public class Program {
	public static void main(String[] args) throws Exception {
		Arguments cmmdArg = new Arguments(args);
		NMF nmf = new NMF();
		String fnDMatrix = "";
		String fnUMatrix = "";
		String fnVMatrix = "";
		int iNumberOfTopics = 50;

		try {
			fnDMatrix = cmmdArg.getValue("d");
			fnUMatrix = cmmdArg.getValue("iu");
			fnVMatrix = cmmdArg.getValue("iv");
			if(cmmdArg.getValue("t") != null && !cmmdArg.getValue("t").equals("")){
				iNumberOfTopics = Integer.parseInt(cmmdArg.getValue("t"));
			}

			String str_UPrefix = cmmdArg.getValue("u");
			String str_VPrefix = cmmdArg.getValue("v");

			if (cmmdArg.getValue("skip") != null
					&& cmmdArg.getValue("skip").length() > 0) {
				nmf.iOutputIterSkip = Integer.parseInt(cmmdArg.getValue("skip"));
			}

			if (str_UPrefix != null && str_UPrefix.length() > 0) {
				nmf.m_UMatrix_prefix = str_UPrefix;
			}
			if (str_VPrefix != null && str_VPrefix.length() > 0) {
				nmf.m_VMatrix_prefix = str_VPrefix;
			}

			if(cmmdArg.getValue("#") != null && !cmmdArg.getValue("#").equals("")){
				nmf.m_iMaxIterNum = Integer.parseInt(cmmdArg.getValue("#"));
			}
			
			nmf.Initialization(fnDMatrix, fnUMatrix, fnVMatrix, iNumberOfTopics);
			// specify the number of cores for running
						if (cmmdArg.getValue("c") != null) {
							int iNumCores = Integer.parseInt(cmmdArg.getValue("c"));
							if (iNumCores > 0) {
								VT_times_V_MultiThread.iMaxThreads = iNumCores;
								D_times_VT_MultiThread.iMaxThreads = iNumCores;
								Update_U_MultiThread.iMaxThreads = iNumCores;
								Update_V_MultiThread.iMaxThreads = iNumCores;
								UT_times_U_MultiThread.iMaxThreads = iNumCores;
								DT_times_U_MultiThread.iMaxThreads = iNumCores;
							}
						}
		} catch (Exception e) {
			usage();
			return;
		}

		System.out.println("Start learning NMF model");
		nmf.NMF_Learn();
		System.out.println("Success.");

		System.out.println("Write matrix U and V\n");
		nmf.OutputU(nmf.m_UMatrix_prefix + ".term-topic_UMatrix");
		nmf.OutputV(nmf.m_VMatrix_prefix + ".doc-topic_VMatrix");
		System.out.println("Suceess!");
	}

	static void usage() {
		System.out
				.println("Usage: java Program -d word_doc_file_name [options]\n\n"
						+

						"Topic model options: \n"
						+ "   -t int        -> number of topics (default 50)\n\n"
						+ 

						"Output options: \n"
						+ "   -u string     -> filename prefix for outputted U matrix (default UMatrix)\n"
						+ "   -v string     -> filename prefix for outputted V matrix (default VMatrix)\n"
						+ "   -skip int     -> number of skipped iterations that don��t output U or V (default 5)\n\n"
						+
						
						"Optimization options:  \n"
						+ "   -# int        -> number of learning iterations (default 500)\n"
						+ "   -c int      	-> number of threads running in parallel (default 4)\n\n"
						+

						"Restart options: \n"
						+ "   -iv string    -> filename of initial V matrix (default auto generate)\n"
						+ "   -iu string    -> filename of initial U matrix (default auto generate)"
						);
	}
}
