LDBC Semantic Publishing Benchmark
==================================

Description
-----------

Semantic Publisingh Benchmark is an LDBC benchmark for testing the performance an RDF engines inspired by the Media/Publishing industry.
Performance is measuerd by producing a workload of CRUD (Create, Read, Update, Delete) operations which are executed simultaneously.

The benhcmark offers a data generator that uses real reference data to produce datasets of various sizes and tests the scalability aspect
of RDF systems. The benchmark workload consists of (a) editorial operations that add new data, alter or delete existing (b) aggregation
operations that retrieve content according to various criteria. The aggregation queries define different "choke points", that is technical 
challenges that a standard RDF store must overcome thus giving opportunities for improvement regarding query optimization.

The benchmark also tests conformance for various rules inside the OWL2-RL rule-set.

