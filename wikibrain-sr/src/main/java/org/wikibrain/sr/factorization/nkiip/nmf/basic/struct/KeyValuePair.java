/****************************************************************************************

 *Laboratory of Intelligent Information Processing, Nankai University

 *Authors: Zhicheng He, Yingjie Xu, Jun Xu, MaoQiang xie, Yalou Huang

 *****************************************************************************************/
package org.wikibrain.sr.factorization.nkiip.nmf.basic.struct;

public class KeyValuePair {

	public int key;
	public float value;

	/**
	 * constructs a KeyValuePair with specified key and value
	 * @param k the key
	 * @param v the value
	 */
	public KeyValuePair(int k, float v) {
		key = k;
		value = v;
	};

	/**
	 * constructs an empty KeyValuePair
	 */
	public KeyValuePair() {
		
	};
}
