package org.wikibrain.sr.factorization;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;
import org.wikibrain.matrix.SparseMatrix;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * Implements algorithm from http://www.csie.ntu.edu.tw/~cjlin/nmf/others/nmf.py
 *
 * @author Shilad Sen
 */
public class MyNMF {
    private Random random = new Random();

    private int m;
    private int n;
    private int rank;

    // Note that this is the transpose matrix of dimensionality N x M
    private WBSparseMatrix V;
    private FlexCompRowMatrix Vt;

    // article -> topic
    private Matrix W;

    // topic -> features
    private Matrix H;

    public MyNMF(SparseMatrix matrix, SparseMatrix transpose, int rank) {
        this.V = new WBSparseMatrix(transpose, matrix);
        this.m = V.numRows();
        this.n = V.numColumns();
        this.Vt = (FlexCompRowMatrix) V.transpose(new FlexCompRowMatrix(n, m));
        System.out.println("m, n is " + m + ", " + n);
        this.rank = rank;
    }

    /*public void nmf() {
        W = Matrices.random(n, rank);
        H = Matrices.random(rank, m);

        for (int i = 0; i < 100; i++) {
            System.out.println("doing " + i);
            nmf_iteration(i);
        }
    } */

    private double[][] denseMatrix(int m, int n) {
        return new double[m][n];
    }

