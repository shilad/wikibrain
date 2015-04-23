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
 * Method:
   * `mostSimilar(A)`: finds the most similar concepts for a particular query `A`.
   * `similarity(A, B)`: estimates the similarity of concepts `A` and `B`.
   * `cosimilarity(rows, cols)`: builds a cosimilarity matrix between the concept in `rows` and `cols.
 * Query mode:
   * `phrase`: Concepts are represented by free-text queries.
   * `article`: Concepts are represented by Wikipedia article via their id.

SR researchers have traditionally studied the `similarity` / `phrase` cell in this six cell (two by three) grid.
However, WikiBrain suports all six cells.
In fact, in our experience applications are more likely to need `cosimilarity` and `mostSimilar` than `similarity`.

## Efficiencey

There are two dimensions of efficiency an application needs to consider.
**Training complexity** describes the amount of CPU time needed to create the data needed by an SR measure.
It is incurred once, offline, before an application is launched.
**Online complexity** describes the amount of CPU time needed to perform an API. 
This cost is incurred for every invocation of an SR API call.

There is often a tradeoff between online and training complexity.
Many algorithms that perform quickly and accurately reuqire longer training time.


## Other SR algorithms:


To use these algorithms, you must *build models* that capture the statistical relationships an SR metric uses to calculate similarities. To do this, run the SRBuilder java program for a particular SR metric (in this case the *inlink* metric):

```bash
./wb-java.sh org.wikibrain.sr.SRBuilder -m inlink
```

The inlink metric is a fast but relatively inaccurate SR metric. You can also build the "ensemble" metric that provides a linear combination of four other metrics. Beware that training the ensemble is costly. It takes about 10 minutes on Simple English Wikipedia, and a little over a day on the full Wikipedia. Most of the model-building time supports the *mostSimilar()* call, so you can speed up model building if you only need *similarity()*. TODO: explain how to do this.
