package org.wikibrain.sr;


import org.junit.Test;

public class TestSRResult {
    @Test
    public void test(){
        SRResult result = new SRResult();
        System.out.println(result.getScore());
        SRResultList results = new SRResultList(5);
        for (SRResult r : results) {
            System.out.println(r.getId() + ":" + r.getScore());
        }
    }
}