    public void nmf2() {
        int i,j,p,q,r;
        int k = rank;

        int MAXN = 110000;
        int MAXM = 110000;

        double maxw_v[] = new double[MAXN];
        double maxh_v[] = new double[MAXM];
        int maxw_ind[] = new int[MAXN];
        int maxh_ind[] = new int[MAXM];
        int winner, hinner;

        double[][] H = denseMatrix(k, m);
        double[][] W = denseMatrix(n, k);
        double[][] VH = denseMatrix(n, k);
        double[][] WV = denseMatrix(k, m);
        double[][] HH = denseMatrix(k, k);
        double[][] WW = denseMatrix(k, k);
        double[][] Hnew = denseMatrix(k, m);
        double[][] GH = denseMatrix(k, m);
        double[][] HD = denseMatrix(k, m);
        double[][] SH  = denseMatrix(k, m);
        double[][] Wnew = denseMatrix(n, k);
        double[][] GW = denseMatrix(n, k);
        double[][] WD = denseMatrix(n, k);
        double[][] SW = denseMatrix(n, k);

        for (i = 0; i < k; i++) {
            for (j = 0; j < m; j++) {
                H[i][j] = random.nextDouble();
            }
        }
        for (i = 0; i < n; i++) {
            for (j = 0; j < k; j++) {
                W[i][j] = random.nextDouble();
            }
        }

        System.err.println("here 1");
        // VH=V*H'
        for ( i=0  ; i<n ; i++ )
            for ( j=0 ; j<k ; j++ )
            {
                VH[i][j] = 0;
                SparseVector sv = Vt.getRow(i);
                for (q = 0; q < sv.getUsed(); q++) {
                    double z = sv.getData()[q];
                    z = sv.getIndex()[q];
                    z = H[j][sv.getIndex()[q]];
                    VH[i][j] += sv.getData()[q] * H[j][sv.getIndex()[q]];
                }
            }

        System.err.println("here 2");
        // WV=W'*V
        for ( i=0 ; i<k ; i++ )
            for ( j=0 ; j<m ; j++ )
            {
                WV[i][j] = 0;
                SparseVector sv = V.getRow(j);
                for ( q=0 ; q<sv.getUsed(); q++ )
                    WV[i][j] += W[sv.getIndex()[q]][i]*sv.getData()[q];
            }

        System.err.println("here 3");
        // HH = H*H'
        for ( i=0 ; i<k ; i++ )
            for (j=0 ; j<k ; j++ )
            {
                HH[i][j] = 0;
                for ( r=0 ; r<m ; r++ )
                    HH[i][j] += H[i][r]*H[j][r];
            }

        // WW = W'*W
        for ( i=0 ; i<k ; i++ )
            for ( j=0 ; j<k ; j++ )
            {
                WW[i][j] = 0;
                for ( r=0 ; r<n ; r++ )
                    WW[i][j] += W[r][i]*W[r][j];
            }

        double ss, diffobj;
        double totaltime=0;
        double atotal = 0, abegin = 0;
        double btotal = 0, bbegin = 0;
        double tol = 0.001;


        System.err.println("here 5");
        for (int iter = 0; iter < 100; iter++) {
            System.err.println("here 6");

            // W updates
            ///
            for ( i=0 ; i<n ; i++ )
                for ( j=0 ; j<k ; j++ )
                    Wnew[i][j] = 0;

            // VH += V * Hnew'
            for (i = 0; i < rank; i++) {
                for (j = 0; j < m; j++) {
                    double g = Hnew[i][j];
                    if (g != 0) {
                        SparseVector sv = V.getRow(j);
                        for (q = 0; q < sv.getUsed(); q++) {
                            VH[sv.getIndex()[q]][i] +=  g * sv.getData()[q];
                        }
                    }
                }
            }

            // Compute GW <- W*HH - VH
            for ( i=0 ; i<n ; i++ )
                for ( j=0 ; j<k ; j++ )
                {
                    GW[i][j] = (-1)*VH[i][j];
                    for ( r=0 ; r<k ; r++ )
                        GW[i][j] += W[i][r]*HH[r][j];
                }

            // Compute Initial GW, WD, SW
            double init = 0;
            for (i = 0; i < n; i++) {
                maxw_v[i] = 0;
                maxw_ind[i] = -1;
                for (j=0 ; j< rank; j++ )
                {
                    double gw = GW[i][j];
                    double hh = HH[j][j];
                    double w = W[i][j];
                    double s = gw / hh;
                    s = w -s;
                    if ( s< 0)
                        s=0;
                    s = s-w;
                    SW[i][j] = s;
                    diffobj = (-1)*s*gw-0.5*hh*s*s;
                    WD[i][j] = diffobj;
                    if (diffobj > maxw_v[i])
                    {
                        maxw_v[i] = diffobj;
                        maxw_ind[i] = j;
                    }
                }
                if ( maxw_v[i]>init)
                    init = maxw_v[i];
            }

            System.err.println("here 7");
            // W's Coordinate updates
            int totalinner =0;
            for (p=0 ; p<n ; p++)
            {
                for ( winner = 0 ; winner <m ; winner++)
                {
                    double pv=maxw_v[p];
                    q = maxw_ind[p];
                    if ( q==-1 )
                        break;
                    double s =SW[p][q];

                    if ( pv< init*tol)
                        break;
                    for ( i=0 ; i<k ; i++)
                    {
                        WW[q][i] = WW[q][i]+s*W[p][i];
                        WW[i][q] = WW[q][i];
                    }
                    WW[q][q] = WW[q][q] + s*s+s*W[p][q];

                    Wnew[p][q] += s;
                    W[p][q] = W[p][q] + s;

                    maxw_v[p] = 0;
                    maxw_ind[p]=-1;
                    for ( i=0 ; i<k ; i++ )
                    {
                        GW[p][i] = GW[p][i] + s*HH[q][i];
                        ss = W[p][i]-GW[p][i]/HH[i][i];
                        if (ss < 0)
                            ss=0;
                        ss = ss-W[p][i];
                        SW[p][i] = ss;
                        diffobj = (-1)*(ss*GW[p][i]+0.5*HH[i][i]*ss*ss);
                        if ( diffobj > maxw_v[p])
                        {
                            maxw_v[p] = diffobj;
                            maxw_ind[p] = i;
                        }
                    }
                }
                totalinner += winner;
            }
            /// H updates
            ///

            // Compute WV
            for ( i=0 ; i<k ; i++ )
                for ( j=0 ; j<n ; j++ )
                    if (Wnew[j][i] != 0)
                    {
                        double g=Wnew[j][i];
                        SparseVector vt = Vt.getRow(j);
                        for (q = 0; q < vt.getUsed(); q++) {
                            WV[i][vt.getIndex()[q]] += g * vt.getData()[q];
                        }
                    }

            for ( i=0 ; i<k ; i++ )
                for ( j=0 ; j<m ; j++ )
                    Hnew[i][j] = 0;

            // Compute GH
            for ( i=0 ; i<k ; i++ )
                for ( j=0 ; j<m ; j++ )
                {
                    GH[i][j] = (-1)*WV[i][j];
                    for ( r=0 ; r<k ; r++ )
                        GH[i][j] += WW[i][r]*H[r][j];
                }

            // Compute Initial GH, HD, SH
            init = 0;
            for ( i=0 ; i<m ; i++ )
            {
                maxh_v[i] = 0;
                maxh_ind[i] = -1;
                for ( j=0 ; j<k ; j++ )
                {
                    double s = GH[j][i]/WW[j][j];
                    s = H[j][i]-s;
                    if ( s< 0)
                        s=0;
                    s = s-H[j][i];
                    SH[j][i] = s;
                    diffobj = (-1)*s*GH[j][i]-0.5*WW[j][j]*s*s;
                    HD[j][i] = diffobj;
                    if (diffobj > maxh_v[i])
                    {
                        maxh_v[i] = diffobj;
                        maxh_ind[i] = j;
                    }
                }
                if ( maxh_v[i] > init )
                    init = maxh_v[i];
            }

            System.err.println("here 8");
            // H's coordinate updates
            totalinner = 0;
            for (q=0 ; q<m ; q++ )
            {
                for ( hinner = 0 ; hinner <n ; hinner++)
                {
                    double pv=maxh_v[q];
                    p = maxh_ind[q];
                    if ( p ==-1 )
                        break;
                    double s = SH[p][q];

                    if (pv < init*tol)
                        break;
                    for ( i=0 ; i<k ; i++)
                    {
                        HH[p][i] = HH[p][i]+s*H[i][q];
                        HH[i][p] = HH[p][i];
                    }
                    HH[p][p] = HH[p][p] + s*s+s*H[p][q];

                    Hnew[p][q] += s;
                    H[p][q] = H[p][q] + s;

                    maxh_v[q] = 0;
                    maxh_ind[q]=-1;
                    double tmpg, tmpw, tmph;
                    for ( i=0 ; i<k ; i++ )
                    {
                        GH[i][q] = GH[i][q] + s*WW[i][p];
                        ss = H[i][q]-GH[i][q]/WW[i][i];
                        if (ss < 0)
                            ss=0;
                        ss = ss-H[i][q];
                        SH[i][q] = ss;
                        diffobj = ss*(GH[i][q]+0.5*WW[i][i]*ss);
                        diffobj *= (-1);
                        if ( diffobj > maxh_v[q])
                        {
                            maxh_v[q] = diffobj;
                            maxh_ind[q] = i;
                        }
                    }
                }
                totalinner += hinner;
            }

            if (iter%1==0)
            {
                double obj = obj(W,H);
                System.out.format("GCD iter: %d   objective value: %f\n", iter, obj);
                double proj = proj(W,H,GW,GH,VH,HH,WV,WW);
            }
        }
    }

