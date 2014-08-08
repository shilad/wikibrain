/****************************************************************************************

 *Laboratory of Intelligent Information Processing, Nankai University

 *Authors: Zhicheng He, Yingjie Xu, Jun Xu, MaoQiang xie, Yalou Huang

 *****************************************************************************************/
package org.wikibrain.sr.factorization.nkiip.nmf.basic.matrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.wikibrain.sr.factorization.nkiip.nmf.basic.struct.KeyValuePair;
import org.wikibrain.sr.factorization.nkiip.nmf.basic.util.StringSplitter;

public class Term_Doc_Matrix {

	public int iNumberOfTerms = 0;
	public int iNumberOfDocs = 0;
	public List<String> lstInternalTermID2OrgTermID = new ArrayList<String>();
	public List<String> lstInternalDocID2OrgDocID = new ArrayList<String>();

	KeyValuePair[][] pData = null;

	/**
	 * load matrix entries from file
	 * @param path file path
	 * @return succeed or not
	 * @throws Exception if file reading fails
	 */
	public boolean Load(String path) throws Exception {
		BufferedReader sr = new BufferedReader(new FileReader(path));
		{
			String line = sr.readLine();
			String[] toks = StringSplitter.RemoveEmptyEntries(StringSplitter
					.split(",:; ", line));

			iNumberOfTerms = Integer.parseInt(toks[1]);
			iNumberOfDocs = Integer.parseInt(toks[3]);
			System.out.println("\nLoading " + iNumberOfDocs
					+ " documents and " + iNumberOfTerms + " words from " + path);

			pData = new KeyValuePair[iNumberOfTerms][];
			int cnt = 0;
			line = sr.readLine();
			while (line != null) {
				String[] tokens = StringSplitter
						.RemoveEmptyEntries(StringSplitter.split("\t ", line));
				String orgTerm = tokens[0];
				int iDocFreq = Integer.parseInt(tokens[1]);

				if (cnt % 10000 == 0) {
					System.out.println("\r" + cnt + "/" + iNumberOfTerms);
				}
				cnt++;

				int termid = lstInternalTermID2OrgTermID.size();
				lstInternalTermID2OrgTermID.add(orgTerm);

				pData[termid] = new KeyValuePair[iDocFreq];
				for (int i = 0; i < iDocFreq; i++) {
					pData[termid][i] = new KeyValuePair();
				}

				for (int i = 3; i < tokens.length; i++) {
					String[] tid_tfidf = tokens[i].split(":");
					pData[termid][i - 3].key = Integer
							.parseInt(tid_tfidf[0]);
					pData[termid][i - 3].value = Float
							.parseFloat(tid_tfidf[1]);
				}
				line = sr.readLine();
			}
		}
		sr.close();
		iNumberOfTerms = lstInternalTermID2OrgTermID.size();
		System.out.println("\n" + iNumberOfDocs + " documents and "
				+ iNumberOfTerms + " words loaded");
		return true;
	}

	/**
	 * get method for matrix row
	 * @param row row number
	 * @return matrix row
	 */
	public KeyValuePair[] GetRow(int row) {
		return pData[row];
	}
}
