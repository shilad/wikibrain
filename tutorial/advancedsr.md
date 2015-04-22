---
        layout: default
        title: Advanced SR usage.
---

# Advanced Semantic Relatedness Usage

WikiBrain provides a robust suite of SR measures and tools.
By default, when you load some language editions, Wikipedia trains the Milne Witten [2006] SR measure.
This algorithm strikes an excellent balance between accuracy, speed, and coverage.
However, you may tap into a variety of other algorithms.

## Modes of operation

As described in the SR overview page, WikiBrain supports a variety of modes of operation.
The two key dimensions it supports are:
 * API call:
   * `mostSimilar(A)` finds the most similar concepts for a particular query `A`.
   * `similarity(A, B)` estimates the similarity of concepts `A` and `B`.
   * `cosimilarity(rows, cols)` builds a cosimilarity matrix between the concept in `rows` and `cols.
 * Query mode:
   * `phrase`
   * `article`

## Other SR algorithms:


To use these algorithms, you must *build models* that capture the statistical relationships an SR metric uses to calculate similarities. To do this, run the SRBuilder java program for a particular SR metric (in this case the *inlink* metric):

```bash
./wb-java.sh org.wikibrain.sr.SRBuilder -m inlink
```

The inlink metric is a fast but relatively inaccurate SR metric. You can also build the "ensemble" metric that provides a linear combination of four other metrics. Beware that training the ensemble is costly. It takes about 10 minutes on Simple English Wikipedia, and a little over a day on the full Wikipedia. Most of the model-building time supports the *mostSimilar()* call, so you can speed up model building if you only need *similarity()*. TODO: explain how to do this.