    double obj(double W[][], double H[][])
    {
        int k = rank;
        int i, j, r, q;
        double total = 0, v;

        // Pick 1000 random rows.
        for (int z = 0; z < 1000; z++) {
            if (z % 100 == 0) System.err.println("calculating row " + z);
            j = random.nextInt(m);
            SparseVector sv = V.getRow(j);
            int now = 0;
            for ( q=0 ; q<sv.getUsed(); q++ )
            {
                i = sv.getIndex()[q];
                // compute elements with Vij==0
                for ( ; now<i ; now++ )
                {
                    v=0;
                    for (r=0 ; r<k ; r++)
                        v -= W[now][r]*H[r][j];
                    total += v*v;
                }

                // compute element with Vij!=0
                v = sv.getData()[q];
                for (r=0 ; r<k ; r++ )
                    v -= W[i][r]*H[r][j];
                total = total + v*v;
                now = i+1;
            }
            for ( ; now<n ; now++ )
            {
                v=0;
                for (r=0 ; r<k ; r++)
                    v -= W[now][r]*H[r][j];
                total += v*v;
            }
        }
        return total/2;
    }

    double proj(double W[][], double H[][], double GW[][], double GH[][], double VH[][], double HH[][], double WV[][], double WW[][])
    {
        int k = rank;
        double pg = 0;
        int count = 0;
        int i, j,r;

        // Compute GW and GH
        for ( i=0 ; i<n ; i++ )
            for ( j=0 ; j<k ; j++ )
            {
                GW[i][j] = (-1)*VH[i][j];
                for ( r=0 ; r<k ; r++ )
                    GW[i][j] += W[i][r]*HH[r][j];
            }

        for ( i=0 ; i<k ; i++ )
            for ( j=0 ; j<m ; j++ )
            {
                GH[i][j] = (-1)*WV[i][j];
                for ( r=0 ; r<k ; r++ )
                    GH[i][j] += WW[i][r]*H[r][j];
            }

        for ( i=0 ; i<n ; i++ )
            for ( j=0 ; j<k ; j++ )
            {
                if ( W[i][j] == 0)
                {
                    if ( GW[i][j]<0)
                        pg += GW[i][j]*GW[i][j];
                }
                else
                    pg += GW[i][j]*GW[i][j];
            }
        for ( i=0 ; i<k ; i++ )
            for ( j=0 ; j<m ; j++ )
            {
                if ( H[i][j] == 0)
                {
                    if ( GH[i][j]<0)
                        pg += GH[i][j]*GH[i][j];
                }
                else
                    pg += GH[i][j]*GH[i][j];
            }
        pg = Math.sqrt(pg);

        return pg;
    }
    /* public void nmf_iteration(int iter) {
        System.err.println("here 1");

        // Update H
        Matrix Wt_W = new DenseMatrix(rank, rank);
        W.transAmult(W, Wt_W);
        System.err.println("here 2");

        double deltaH = 0.0;

        Matrix newH = new DenseMatrix(H.numRows(), H.numColumns());
        for (int a = 0; a < rank; a++) {
            for (int u = 0; u < m; u++) {
                SparseVector vec = Vt.getRow(u);
                int vecIds[] = vec.getIndex();
                double vecVals[] = vec.getData();
                double wt_v = 0.0;
                for (int i = 0; i < vec.getUsed(); i++) {
                    wt_v += W.get(vecIds[i], a) * vecVals[i];
                }
                double wt_w_h = 1e-9;
                for (int i = 0; i < rank; i++) {
                    wt_w_h += Wt_W.get(a, i) * H.get(i, u);
                }
                double k = wt_v / wt_w_h;
                newH.set(a, u, H.get(a, u) * k + 1e-9);
                deltaH += Math.abs(k - 1.0);
            }
        }
        System.err.println("here 3: " + deltaH / (rank * m));

        // Update W
        Matrix H_Ht = new DenseMatrix(rank, rank);
        H.transBmult(H, H_Ht);
        Matrix newW = new DenseMatrix(W, true);
        double deltaW = 0.0;
        for (int i = 0; i < n; i++) {
            for (int a = 0; a < rank; a++) {

                SparseVector vec = V.getRow(i);
                int vecIds[] = vec.getIndex();
                double vecVals[] = vec.getData();

//                System.err.println("indexes are " + Arrays.toString(vecIds));
                double v_ht = 0.0;
                for (int j = 0; j < vec.getUsed(); j++) {
                    v_ht += vecVals[j] * H.get(a, vecIds[j]);
                }
                double w_h_ht = 1e-9;
                for (int j = 0; j < rank; j++) {
                    w_h_ht += W.get(i, j) * H_Ht.get(j, a);
                }
                double k = 1.0 *  v_ht / w_h_ht;
                newW.set(i, a, W.get(i, a) * k + 1e-9);
                deltaW+= Math.abs(k - 1.0);
            }
        }
        System.err.println("here 4: " + deltaW / (rank * n));

        H = newH;
        W = newW;
    }        */

    public static void main(String args[]) throws IOException {

        SparseMatrix m = new SparseMatrix(new File("/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/inlink/simple/feature.matrix"));
        SparseMatrix mt = new SparseMatrix(new File("/Volumes/ShiladsFastDrive/wikibrain-simple/dat/sr/inlink/simple/featureTranspose.matrix"));
        MyNMF nmf = new MyNMF(m, mt, 50);
        nmf.nmf2();
    }
}
