package org.wikapidia.sr;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;
import org.wikapidia.sr.normalize.PercentileNormalizer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestNormalizer {
    @Test
    public void testPercent() {
        PercentileNormalizer p = new PercentileNormalizer();
        for (double x : Arrays.asList(1.0, 4.0, 3.2, 5.0, 7.9, 10.5, 11.2, 6.5)) {
            p.observe(x);
        }
        p.observationsFinished();
        assertEquals(p.normalize(1.0), 0.1111, 0.0001);
        assertEquals(p.normalize(11.2), 0.8888, 0.0001);
        assertTrue(p.normalize(0.0) > 0);
        assertTrue(p.normalize(0.0) < 0.111);
        assertTrue(p.normalize(0.01) > p.normalize(0.0));
        assertTrue(p.normalize(20) > 0.888888);
        assertTrue(p.normalize(20) < 1.0);
        assertTrue(p.normalize(20) < p.normalize(200));
    }

    @Test
    public void testPercent2() {
        PercentileNormalizer p = new PercentileNormalizer();
        for (double x : Arrays.asList(1.0, 1.0, 3.2, 5.0, 7.9, 10.5, 11.2, 6.5)) {
            p.observe(x);
        }
        p.observationsFinished();
    }

    @Test
    public void testPercentIO() throws IOException, ClassNotFoundException {
        PercentileNormalizer p = new PercentileNormalizer();
        for (double x : Arrays.asList(1.0, 4.0, 3.2, 5.0, 7.9, 10.5, 11.2, 6.5)) {
            p.observe(x);
        }
        p.observationsFinished();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(stream);
        out.writeObject(p);

        ByteArrayInputStream stream2 = new ByteArrayInputStream(stream.toByteArray());
        ObjectInputStream in = new ObjectInputStream(stream2);
        p = (PercentileNormalizer) in.readObject();

        assertEquals(p.normalize(1.0), 0.1111, 0.0001);
        assertEquals(p.normalize(11.2), 0.8888, 0.0001);
        assertTrue(p.normalize(0.0) > 0);
        assertTrue(p.normalize(0.0) < 0.111);
        assertTrue(p.normalize(0.01) > p.normalize(0.0));
        assertTrue(p.normalize(20) > 0.888888);
        assertTrue(p.normalize(20) < 1.0);
        assertTrue(p.normalize(20) < p.normalize(200));
    }

    @Test
    public void testPolyInterp(){
        /* FIXME
        LoessNormalizer polyInterp = new LoessNormalizer();
        List<double[]> points= new ArrayList<double[]>();
        points.add(new double[]{0,0});
        points.add(new double[]{1.5,0.5});
        points.add(new double[]{-2,0.025});
        points.add(new double[]{4,0.7});
        points.add(new double[]{13,0.9});
        points.add(new double[]{0,0.1});
        points.add(new double[]{0,0.3});
        points.add(new double[]{-1,0.6});
        points.add(new double[]{14,0.5});
        points.add(new double[]{7,0.5});
        points.add(new double[]{-3,0.1});
        points.add(new double[]{4,0.6});
        for (double[] p:points){
            polyInterp.observe(p[0],p[1]);
        }
        polyInterp.observationsFinished();
        double[][] nodes=new double[][]{{-2,0.2416},{0,0.1333},{3.1666,0.6},{11.3333,0.6333}};
        double[][] acknodes=polyInterp.getNodes();
        assertEquals(acknodes.length, nodes.length);
        List<double[]> allpoints = polyInterp.getAllPoints();
        assertEquals(-3.0, allpoints.get(0)[0],0.0001);
        assertEquals(-2.0, allpoints.get(1)[0],0.0001);
        assertEquals(-1.0, allpoints.get(2)[0],0.0001);
        assertArrayEquals(nodes[0],acknodes[0],0.0001);
        assertArrayEquals(nodes[1],acknodes[1],0.0001);
        assertArrayEquals(nodes[2],acknodes[2],0.0001);
        assertArrayEquals(nodes[3],acknodes[3],0.0001);
        assertEquals(polyInterp.normalize(0),0.1333,0.0001);
        assertEquals(polyInterp.normalize(1),0.2807,0.0001);
        assertEquals(polyInterp.normalize(14),0.6442,0.0001);
        assertEquals(polyInterp.normalize(4),0.6034,0.0001);
        assertEquals(polyInterp.normalize(-2),0.2416,0.0001);
        assertEquals(polyInterp.normalize(2.2),0.4575,0.0001);
        assertEquals(polyInterp.normalize(10000),1,0.0001);
        */
    }
}
